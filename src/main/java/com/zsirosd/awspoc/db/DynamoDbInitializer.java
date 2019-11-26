package com.zsirosd.awspoc.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.zsirosd.awspoc.entity.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DynamoDbInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DynamoDbInitializer.class);
    private static final String TABLE_NAME_BOOKS = "books";

    private final AmazonDynamoDB amazonDynamoDB;

    private DynamoDBMapper dynamoDBMapper;

    @Autowired
    public DynamoDbInitializer(AmazonDynamoDB amazonDynamoDB) {
        this.amazonDynamoDB = amazonDynamoDB;
    }

    @Override
    public void run(String... args) {
        dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB);
        CreateTableRequest tableRequest = dynamoDBMapper
                .generateCreateTableRequest(Book.class);
        tableRequest.setProvisionedThroughput(
                new ProvisionedThroughput(1L, 1L));
        logger.info("Creating table name '" + TABLE_NAME_BOOKS + "'");
        try {
            amazonDynamoDB.createTable(tableRequest);
            logger.info("Table with name '" + TABLE_NAME_BOOKS + "' created");
        } catch (ResourceInUseException e) {
            logger.error(TABLE_NAME_BOOKS + " table is already existing. Nothing to create");
        }
    }
}
