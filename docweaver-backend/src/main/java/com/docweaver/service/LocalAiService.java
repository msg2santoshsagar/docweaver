package com.docweaver.service;

import com.docweaver.entity.AppConfig;
import com.docweaver.util.FilenameUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class LocalAiService {

    private static final Logger log = LoggerFactory.getLogger(LocalAiService.class);
    private static final Duration OLLAMA_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration OLLAMA_INFERENCE_TIMEOUT = Duration.ofSeconds(300);
    private static final Duration OLLAMA_STATUS_TIMEOUT = Duration.ofSeconds(10);
    private static final int AI_MAX_IMAGE_DIMENSION = 1400;
    private static final int AI_TARGET_IMAGE_BYTES = 1_200_000;
    private static final float AI_JPEG_QUALITY = 0.82f;
    private static final float AI_JPEG_QUALITY_FALLBACK = 0.68f;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(OLLAMA_CONNECT_TIMEOUT)
            .build();

    public AiDocumentAnalysis analyzeImage(AppConfig config, String mimeType, String fileBytes) {
        if (!Boolean.TRUE.equals(config.getAiEnabled())) {
            return AiDocumentAnalysis.empty();
        }

        try {
            String optimizedImage = optimizeImageForInference(fileBytes, mimeType);
            String prompt = """
                    Classify this document image and return ONLY valid JSON.
                    No markdown, no prose.
                    {
                      "subject": "person/vendor/patient if visible else empty",
                      "documentDate": "YYYY-MM-DD if visible else empty",
                      "docType": "prescription|invoice|report|id|bill|receipt|other",
                      "subType": "short subtype or empty",
                      "attributes": "short extra attribute words or empty",
                      "groupKey": "same value for pages of same document if detectable else empty",
                      "pageNumber": 0 if unknown,
                      "totalPages": 0 if unknown,
                      "confidence": 0.0 to 1.0
                    }
                    """;

            ObjectNode requestPayload = objectMapper.createObjectNode();
            requestPayload.put("model", config.getAiModel());
            requestPayload.put("prompt", prompt);
            requestPayload.put("stream", false);
            requestPayload.put("format", "json");
            requestPayload.putArray("images").add(optimizedImage);
            ObjectNode options = requestPayload.putObject("options");
            options.put("temperature", 0.1);
            options.put("num_predict", 180);
            String requestBody = requestPayload.toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimTrailingSlash(config.getAiBaseUrl()) + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .timeout(OLLAMA_INFERENCE_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Ollama inference failed with status {}", response.statusCode());
                return AiDocumentAnalysis.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            String aiJson = root.path("response").asText("");
            if (aiJson.isBlank()) {
                log.warn("Ollama inference returned an empty response body.");
                return AiDocumentAnalysis.empty();
            }

            JsonNode ai = parseAiJson(aiJson);
            if (ai == null) {
                log.warn("Ollama response was not valid JSON.");
                return AiDocumentAnalysis.empty();
            }

            String subject = normalizeToken(ai.path("subject").asText(""));
            String documentDate = normalizeToken(ai.path("documentDate").asText(""));
            String docType = normalizeDocType(ai.path("docType").asText(""));
            String subType = normalizeToken(ai.path("subType").asText(""));
            String attributes = normalizeToken(ai.path("attributes").asText(""));
            String groupKey = normalizeToken(ai.path("groupKey").asText(""));
            int pageNumber = Math.max(0, ai.path("pageNumber").asInt(0));
            int totalPages = Math.max(0, ai.path("totalPages").asInt(0));
            double confidence = Math.max(0.0, Math.min(1.0, ai.path("confidence").asDouble(0.0)));

            return new AiDocumentAnalysis(subject, documentDate, docType, subType, attributes, groupKey, pageNumber, totalPages, confidence);
        } catch (Exception e) {
            log.warn("Ollama inference call failed: {}", e.getMessage());
            return AiDocumentAnalysis.empty();
        }
    }

    public AiStatus status(AppConfig config) {
        if (!Boolean.TRUE.equals(config.getAiEnabled())) {
            return new AiStatus(false, false, config.getAiModel(), config.getAiBaseUrl(), "AI is disabled in settings.", List.of());
        }

        String endpoint = trimTrailingSlash(config.getAiBaseUrl());
        String configuredModel = config.getAiModel() == null ? "" : config.getAiModel().trim();
        if (configuredModel.isBlank()) {
            return new AiStatus(true, false, configuredModel, endpoint, "AI model is not configured.", List.of());
        }

        try {
            HttpRequest tagsRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + "/api/tags"))
                    .timeout(OLLAMA_STATUS_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> tagsResponse = httpClient.send(tagsRequest, HttpResponse.BodyHandlers.ofString());
            if (tagsResponse.statusCode() < 200 || tagsResponse.statusCode() >= 300) {
                return new AiStatus(
                        true,
                        false,
                        configuredModel,
                        endpoint,
                        "Unable to read Ollama tags (" + tagsResponse.statusCode() + ").",
                        List.of()
                );
            }

            JsonNode tagsRoot = objectMapper.readTree(tagsResponse.body());
            List<String> availableModels = new ArrayList<>();
            for (JsonNode modelNode : tagsRoot.path("models")) {
                String modelName = modelNode.path("name").asText("");
                if (!modelName.isBlank()) {
                    availableModels.add(modelName);
                }
            }

            boolean modelFound = availableModels.stream().anyMatch(configuredModel::equals);
            if (!modelFound) {
                return new AiStatus(
                        true,
                        false,
                        configuredModel,
                        endpoint,
                        "Configured model is not installed in Ollama.",
                        availableModels
                );
            }

            return new AiStatus(true, true, configuredModel, endpoint, "OK", availableModels);
        } catch (Exception e) {
            return new AiStatus(
                    true,
                    false,
                    configuredModel,
                    endpoint,
                    "Cannot reach Ollama endpoint.",
                    List.of()
            );
        }
    }

    private JsonNode parseAiJson(String text) {
        try {
            return objectMapper.readTree(text);
        } catch (Exception ignored) {
            String trimmed = text.trim();
            if (trimmed.startsWith("```")) {
                int firstBrace = trimmed.indexOf('{');
                int lastBrace = trimmed.lastIndexOf('}');
                if (firstBrace >= 0 && lastBrace > firstBrace) {
                    trimmed = trimmed.substring(firstBrace, lastBrace + 1);
                }
            } else {
                int firstBrace = trimmed.indexOf('{');
                int lastBrace = trimmed.lastIndexOf('}');
                if (firstBrace >= 0 && lastBrace > firstBrace) {
                    trimmed = trimmed.substring(firstBrace, lastBrace + 1);
                }
            }
            try {
                return objectMapper.readTree(trimmed);
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }

    private String optimizeImageForInference(String fileBytes, String mimeType) {
        if (fileBytes == null || fileBytes.isBlank()) {
            return fileBytes;
        }

        try {
            byte[] originalBytes = Base64.getDecoder().decode(fileBytes);
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (source == null) {
                return fileBytes;
            }

            int sourceWidth = source.getWidth();
            int sourceHeight = source.getHeight();
            double scale = Math.min(
                    1.0d,
                    (double) AI_MAX_IMAGE_DIMENSION / Math.max(sourceWidth, sourceHeight)
            );
            int targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
            int targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));

            BufferedImage optimized = source;
            if (scale < 1.0d) {
                optimized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = optimized.createGraphics();
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
                graphics.dispose();
            }

            byte[] compressed = writeJpeg(optimized, AI_JPEG_QUALITY);
            if (compressed.length > AI_TARGET_IMAGE_BYTES) {
                compressed = writeJpeg(optimized, AI_JPEG_QUALITY_FALLBACK);
            }
            if (compressed.length == 0) {
                return fileBytes;
            }

            if (originalBytes.length <= AI_TARGET_IMAGE_BYTES && compressed.length >= originalBytes.length && isAlreadyVisionFriendly(mimeType)) {
                return fileBytes;
            }
            return Base64.getEncoder().encodeToString(compressed);
        } catch (Exception e) {
            log.debug("Failed to optimize image for AI inference: {}", e.getMessage());
            return fileBytes;
        }
    }

    private boolean isAlreadyVisionFriendly(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        String normalized = mimeType.toLowerCase(Locale.ROOT);
        return normalized.contains("jpeg") || normalized.contains("jpg") || normalized.contains("png");
    }

    private byte[] writeJpeg(BufferedImage image, float quality) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").hasNext()
                ? ImageIO.getImageWritersByFormatName("jpg").next()
                : null;
        if (writer == null) {
            return new byte[0];
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream imageOutput = new MemoryCacheImageOutputStream(outputStream)) {
            writer.setOutput(imageOutput);
            ImageWriteParam params = writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(Math.max(0.4f, Math.min(0.95f, quality)));
            }
            writer.write(null, new IIOImage(image, null, null), params);
            writer.dispose();
            return outputStream.toByteArray();
        } catch (Exception e) {
            writer.dispose();
            return new byte[0];
        }
    }

    private String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return FilenameUtil.sanitizeBaseName(value);
    }

    private String normalizeDocType(String value) {
        String normalized = normalizeToken(value).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "other";
        }
        if (normalized.contains("prescription")) return "prescription";
        if (normalized.contains("invoice")) return "invoice";
        if (normalized.contains("report")) return "report";
        if (normalized.contains("receipt")) return "receipt";
        if (normalized.equals("id") || normalized.contains("identity") || normalized.contains("license") || normalized.contains("passport")) {
            return "id";
        }
        if (normalized.contains("bill")) return "bill";
        return normalized;
    }

    private String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://host.docker.internal:11434";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public record AiDocumentAnalysis(
            String subject,
            String documentDate,
            String docType,
            String subType,
            String attributes,
            String groupKey,
            int pageNumber,
            int totalPages,
            double confidence
    ) {
        public static AiDocumentAnalysis empty() {
            return new AiDocumentAnalysis("", "", "other", "", "", "", 0, 0, 0.0);
        }
    }

    public record AiStatus(
            boolean enabled,
            boolean available,
            String configuredModel,
            String endpoint,
            String reason,
            List<String> availableModels
    ) {
    }
}
