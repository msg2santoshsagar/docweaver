package com.docweaver.controller;

import com.docweaver.dto.ProcessRequest;
import com.docweaver.dto.ProcessResponse;
import com.docweaver.service.ProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/process")
@RequiredArgsConstructor
public class ProcessingController {

    private final ProcessingService processingService;

    @PostMapping
    public ProcessResponse process(@RequestBody ProcessRequest request) {
        return processingService.process(request);
    }
}
