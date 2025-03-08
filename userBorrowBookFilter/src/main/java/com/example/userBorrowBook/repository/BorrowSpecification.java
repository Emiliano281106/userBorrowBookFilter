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
