package com.zsirosd.awspoc.api.controller;

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.zsirosd.awspoc.entity.Book;
import com.zsirosd.awspoc.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1")
public class BookController {

    private static final String BUCKET_NAME = "awspoc1662";
    private static final String KEY_PREFIX = "book_images/"; // folder name

    private final BookRepository bookRepository;
    private final AmazonS3 amazonS3client;

    @Autowired
    public BookController(BookRepository bookRepository, AmazonS3 amazonS3client) {
        this.bookRepository = bookRepository;
        this.amazonS3client = amazonS3client;
    }

    @GetMapping("/books/{id}")
    public Book getOneBookById(@PathVariable(value = "id") String bookId) {
        return findOneBookOrThrowExp(bookId);
    }

    @GetMapping("/books/{id}/image")
    public ResponseEntity<byte[]> getBookImage(@PathVariable(value = "id") String bookId) throws IOException {
        S3Object fullObject = amazonS3client.getObject(new GetObjectRequest(BUCKET_NAME, KEY_PREFIX + bookId));
        String contentType = fullObject.getObjectMetadata().getContentType();
        byte[] content = fullObject.getObjectContent().readAllBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(contentType));
        // TODO add cloudfront

        return ResponseEntity.ok().headers(headers).body(content);
    }

    @GetMapping("/books")
    private Iterable<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @PutMapping("/books/{id}")
    public Book updateBook(@PathVariable(value = "id") String bookId, @Valid @RequestBody Book bookDetails) {
        Book book = findOneBookOrThrowExp(bookId);
        book.setTitle(bookDetails.getTitle());
        book.setDescription(bookDetails.getDescription());
        book.setId(bookDetails.getId());

        return bookRepository.save(book);
    }

    @PatchMapping("/books/{id}/image")
    public ResponseEntity<Void> uploadBookImage(@PathVariable(value = "id") String bookId, @RequestPart MultipartFile imageOfBook) throws IOException {
        amazonS3client.putObject(new PutObjectRequest(BUCKET_NAME, KEY_PREFIX + bookId, convertMultipartToFile(imageOfBook)));
        return ResponseEntity.ok().build();
    }

    private File convertMultipartToFile(MultipartFile fileToConvert) throws IOException {
        File convertedFile = new File(Objects.requireNonNull(fileToConvert.getOriginalFilename()));
        FileOutputStream fileOutputStream = new FileOutputStream(convertedFile);
        fileOutputStream.write(fileToConvert.getBytes());
        fileOutputStream.close();
        return convertedFile;
    }

    @PostMapping("/books/{id}")
    public Book createBook(@Valid @RequestBody Book bookDetails) {
        return bookRepository.save(bookDetails);
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable(value = "id") String bookId) {
        Book bookToDelete = findOneBookOrThrowExp(bookId);
        bookRepository.delete(bookToDelete);
        return ResponseEntity.ok().build();
    }

    private Book findOneBookOrThrowExp(@PathVariable("id") String bookId) {
        return bookRepository.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
    }

}
