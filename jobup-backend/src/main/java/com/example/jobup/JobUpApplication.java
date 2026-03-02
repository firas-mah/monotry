package com.example.jobup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
public class JobUpApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobUpApplication.class, args);
    }

}
