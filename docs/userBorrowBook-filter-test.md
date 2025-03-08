# JPA Filter and Specification: JUnit test

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
package com.example.userBorrowBook.repository;


import com.example.userBorrowBook.model.Book;
import com.example.userBorrowBook.model.Borrow;
import com.example.userBorrowBook.model.UserApp;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;

public class BorrowSpecification {

    public static Specification<Borrow> filterByParameters(
            String bookTitle,
            String isbn,
            Boolean available,
            Integer userAge,
            Boolean archived,
            LocalDate dob,
            Boolean returned
    ) {
        return (Root<Borrow> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();

            Join<Borrow, Book> bookJoin = root.join("book", JoinType.INNER);
            Join<Borrow, UserApp> userJoin = root.join("user", JoinType.INNER);


            if (bookTitle != null && !bookTitle.isEmpty()) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.like(bookJoin.get("title"), "%" + bookTitle + "%"));
            }

            if (isbn != null && !isbn.isEmpty()) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(bookJoin.get("isbn"), isbn));
            }

            if (available != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(bookJoin.get("available"), available));
            }

            if (userAge != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.lessThan(userJoin.get("age"), userAge));
            }

            if (archived != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(userJoin.get("archived"), archived));
            }

            if (dob != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(userJoin.get("dob"), dob));
            }

            if (returned != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("returned"), returned));
            }

            return predicate;
        };
    }
}
}
```

## Execution

## API Rest

Here's the Key points about the curl command

1. The base URL is now `http://localhost:8080/api/borrows/filter`, matching the `@RequestMapping` and `@GetMapping` in the controller.

2. All parameters are optional, as specified by `@RequestParam(required = false)` in the controller.

3. It is included the `page` and `size` parameters with their default values (0 and 10 respectively).

4. The date format for `dob` is YYYY-MM-DD, which should work with the `LocalDate` parameter in the controller.

Remember to:

