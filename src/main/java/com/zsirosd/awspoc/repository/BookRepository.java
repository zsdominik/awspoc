package com.zsirosd.awspoc.repository;

import com.zsirosd.awspoc.entity.Book;
import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;

@EnableScan
public interface BookRepository extends CrudRepository<Book, String> { }
