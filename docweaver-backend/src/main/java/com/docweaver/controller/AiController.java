package com.docweaver.controller;

import com.docweaver.dto.AutoCategorizeRequest;
import com.docweaver.dto.AutoCategorizeResponse;
import com.docweaver.dto.AiStatusResponse;
import com.docweaver.entity.AppConfig;
import com.docweaver.service.AppConfigService;
import com.docweaver.service.AutoCategorizeService;
import com.docweaver.service.LocalAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AutoCategorizeService autoCategorizeService;
    private final AppConfigService appConfigService;
    private final LocalAiService localAiService;

    @PostMapping("/auto-categorize")
    public AutoCategorizeResponse autoCategorize(@RequestBody AutoCategorizeRequest request) {
        return autoCategorizeService.autoCategorize(request);
    }

    @GetMapping("/status")
    public AiStatusResponse status() {
        AppConfig config = appConfigService.getOrCreate();
        LocalAiService.AiStatus status = localAiService.status(config);
        return new AiStatusResponse(
                status.enabled(),
                status.available(),
                status.configuredModel(),
                status.endpoint(),
                status.reason(),
                status.availableModels()
        );
    }
}
