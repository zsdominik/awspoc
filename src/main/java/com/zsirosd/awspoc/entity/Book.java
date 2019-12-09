package com.zsirosd.awspoc.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.*;

@DynamoDBTable(tableName = "Books")
public class Book {

    private String id;
    private String title;
    private String description;
    private String imageUrl;
    private String imageName;

    public Book() { }

    public Book(String id, String title, String description, String imageUrl, String imageName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.imageName = imageName;
    }

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

    @DynamoDBAttribute
    public String getImageUrl() {
        return imageUrl;
    }

    @DynamoDBAttribute
    public String getImageName() {
        return imageName;
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

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
}
