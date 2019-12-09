package com.zsirosd.awspoc.api.controller;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.zsirosd.awspoc.api.request.CreateAndUpdateBookRequest;
import com.zsirosd.awspoc.entity.Book;
import com.zsirosd.awspoc.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
public class BookController {

    private static final String BUCKET_NAME = "awspoc1662";
    private static final String KEY_PREFIX = "book_images"; // folder name
    private static final Logger LOGGER = LoggerFactory.getLogger(BookController.class);

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
    public Book updateBook(@PathVariable(value = "id") String bookId, @Valid @RequestBody CreateAndUpdateBookRequest createAndUpdateBookRequest) {
        Book updatingBook = findOneBookOrThrowExp(bookId);
        updatingBook.setTitle(createAndUpdateBookRequest.getTitle());
        updatingBook.setDescription(createAndUpdateBookRequest.getDescription());

        return bookRepository.save(updatingBook);
    }

    @PatchMapping("/books/{id}")
    public ResponseEntity<Book> uploadBookImage(@PathVariable(value = "id") String bookId, @RequestPart MultipartFile imageMultipart) throws IOException {
        Book result = storeNewAndDeleteOldImage(bookId, imageMultipart);
        return ResponseEntity.ok(result);
    }

    private Book storeNewAndDeleteOldImage(String bookId, MultipartFile imageMultipart) throws IOException, SdkClientException {
        String newImageName = imageMultipart.getOriginalFilename();
        File newImageFile = convertMultipartToFile(imageMultipart);
        storeFileToS3(newImageName, newImageFile);
        Book bookToPatch = findOneBookOrThrowExp(bookId);
        String oldImageName = bookToPatch.getImageName();
        Book patchedBook;
        // TODO handle the scenario of there are different books with same name image (same keys in S3)
        try {
            patchedBook = saveNewImageDataOfBookToDb(bookToPatch, newImageName);
        } catch (Exception e) {
            LOGGER.error("Cannot store new image data into the database. Rolling back S3");
            deleteImageFromS3ByName(newImageName);
            // TODO find better solution
            return bookToPatch;
        }

        if (null != oldImageName) {
            deleteImageFromS3ByName(oldImageName);
        }

        return patchedBook;
    }

    private void deleteImageFromS3ByName(String imageName) {
        amazonS3client.deleteObject(new DeleteObjectRequest(BUCKET_NAME, KEY_PREFIX + "/" + imageName));
    }

    private void storeFileToS3(String fileName, File file) {
        amazonS3client.putObject(new PutObjectRequest(BUCKET_NAME, KEY_PREFIX + "/" + fileName,
                file).withCannedAcl(CannedAccessControlList.PublicRead));
    }

    private Book saveNewImageDataOfBookToDb(Book bookToPatch, String imageName) {
        String newImageUrl = amazonS3client.getUrl(BUCKET_NAME, KEY_PREFIX + "/" + imageName).toString();
        // TODO create builder
        Book patchedBook = new Book(bookToPatch.getId(), bookToPatch.getTitle(), bookToPatch.getDescription(), newImageUrl, imageName);
        return bookRepository.save(patchedBook);
    }

    private File convertMultipartToFile(MultipartFile fileToConvert) throws IOException {
        File convertedFile = new File(fileToConvert.getOriginalFilename());
        FileOutputStream fileOutputStream = new FileOutputStream(convertedFile);
        fileOutputStream.write(fileToConvert.getBytes());
        fileOutputStream.close();
        return convertedFile;
    }

    @PostMapping("/books/{id}")
    public Book createBook(@Valid @RequestBody CreateAndUpdateBookRequest createAndUpdateBookRequest) {
        Book createdBook = new Book();
        createdBook.setDescription(createAndUpdateBookRequest.getDescription());
        createdBook.setTitle(createAndUpdateBookRequest.getTitle());
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
