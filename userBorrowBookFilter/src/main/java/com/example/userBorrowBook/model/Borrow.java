package com.example.userBorrowBook.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Borrow {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private LocalDate borrowDate;
    private LocalDate returnDate;
    private boolean returned;
    private int points;

    // Unidirectional relationship with UserApp
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    //@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private UserApp user;

    // Unidirectional relationship with Book
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id", nullable = false)
    //@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Book book;
}