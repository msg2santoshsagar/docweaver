package com.docweaver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DocWeaverApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocWeaverApplication.class, args);
    }
}
