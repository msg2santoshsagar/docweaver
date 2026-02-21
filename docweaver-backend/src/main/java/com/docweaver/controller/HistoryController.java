package com.docweaver.controller;

import com.docweaver.dto.GeneratedDocumentDto;
import com.docweaver.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping
    public List<GeneratedDocumentDto> list() {
        return historyService.list();
    }
}
