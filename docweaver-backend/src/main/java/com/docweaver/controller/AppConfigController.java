package com.docweaver.controller;

import com.docweaver.dto.AppConfigDto;
import com.docweaver.service.AppConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class AppConfigController {

    private final AppConfigService appConfigService;

    @GetMapping
    public AppConfigDto read() {
        return appConfigService.read();
    }

    @PutMapping
    public AppConfigDto update(@RequestBody AppConfigDto request) {
        return appConfigService.update(request);
    }
}
