package com.zsirosd.awspoc.api.controller;

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.zsirosd.awspoc.entity.Book;
import com.zsirosd.awspoc.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class BookController {

    private final BookRepository bookRepository;

    @Autowired
    public BookController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @GetMapping("/books/{id}")
    public Book getOneBookById(@PathVariable(value = "id") String bookId) {
        return findOneBookOrThrowExp(bookId);
    }

    private Book findOneBookOrThrowExp(@PathVariable("id") String bookId) {
        return bookRepository.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
    }

    @GetMapping("/books")
    private Iterable<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @PutMapping("/books/{id}")
    public Book updateBook(@PathVariable(value = "id") String bookId, @Valid @RequestBody Book bookDetails) {
        Book book = findOneBookOrThrowExp(bookId);
        book.setImageId(bookDetails.getImageId());
        book.setTitle(bookDetails.getTitle());
        book.setDescription(bookDetails.getDescription());
        book.setId(bookDetails.getId());

        return bookRepository.save(book);
    }

    @PostMapping("/books")
    public Book createBook(@Valid @RequestBody Book bookDetails) {
        return bookRepository.save(bookDetails);
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable(value = "id") String bookId) {
        Book bookToDelete = findOneBookOrThrowExp(bookId);
        bookRepository.delete(bookToDelete);
        return ResponseEntity.ok().build();
    }
}
