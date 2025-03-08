# JPA Filter and Specification

## Links

- [GitHub - AlbertProfe/userBorrowBook](https://github.com/AlbertProfe/userBorrowBook)

## Filter step-by-step

To implement a filter for `Borrow` records based on the parameters:

- book title, ISBN, availability,
- user age, archived status, date of birth,
- and borrow return status

the best approach is to use **Spring Data JPA Specifications**. 

This allows for dynamic and reusable filtering logic without writing multiple query methods.

> This approach uses Spring Data JPA's powerful `JpaSpecificationExecutor` interface to handle complex filtering requirements in a <mark>clean, fast-production and maintainable way</mark>.

### Step 1: Add `JpaSpecificationExecutor` to the Repository

Modify your `BorrowRepository` to extend `JpaSpecificationExecutor`:

```java
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BorrowRepository extends JpaRepository<Borrow, Long>, JpaSpecificationExecutor<Borrow> {
}
```

This enables the use of JPA Specifications for dynamic queries.

### Step 2: Create a Specification for Filtering

Create a `BorrowSpecification` class to dynamically build query predicates based on client-provided parameters.

```java
import org.springframework.data.jpa.domain.Specification;
import javax.persistence.criteria.*;
import java.time.LocalDate;

public class BorrowSpecification {

    public static Specification<Borrow> filterByParameters(
            String bookTitle,
            String isbn,
            Boolean isAvailable,
            Integer userAge,
            Boolean isArchived,
            LocalDate dob,
            Boolean isReturned
    ) {
        return (Root<Borrow> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();

            // Join with related entities
            Join<Borrow, Book> bookJoin = root.join("book", JoinType.INNER);
            Join<Borrow, UserApp> userJoin = root.join("user", JoinType.INNER);

            // Filter by book title (LIKE)
            if (bookTitle != null && !bookTitle.isEmpty()) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.like(bookJoin.get("title"), "%" + bookTitle + "%"));
            }

            // Filter by ISBN
            if (isbn != null && !isbn.isEmpty()) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(bookJoin.get("isbn"), isbn));
            }

            // Filter by book availability
            if (isAvailable != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(bookJoin.get("isAvailable"), isAvailable));
            }

            // Filter by user age
            if (userAge != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.lessThan(userJoin.get("age"), userAge));
            }

            // Filter by user archived status
            if (isArchived != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(userJoin.get("isArchived"), isArchived));
            }

            // Filter by user date of birth
            if (dob != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(userJoin.get("dob"), dob));
            }

            // Filter by borrow return status
            if (isReturned != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("isReturned"), isReturned));
            }

            return predicate;
        };
    }
}
```

### Step 3: Implement the Controller

Create a REST controller to expose the filter functionality. The controller will accept query parameters from the client.

```java
package com.example.userBorrowBook.controller;

import com.example.userBorrowBook.model.Borrow;
import com.example.userBorrowBook.repository.BorrowRepository;
import com.example.userBorrowBook.repository.BorrowSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/borrows")
public class BorrowController {

    @Autowired
    private BorrowRepository borrowRepository;

    @GetMapping
    @Transactional // Add transaction to ensure lazy-loaded entities are initialized
    public ResponseEntity<List<Borrow>> filterBorrows(
            @RequestParam(required = false) String bookTitle,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Integer userAge,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dob,
            @RequestParam(required = false) Boolean returned
    ) {
        List<Borrow> borrows = borrowRepository.findAll(BorrowSpecification.filterByParameters(
                bookTitle, isbn, available, userAge, archived, dob, returned
        ));

        // Initialize lazy-loaded relationships to avoid serialization issues
        borrows.forEach(borrow -> {
            borrow.getBook().getTitle(); // Force initialization of book
            borrow.getUser().getUserAppName(); // Force initialization of user
        });

        return ResponseEntity.ok(borrows);
    }
}
```

#### JSON serialization

We must face a common JSON serialization problem in Spring Boot when working with Hibernate's lazy loading are caused by:

1. **Lazy Loading with Jackson Serialization**: When we try to serialize lazy-loaded entities (Book and UserApp) to JSON, Jackson tries to access proxy objects that haven't been initialized.
   
   1. **@Transactional**: we need to add `@Transactional` to ensure the transaction is still active when accessing lazy-loaded entities, and manually initialized them by accessing a property.
   
   2. **Prevent infinite recursion**: we need to add `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})` to tell Jackson to ignore Hibernate's proxy-related fields during serialization.
   
   3. **Initialize lazy-loaded relationships**: to avoid serialization issues:
      
      ```java
               borrows.forEach(borrow -> {
                    borrow.getBook().getTitle(); // Force initialization of book
                    borrow.getUser().getUserAppName(); // Force initialization of user
                });
      ```

## Execution

### API Rest

Here's the Key points about the curl command

1. The base URL is now `http://localhost:8080/api/borrows`, matching the `@RequestMapping` and `@GetMapping` in the controller.

2. All parameters are optional, as specified by `@RequestParam(required = false)` in the controller.

3. It is <mark>NOT</mark> included the `page` and `size` parameters with their default values (0 and 10 respectively).

4. The date format for `dob` is YYYY-MM-DD, which should work with the `LocalDate` parameter in the controller.

Remember to:

- adjust the host and port ([http://localhost:8080](http://localhost:8080/)) if the application is running on a different address,

- if the API requires authentication, we may need to add appropriate headers to the curl command.

#### CURL

**Query#1**

```bash
curl "http://localhost:8080/api/borrows?bookTitle=To%20Kill&returned=false"
```

This SQL query performs the following operations:

1. Joins the `borrow` table with both `book` and `user_app` tables
2. Filters for books whose titles contain "To Kill"
3. Filters for borrows that haven't been returned yet

The query will return all borrow records where the book title contains "To Kill" and the item hasn't been returned yet.

```sql
SELECT 
    b.id, b.borrow_date, b.return_date, b.returned, b.points, 
    b.user_id, b.book_id
FROM 
    borrow b
INNER JOIN 
    book bk ON b.book_id = bk.id
INNER JOIN 
    user_app u ON b.user_id = u.id
WHERE 
    bk.title LIKE '%To Kill%'
    AND b.returned = false;
```

**Query#2**

```bash
curl "http://localhost:8080/api/borrows?bookTitle=To%20Kill&returned=true"
```

```sql
SELECT 
    b.id, b.borrow_date, b.return_date, b.returned, b.points, 
    b.user_id, b.book_id
FROM 
    borrow b
INNER JOIN 
    book bk ON b.book_id = bk.id
INNER JOIN 
    user_app u ON b.user_id = u.id
WHERE 
    bk.title LIKE '%To Kill%'
    AND b.returned = true;
```
