package com.zsirosd.awspoc.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.*;

@DynamoDBTable(tableName = "Books")
public class Book {

    private String id;
    private String title;
    private String description;

    public Book() { }

    @DynamoDBHashKey
    @DynamoDBGeneratedUuid(DynamoDBAutoGenerateStrategy.CREATE)
    public String getId() {
        return id;
    }

    @DynamoDBAttribute
    public String getTitle() {
        return title;
    }

    @DynamoDBAttribute
    public String getDescription() {
        return description;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
