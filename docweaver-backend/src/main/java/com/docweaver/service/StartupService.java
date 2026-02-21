package com.docweaver.service;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StartupService {

    @Bean
    ApplicationRunner initializeApp(AppConfigService appConfigService) {
        return args -> appConfigService.initializeStorage();
    }
}
