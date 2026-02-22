package com.docweaver.controller;

import com.docweaver.dto.HistoryPageDto;
import com.docweaver.entity.GeneratedType;
import com.docweaver.entity.ProcessingStatus;
import com.docweaver.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping
    public HistoryPageDto list(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "25") int size,
                               @RequestParam(required = false) ProcessingStatus status,
                               @RequestParam(required = false) GeneratedType type,
                               @RequestParam(required = false) String query) {
        return historyService.list(page, size, status, type, query);
    }
}
