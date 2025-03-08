package com.example.userBorrowBook.repository;

import com.example.userBorrowBook.model.Borrow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BorrowRepository extends JpaRepository<Borrow, String>, JpaSpecificationExecutor<Borrow> {
    // Custom query methods}
}