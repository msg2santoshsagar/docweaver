package com.docweaver.service;

import com.docweaver.dto.NameSuggestionRequest;
import com.docweaver.dto.NameSuggestionResponse;
import com.docweaver.util.FilenameUtil;
import org.springframework.stereotype.Service;

@Service
public class SuggestionService {

    public NameSuggestionResponse suggest(NameSuggestionRequest request) {
        String base = request.fileName();
        if (base.contains(".")) {
            base = base.substring(0, base.lastIndexOf('.'));
        }
        String suggested = FilenameUtil.sanitizeBaseName(base);
        return new NameSuggestionResponse(suggested, 0.72, "BASIC");
    }
}
