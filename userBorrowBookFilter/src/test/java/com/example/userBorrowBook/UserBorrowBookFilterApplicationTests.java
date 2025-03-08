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
