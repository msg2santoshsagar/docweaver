package com.docweaver.util;

import java.text.Normalizer;
import java.util.Locale;

public final class FilenameUtil {

    private FilenameUtil() {
    }

    public static String sanitizeBaseName(String input) {
        if (input == null || input.isBlank()) {
            return "document";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._\\-\\s]", " ")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^[\\-._]+|[\\-._]+$", "");

        return normalized.isBlank() ? "document" : normalized;
    }

    public static String extensionFromFileName(String fileName) {
        if (fileName == null) {
            return "";
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