- adjust the host and port ([http://localhost:8080](http://localhost:8080/)) if the application is running on a different address,

- if the API requires authentication, we may need to add appropriate headers to the curl command.

```bash
[Sat Mar 08 06:38:21] albert@albert-VirtualBox:~
$ curl "http://localhost:8080/api/borrows/filter?bookTitle=Spring&isbn=123456789&isAvailable=true&userAge=25&isArchived=false&dob=2000-01-01&isReturned=false&page=0&size=10"
[]
```

### Test the Filter

Write an integration test to verify that the filter works as expected.

> **Entity State Management**:  Ensure that the entities you're working with are in the correct state before saving. Sometimes, issues arise when entities are detached or in an inconsistent state.

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//@DataJpaTest
@SpringBootTest
public class BorrowFilterTest {

    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    private UserAppRepository userAppRepository;

    @Autowired
    private BookRepository bookRepository;

    @Test
    //@Transactional
    public void testFilterBorrows() {
        // Arrange: Create and save a user
        UserApp user = new UserApp();
        user.setId("U001");
        user.setName("John Doe");
        user.setAge(24);
        user.setIsArchived(false);
        user.setDob(LocalDate.of(2000, 1, 1));
        //userAppRepository.save(user);
        userAppRepository.saveAndFlush(user);

        // Arrange: Create and save a book
        Book book = new Book();
        book.setId("B001");
        book.setTitle("Spring Boot Guide");
        book.setIsbn("123456789");
        book.setIsAvailable(true);
        // bookRepository.save(book);
        bookRepository.saveAndFlush(book);

        // Arrange: Create and save a borrow record
        Borrow borrow = new Borrow();
        borrow.setBorrowDate(LocalDate.now());
        borrow.setReturnDate(LocalDate.now().plusDays(14));
        borrow.setIsReturned(false);
        borrow.setUser(user);
        borrow.setBook(book);
        //borrowRepository.save(borrow);
        borrowRepository.saveAndFlush(borrow);

        // Act: Filter borrows with specific parameters
        List<Borrow> result = borrowRepository.findAll(BorrowSpecification.filterByParameters(
                "Spring", "123456789", true, 25, false, LocalDate.of(2000, 1, 1), false));

        // Assert: Verify that the result contains only one matching record
        assertEquals(1, result.size());
        assertEquals(borrow.getId(), result.get(0).getId());
    }
}
```

#### transactional

`@Transactional`is an **annotation used in Spring and JPA** to define <mark>transactional boundaries</mark> for methods or classes. 

It ensures that a group of database operations are executed as<mark> a single atomic unit</mark>, adhering to ACID principles. When applied, it manages the transaction lifecycle, including starting, committing, or rolling back transactions automatically.

Key features of `@Transactional`include:

1. Declarative transaction management

2. Propagation behavior control

3. Isolation level specification

4. Read-only flag for optimization

5. Timeout settings

6. Exception handling for rollback

It simplifies transaction handling by eliminating the need for explicit transaction code. @Transactional is typically used at the service layer to encompass multiple repository calls within a single transaction, ensuring data consistency and integrity across database operations.

#### userAppRepository.saveAndFlush(user)

`userAppRepository.saveAndFlush(user)` is a method call that saves a user entity to the database and <mark>immediately flushes the changes to ensure they are persisted</mark>. This method combines two operations:

1. It <mark>saves</mark> the user entity to the database, similar to the save() method.

2. It forces an immediate <mark>flush</mark> of the persistence context, **synchronizing the in-memory state with the database.**

The **saveAndFlush()** method is useful when you need to ensure that the data is immediately written to the database and available for subsequent operations within the same transaction.

However, it's important to note that using `saveAndFlush()` can have performance implications:

1. It prevents Hibernate from optimizing database operations and can lead to unnecessary database writes.

2. It may interfere with Hibernate's ability to group similar JDBC statements into batches

In most cases, using the standard `save() `method is sufficient and allows Hibernate to manage persistence more efficiently. 

> The `saveAndFlush() `method should be used sparingly, typically only when you explicitly need the data to be immediately available in the database within the same transaction

#### Executed test

```bash
Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build what is described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html#0.3
WARNING: A Java agent has been loaded dynamically (/home/albert/.m2/repository/net/bytebuddy/byte-buddy-agent/1.15.11/byte-buddy-agent-1.15.11.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
2025-03-08T07:10:46.703+01:00  INFO 35189 --- [userBorrowBookFilter] [           main] e.u.UserBorrowBookFilterApplicationTests : Starting testFilterBorrows
2025-03-08T07:10:47.137+01:00  INFO 35189 --- [userBorrowBookFilter] [           main] e.u.UserBorrowBookFilterApplicationTests : User saved: UserApp(id=U001, userAppName=John Doe, email=null, password=null, age=24, address=null, isArchived=false, dob=2000-01-01)
2025-03-08T07:10:47.154+01:00  INFO 35189 --- [userBorrowBookFilter] [           main] e.u.UserBorrowBookFilterApplicationTests : Book saved: Book(id=B001, title=Spring Boot Guide, author=null, isbn=123456789, pagesQty=0, available=true, publicationDate=null)
2025-03-08T07:10:47.241+01:00  INFO 35189 --- [userBorrowBookFilter] [           main] e.u.UserBorrowBookFilterApplicationTests : Borrow saved: Borrow(id=f7e87fec-a350-40fd-ae1e-19cddf01c674, borrowDate=2025-03-08, returnDate=2025-03-22, isReturned=false, points=0, user=UserApp(id=U001, userAppName=John Doe, email=null, password=null, age=24, address=null, isArchived=false, dob=2000-01-01), book=Book(id=B001, title=Spring Boot Guide, author=null, isbn=123456789, pagesQty=0, available=true, publicationDate=null))
2025-03-08T07:10:47.812+01:00  INFO 35189 --- [userBorrowBookFilter] [           main] e.u.UserBorrowBookFilterApplicationTests : Filter result size: 1
2025-03-08T07:10:47.825+01:00  INFO 35189 --- [userBorrowBookFilter] [           main] e.u.UserBorrowBookFilterApplicationTests : Assertion passed. Test completed successfully
```

## Key Features of This Implementation:

1. **Dynamic Querying**:
   
   - The `BorrowSpecification` dynamically builds predicates based on client-provided parameters.

2. **Reusable Logic**:
   
   - The specification can be reused across different controllers or services.

3. **Pagination**:
   
   - The controller supports pagination to handle large datasets efficiently.

4. **Flexible Filtering**:
   
   - Clients can provide any combination of filters without requiring multiple query methods in the repository.

### Citations:

1. https://binarymindset.com/filter-your-data-the-right-way-using-jpa-specifications/
2. [JPA Specifications for Dynamic Filtering in Spring Boot - Hyper Leap](https://www.hyper-leap.com/2024/06/27/jpa-specifications-for-dynamic-filtering-in-spring-boot/)
3. [Reddit - Dive into anything](https://www.reddit.com/r/SpringBoot/comments/znj9wg/filter_data_the_right_way_using_spring_jpa/)
4. [Advanced Data Querying with Spring Data JPA and Hibernate: Tips for Complex Queries and Performance Optimization](https://www.linkedin.com/pulse/advanced-data-querying-spring-jpa-hibernate-tips-shant-khayalian-jzfye)
5. [Spring Data Jpa: Global Filter Query](https://rashidi.github.io/spring-boot-tutorials/data-jpa-filtered-query/)
6. [hibernate - What is the best solution for filtering results in Spring Data JPA with many properties? - Stack Overflow](https://stackoverflow.com/questions/26449410/what-is-the-best-solution-for-filtering-results-in-spring-data-jpa-with-many-pro)
7. [Implement your Search/Filter Criteria API with Spring Data JPA Specifications | Stackademic](https://stackademic.com/blog/implement-your-search-filter-api-with-spring-data-jpa-specifications-365f2089ef1c)
8. [Dynamic Spring Data JPA Repository Query With Arbitrary AND Clauses | Baeldung](https://www.baeldung.com/spring-data-jpa-query-arbitrary-and-clauses)
