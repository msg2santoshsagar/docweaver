package com.docweaver.controller;

import com.docweaver.dto.NameSuggestionRequest;
import com.docweaver.dto.NameSuggestionResponse;
import com.docweaver.service.SuggestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suggestions")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @PostMapping("/name")
    public NameSuggestionResponse suggestName(@Valid @RequestBody NameSuggestionRequest request) {
        return suggestionService.suggest(request);
    }
}
