package com.zsirosd.awspoc.api.controller;

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.zsirosd.awspoc.api.request.CreateBookRequest;
import com.zsirosd.awspoc.api.request.UpdateBookRequest;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class BookController {

    private static final String BUCKET_NAME = "awspoc1662";
    private static final String KEY_PREFIX = "book_images"; // folder name

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

    @GetMapping("/books")
    private Iterable<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @PutMapping("/books/{id}")
    public Book updateBook(@PathVariable(value = "id") String bookId, @Valid @RequestBody UpdateBookRequest updateBookRequest) {
        Book updatingBook = findOneBookOrThrowExp(bookId);
        updatingBook.setTitle(updateBookRequest.getTitle());
        updatingBook.setDescription(updateBookRequest.getDescription());
        updatingBook.setImageId(updateBookRequest.getImageId());

        return bookRepository.save(updatingBook);
    }

    @GetMapping("/books/{id}/image")
    public ResponseEntity<byte[]> getBookImage(@PathVariable(value = "id") String bookId) throws IOException {
        Book book = findOneBookOrThrowExp(bookId);
        S3Object fullObject = amazonS3client.getObject(new GetObjectRequest(BUCKET_NAME, KEY_PREFIX + "/" + book.getImageId() + "/thumbnail"));
        String contentType = fullObject.getObjectMetadata().getContentType();
        byte[] content = fullObject.getObjectContent().readAllBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(contentType));
        // TODO add cloudfront

        return ResponseEntity.ok().headers(headers).body(content);
    }

    @PatchMapping("/books/{id}/image")
    public ResponseEntity<PutObjectResult> uploadBookImage(@PathVariable(value = "id") String bookId, @RequestPart MultipartFile imageMultipart) throws IOException {
        Book book = findOneBookOrThrowExp(bookId);
        PutObjectResult result = storeImage(book, imageMultipart);
        return ResponseEntity.ok(result);
    }

    private PutObjectResult storeImage(Book book, MultipartFile imageMultipart) throws IOException {
        File imageFile = convertMultipartToFile(imageMultipart);
        String imageId = book.getImageId();
        if (null == imageId) {
            imageId = UUID.randomUUID().toString();
            saveNewImageOfBook(book, imageId);
        }
        return storeFileToS3(imageId, imageFile);
    }

    private void saveNewImageOfBook(Book book, String imageId) {
        book.setImageId(imageId);
        bookRepository.save(book);
    }

    private PutObjectResult storeFileToS3(String fileId, File file) {
        return amazonS3client.putObject(new PutObjectRequest(BUCKET_NAME, KEY_PREFIX + "/" + fileId + "/thumbnail", file));
    }

    private File convertMultipartToFile(MultipartFile fileToConvert) throws IOException {
        File convertedFile = new File(Objects.requireNonNull(fileToConvert.getOriginalFilename()));
        FileOutputStream fileOutputStream = new FileOutputStream(convertedFile);
        fileOutputStream.write(fileToConvert.getBytes());
        fileOutputStream.close();
        return convertedFile;
    }

    @PostMapping("/books/{id}")
    public Book createBook(@Valid @RequestBody CreateBookRequest createBookRequest) {
        Book createdBook = new Book();
        createdBook.setDescription(createBookRequest.getDescription());
        createdBook.setTitle(createBookRequest.getTitle());
        return bookRepository.save(createdBook);
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
