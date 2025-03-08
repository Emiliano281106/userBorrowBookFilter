```java
package com.example.userBorrowBook;

import com.example.userBorrowBook.model.Book;
import com.example.userBorrowBook.model.Borrow;
import com.example.userBorrowBook.model.UserApp;
import com.example.userBorrowBook.repository.BookRepository;
import com.example.userBorrowBook.repository.BorrowRepository;
import com.example.userBorrowBook.repository.BorrowSpecification;
import com.example.userBorrowBook.repository.UserAppRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserBorrowBookFilterApplicationTests {

    @Autowired
    BookRepository bookRepository;
    @Autowired
    BorrowRepository borrowRepository;
    @Autowired
    UserAppRepository userAppRepository;

    private static final Logger logger = LoggerFactory.getLogger(UserBorrowBookFilterApplicationTests.class);

    @Test
    void contextLoads() {
    }

    @Test
    @Transactional
    public void testFilterBorrows() {
        logger.info("Starting testFilterBorrows");

        // Arrange: Create and save a user
        UserApp user = new UserApp();
        //user.setId("U101");
        user.setUserAppName("John Doe");
        user.setAge(24);
        user.setArchived(false);
        user.setDob(LocalDate.of(2000, 1, 1));
        userAppRepository.saveAndFlush(user);
        logger.info("User saved: {}", user);

        // Arrange: Create and save a book
        Book book = new Book();
        //book.setId("B101");
        book.setTitle("Spring Boot Guide");
        book.setIsbn("123456789");
        book.setAvailable(true);
        bookRepository.saveAndFlush(book);
        logger.info("Book saved: {}", book);

        // Arrange: Create and save a borrow record
        Borrow borrow = new Borrow();
        borrow.setBorrowDate(LocalDate.now());
        borrow.setReturnDate(LocalDate.now().plusDays(14));
        borrow.setReturned(false);
        borrow.setUser(user);
        borrow.setBook(book);
        borrowRepository.saveAndFlush(borrow);
        logger.info("Borrow saved: {}", borrow);

        // Act: Filter borrows with specific parameters
        List<Borrow> result = borrowRepository.findAll(BorrowSpecification.filterByParameters(
                "Spring", "123456789", true, 25, false, LocalDate.of(2000, 1, 1), false));
        logger.info("Filter result size: {}", result.size());

        // Assert: Verify that the result contains only one matching record
        assertEquals(1, result.size());
        assertEquals(borrow.getId(), result.get(0).getId());
        logger.info("Assertion passed. Test completed successfully");
    }
}
```

```bash
Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build what is described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html#0.3
WARNING: A Java agent has been loaded dynamically (/home/albert/.m2/repository/net/bytebuddy/byte-buddy-agent/1.15.11/byte-buddy-agent-1.15.11.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
2025-03-08T08:49:33.052+01:00  INFO 11200 --- [userBorrowBookFilter] [           main] e.u.UserBorrowBookFilterApplicationTests : Starting testFilterBorrows
2025-03-08T08:49:33.175+01:00  INFO 11200 --- [userBorrowBookFilter] [           main] e.u.UserBorrowBookFilterApplicationTests : User saved: UserApp(id=724b43d6-322b-4f44-9a36-f6d5cb819080, userAppName=John Doe, email=null, password=null, age=24, address=null, archived=false, dob=2000-01-01)
2025-03-08T08:49:33.183+01:00  INFO 11200 --- [userBorrowBookFilter] [           main] e.u.UserBorrowBookFilterApplicationTests : Book saved: Book(id=2fe2ff75-c9b6-46dd-b108-f1313d100de5, title=Spring Boot Guide, author=null, isbn=123456789, pagesQty=0, available=true, publicationDate=null)
2025-03-08T08:49:33.203+01:00  INFO 11200 --- [userBorrowBookFilter] [           main] e.u.UserBorrowBookFilterApplicationTests : Borrow saved: Borrow(id=a9760a39-77b2-42e2-a934-c51f3b3a21d2, borrowDate=2025-03-08, returnDate=2025-03-22, returned=false, points=0, user=UserApp(id=724b43d6-322b-4f44-9a36-f6d5cb819080, userAppName=John Doe, email=null, password=null, age=24, address=null, archived=false, dob=2000-01-01), book=Book(id=2fe2ff75-c9b6-46dd-b108-f1313d100de5, title=Spring Boot Guide, author=null, isbn=123456789, pagesQty=0, available=true, publicationDate=null))
2025-03-08T08:49:33.416+01:00  INFO 11200 --- [userBorrowBookFilter] [           main] e.u.UserBorrowBookFilterApplicationTests : Filter result size: 1
2025-03-08T08:49:33.419+01:00  INFO 11200 --- [userBorrowBookFilter] [           main] e.u.UserBorrowBookFilterApplicationTests : Assertion passed. Test completed successfully
```
