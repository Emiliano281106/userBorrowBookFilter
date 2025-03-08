# JPA Filter and Specification: Rest Controller

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
    //@Transactional // Add transaction to ensure lazy-loaded entities are initialized
    public ResponseEntity<List<Borrow>> filterBorrows(
            @RequestParam(required = false) String bookTitle,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Integer userAge,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dob,
            @RequestParam(required = false) Boolean returned
    ) {
        // Remember to set fetch to EAGER
        List<Borrow> borrows = borrowRepository.findAll(BorrowSpecification.filterByParameters(
                bookTitle, isbn, available, userAge, archived, dob, returned
        ));

        // Initialize lazy-loaded relationships to avoid serialization issues
        /* borrows.forEach(borrow -> {
            borrow.getBook().getTitle(); // Force initialization of book
            borrow.getUser().getUserAppName(); // Force initialization of user
        });*/

        return ResponseEntity.ok(borrows);
    }
}
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
2. Filters for books whose titles contain<mark> "To Kill"</mark>
3. Filters for borrows that haven't been returned yet

The **query** will return all `borrow` records where the `book` title contains <mark>"To Kill" </mark> and the item hasn't been returned yet.

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

```bash
[Sat Mar 08 10:58:14] albert@albert-VirtualBox:~
$ curl "http://localhost:8080/api/borrows?bookTitle=To%20Kill&returned=false"
[]
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

```bash
[Sat Mar 08 10:58:17] albert@albert-VirtualBox:~
$ curl "http://localhost:8080/api/borrows?bookTitle=To%20Kill&returned=true"
[
    {
        "id": "BR001",
        "borrowDate": "2025-02-15",
        "returnDate": "2025-03-15",
        "returned": true,
        "points": 10,
        "user": {
            "id": "U001",
            "userAppName": "John Doe",
            "email": "john.doe@example.com",
            "password": "password123",
            "age": 30,
            "address": "123 Main St, Anytown, USA",
            "archived": false,
            "dob": "1995-05-15"
        },
        "book": {
            "id": "B001",
            "title": "To Kill a Mockingbird",
            "author": "Harper Lee",
            "isbn": "9780446310789",
            "pagesQty": 281,
            "available": true,
            "publicationDate": "1960-07-11"
        }
    }
]
```

**Query#3**

Filter by user age less than 30 and not archived:

```bash
[Sat Mar 08 10:58:17] albert@albert-VirtualBox:~
curl "http://localhost:8080/api/borrows?userAge=30&archived=false"
[
    {
        "id": "BR002",
        "borrowDate": "2025-02-16",
        "returnDate": "2025-03-16",
        "returned": false,
        "points": 5,
        "user": {
            "id": "U002",
            "userAppName": "Jane Smith",
            "email": "jane.smith@example.com",
            "password": "securepass456",
            "age": 28,
            "address": "456 Elm St, Somewhere, USA",
            "archived": false,
            "dob": "1997-08-22"
        },
        "book": {
            "id": "B002",
            "title": "1984",
            "author": "George Orwell",
            "isbn": "9780451524935",
            "pagesQty": 328,
            "available": true,
            "publicationDate": "1949-06-08"
        }
    },
    {
        "id": "BR004",
        "borrowDate": "2025-02-18",
        "returnDate": "2025-03-18",
        "returned": false,
        "points": 8,
        "user": {
            "id": "U004",
            "userAppName": "Emily Brown",
            "email": "emily.brown@example.com",
            "password": "emilyb2023",
            "age": 26,
            "address": "101 Pine Rd, Elsewhere, USA",
            "archived": false,
            "dob": "1999-11-30"
        },
        "book": {
            "id": "B004",
            "title": "The Great Gatsby",
            "author": "F. Scott Fitzgerald",
            "isbn": "9780743273565",
            "pagesQty": 180,
            "available": true,
            "publicationDate": "1925-04-10"
        }
    },
    {
        "id": "BR008",
        "borrowDate": "2025-02-22",
        "returnDate": "2025-03-22",
        "returned": false,
        "points": 7,
        "user": {
            "id": "U008",
            "userAppName": "Lisa Taylor",
            "email": "lisa.taylor@example.com",
            "password": "taylorpass246",
            "age": 29,
            "address": "505 Spruce St, Someplace, USA",
            "archived": false,
            "dob": "1996-04-12"
        },
        "book": {
            "id": "B008",
            "title": "The Lord of the Rings",
            "author": "J.R.R. Tolkien",
            "isbn": "9780618640157",
            "pagesQty": 1178,
            "available": true,
            "publicationDate": "1954-07-29"
        }
    },
    {
        "id": "BR014",
        "borrowDate": "2025-02-28",
        "returnDate": "2025-03-28",
        "returned": false,
        "points": 8,
        "user": {
            "id": "U014",
            "userAppName": "Michelle Lee",
            "email": "michelle.lee@example.com",
            "password": "leepass357",
            "age": 27,
            "address": "222 Poplar Rd, Whereplace, USA",
            "archived": false,
            "dob": "1998-03-17"
        },
        "book": {
            "id": "B014",
            "title": "Harry Potter and the Philosopher's Stone",
            "author": "J.K. Rowling",
            "isbn": "9780747532743",
            "pagesQty": 223,
            "available": true,
            "publicationDate": "1997-06-26"
        }
    },
    {
        "id": "BR018",
        "borrowDate": "2025-03-04",
        "returnDate": "2025-04-03",
        "returned": false,
        "points": 10,
        "user": {
            "id": "U018",
            "userAppName": "Sophia Rodriguez",
            "email": "sophia.rodriguez@example.com",
            "password": "rodriguezpass951",
            "age": 25,
            "address": "666 Elm Ln, Whoplace, USA",
            "archived": false,
            "dob": "2000-09-14"
        },
        "book": {
            "id": "B018",
            "title": "The Fault in Our Stars",
            "author": "John Green",
            "isbn": "9780525478812",
            "pagesQty": 313,
            "available": true,
            "publicationDate": "2012-01-10"
        }
    },
    {
        "id": "BR025",
        "borrowDate": "2025-03-11",
        "returnDate": "2025-04-10",
        "returned": false,
        "points": 9,
        "user": {
            "id": "U002",
            "userAppName": "Jane Smith",
            "email": "jane.smith@example.com",
            "password": "securepass456",
            "age": 28,
            "address": "456 Elm St, Somewhere, USA",
            "archived": false,
            "dob": "1997-08-22"
        },
        "book": {
            "id": "B006",
            "title": "The Catcher in the Rye",
            "author": "J.D. Salinger",
            "isbn": "9780316769174",
            "pagesQty": 234,
            "available": true,
            "publicationDate": "1951-07-16"
        }
    },
    {
        "id": "BR026",
        "borrowDate": "2025-03-12",
        "returnDate": "2025-04-11",
        "returned": true,
        "points": 11,
        "user": {
            "id": "U002",
            "userAppName": "Jane Smith",
            "email": "jane.smith@example.com",
            "password": "securepass456",
            "age": 28,
            "address": "456 Elm St, Somewhere, USA",
            "archived": false,
            "dob": "1997-08-22"
        },
        "book": {
            "id": "B010",
            "title": "Brave New World",
            "author": "Aldous Huxley",
            "isbn": "9780060850524",
            "pagesQty": 311,
            "available": true,
            "publicationDate": "1932-01-01"
        }
    },
    {
        "id": "BR027",
        "borrowDate": "2025-03-13",
        "returnDate": "2025-04-12",
        "returned": false,
        "points": 6,
        "user": {
            "id": "U002",
            "userAppName": "Jane Smith",
            "email": "jane.smith@example.com",
            "password": "securepass456",
            "age": 28,
            "address": "456 Elm St, Somewhere, USA",
            "archived": false,
            "dob": "1997-08-22"
        },
        "book": {
            "id": "B014",
            "title": "Harry Potter and the Philosopher's Stone",
            "author": "J.K. Rowling",
            "isbn": "9780747532743",
            "pagesQty": 223,
            "available": true,
            "publicationDate": "1997-06-26"
        }
    },
    {
        "id": "BR028",
        "borrowDate": "2025-03-14",
        "returnDate": "2025-04-13",
        "returned": true,
        "points": 14,
        "user": {
            "id": "U002",
            "userAppName": "Jane Smith",
            "email": "jane.smith@example.com",
            "password": "securepass456",
            "age": 28,
            "address": "456 Elm St, Somewhere, USA",
            "archived": false,
            "dob": "1997-08-22"
        },
        "book": {
            "id": "B018",
            "title": "The Fault in Our Stars",
            "author": "John Green",
            "isbn": "9780525478812",
            "pagesQty": 313,
            "available": true,
            "publicationDate": "2012-01-10"
        }
    },
    {
        "id": "BR033",
        "borrowDate": "2025-03-19",
        "returnDate": "2025-04-18",
        "returned": false,
        "points": 7,
        "user": {
            "id": "U004",
            "userAppName": "Emily Brown",
            "email": "emily.brown@example.com",
            "password": "emilyb2023",
            "age": 26,
            "address": "101 Pine Rd, Elsewhere, USA",
            "archived": false,
            "dob": "1999-11-30"
        },
        "book": {
            "id": "B008",
            "title": "The Lord of the Rings",
            "author": "J.R.R. Tolkien",
            "isbn": "9780618640157",
            "pagesQty": 1178,
            "available": true,
            "publicationDate": "1954-07-29"
        }
    },
    {
        "id": "BR034",
        "borrowDate": "2025-03-20",
        "returnDate": "2025-04-19",
        "returned": true,
        "points": 12,
        "user": {
            "id": "U004",
            "userAppName": "Emily Brown",
            "email": "emily.brown@example.com",
            "password": "emilyb2023",
            "age": 26,
            "address": "101 Pine Rd, Elsewhere, USA",
            "archived": false,
            "dob": "1999-11-30"
        },
        "book": {
            "id": "B012",
            "title": "The Little Prince",
            "author": "Antoine de Saint-Exupéry",
            "isbn": "9780156012195",
            "pagesQty": 96,
            "available": true,
            "publicationDate": "1943-04-06"
        }
    },
    {
        "id": "BR035",
        "borrowDate": "2025-03-21",
        "returnDate": "2025-04-20",
        "returned": false,
        "points": 9,
        "user": {
            "id": "U004",
            "userAppName": "Emily Brown",
            "email": "emily.brown@example.com",
            "password": "emilyb2023",
            "age": 26,
            "address": "101 Pine Rd, Elsewhere, USA",
            "archived": false,
            "dob": "1999-11-30"
        },
        "book": {
            "id": "B016",
            "title": "The Hunger Games",
            "author": "Suzanne Collins",
            "isbn": "9780439023481",
            "pagesQty": 374,
            "available": true,
            "publicationDate": "2008-09-14"
        }
    },
    {
        "id": "BR036",
        "borrowDate": "2025-03-22",
        "returnDate": "2025-04-21",
        "returned": true,
        "points": 15,
        "user": {
            "id": "U004",
            "userAppName": "Emily Brown",
            "email": "emily.brown@example.com",
            "password": "emilyb2023",
            "age": 26,
            "address": "101 Pine Rd, Elsewhere, USA",
            "archived": false,
            "dob": "1999-11-30"
        },
        "book": {
            "id": "B020",
            "title": "The Help",
            "author": "Kathryn Stockett",
            "isbn": "9780425232200",
            "pagesQty": 451,
            "available": true,
            "publicationDate": "2009-02-10"
        }
    },
    {
        "id": "16a0e662-89cc-4c3e-b108-ca1101efa23c",
        "borrowDate": "2025-03-08",
        "returnDate": "2025-03-22",
        "returned": false,
        "points": 0,
        "user": {
            "id": "e6013cd5-200c-4084-8c66-01c6a309f8f7",
            "userAppName": "John Doe",
            "email": null,
            "password": null,
            "age": 24,
            "address": null,
            "archived": false,
            "dob": "2000-01-01"
        },
        "book": {
            "id": "f7a9d378-b390-436c-80fe-ce569f8c1fa6",
            "title": "Spring Boot Guide",
            "author": null,
            "isbn": "123456789",
            "pagesQty": 0,
            "available": true,
            "publicationDate": null
        }
    },
    {
        "id": "626f7f77-1451-4d06-aacf-328b723c6040",
        "borrowDate": "2025-03-08",
        "returnDate": "2025-03-22",
        "returned": false,
        "points": 0,
        "user": {
            "id": "3d1455ff-b75a-4379-b8fb-be29c284bc52",
            "userAppName": "John Doe",
            "email": null,
            "password": null,
            "age": 24,
            "address": null,
            "archived": false,
            "dob": "2000-01-01"
        },
        "book": {
            "id": "0591ea31-0d55-484d-a728-69a4c4a95703",
            "title": "Spring Boot Guide",
            "author": null,
            "isbn": "123456789",
            "pagesQty": 0,
            "available": true,
            "publicationDate": null
        }
    }
]
```

**Query#4**

Complex filter with multiple parameters, this filter uses query parameters to search for `borrow` records in an API. It looks for:

- books with a title containing "Spring" (`bookTitle=Spring`),

- an ISBN of `123456789` (`isbn=123456789`),

- and availability set to `true` (`available=true`),

- it also filters users by age (`userAge=25`),

- ensures they are not archived (`archived=false`),

- and have a date of birth of `2000-01-01` (`dob=2000-01-01`),

- finally, it only includes borrows that are not yet returned (`returned=false`).

```bash
[Sat Mar 08 10:58:17] albert@albert-VirtualBox:~
curl "http://localhost:8080/api/borrows?bookTitle=Spring&isbn=123456789&available=true&userAge=25&archived=false&dob=2000-01-01&returned=false"
[
    {
        "id": "16a0e662-89cc-4c3e-b108-ca1101efa23c",
        "borrowDate": "2025-03-08",
        "returnDate": "2025-03-22",
        "returned": false,
        "points": 0,
        "user": {
            "id": "e6013cd5-200c-4084-8c66-01c6a309f8f7",
            "userAppName": "John Doe",
            "email": null,
            "password": null,
            "age": 24,
            "address": null,
            "archived": false,
            "dob": "2000-01-01"
        },
        "book": {
            "id": "f7a9d378-b390-436c-80fe-ce569f8c1fa6",
            "title": "Spring Boot Guide",
            "author": null,
            "isbn": "123456789",
            "pagesQty": 0,
            "available": true,
            "publicationDate": null
        }
    },
    {
        "id": "626f7f77-1451-4d06-aacf-328b723c6040",
        "borrowDate": "2025-03-08",
        "returnDate": "2025-03-22",
        "returned": false,
        "points": 0,
        "user": {
            "id": "3d1455ff-b75a-4379-b8fb-be29c284bc52",
            "userAppName": "John Doe",
            "email": null,
            "password": null,
            "age": 24,
            "address": null,
            "archived": false,
            "dob": "2000-01-01"
        },
        "book": {
            "id": "0591ea31-0d55-484d-a728-69a4c4a95703",
            "title": "Spring Boot Guide",
            "author": null,
            "isbn": "123456789",
            "pagesQty": 0,
            "available": true,
            "publicationDate": null
        }
    }
]
```
