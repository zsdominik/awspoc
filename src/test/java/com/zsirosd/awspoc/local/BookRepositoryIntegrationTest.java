package com.zsirosd.awspoc.local;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.zsirosd.awspoc.AwspocApplication;
import com.zsirosd.awspoc.entity.Book;
import com.zsirosd.awspoc.repository.BookRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = AwspocApplication.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "amazon.dynamodb.endpoint=http://localhost:8002/",
        "amazon.aws.accesskey=test1",
        "amazon.aws.secretkey=test231"})
public class BookRepositoryIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(BookRepositoryIntegrationTest.class);

    private DynamoDBMapper dynamoDBMapper;

    @Autowired
    private AmazonDynamoDB amazonDynamoDB;

    @Autowired
    private BookRepository repository;

    @Before
    public void setup() {
        dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB);

        CreateTableRequest tableRequest = dynamoDBMapper
                .generateCreateTableRequest(Book.class);
        tableRequest.setProvisionedThroughput(
                new ProvisionedThroughput(1L, 1L));

        try {
            amazonDynamoDB.deleteTable("books");
        } catch (ResourceNotFoundException e) {
            logger.error("Table is not existing, nothing to delete: " + e.getMessage());
        }
        amazonDynamoDB.createTable(tableRequest);

        dynamoDBMapper.batchDelete(repository.findAll());
    }

    @Test
    public void sampleTestCase() {
        Book book = new Book();
        String generatedId = UUID.randomUUID().toString();
        book.setId(generatedId);
        book.setDescription("The description of this strange test Book");
        book.setTitle("A Totally Boring Title");
        book.setImageId("123");

        repository.save(book);

        List<Book> result = (List<Book>) repository.findAll();

        assertTrue("Not empty", result.size() > 0);
        assertEquals("", generatedId, result.get(0).getId());
    }
}
