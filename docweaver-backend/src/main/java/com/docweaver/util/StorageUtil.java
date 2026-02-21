package com.docweaver.util;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class StorageUtil {

    public Path ensureFolder(String folder) throws IOException {
        Path path = Paths.get(folder).toAbsolutePath().normalize();
        Files.createDirectories(path);
        return path;
    }

    public Path resolveSafe(Path root, String fileName) {
        Path resolved = root.resolve(fileName).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Path traversal blocked");
        }
        return resolved;
    }

    public Path uniquePath(Path root, String baseName, String extension) {
        String extSuffix = extension == null || extension.isBlank() ? "" : "." + extension;
        String candidateName = baseName + extSuffix;
        Path candidate = resolveSafe(root, candidateName);
        int index = 1;
        while (Files.exists(candidate)) {
            candidateName = baseName + "-" + index + extSuffix;
            candidate = resolveSafe(root, candidateName);
            index++;
        }
        return candidate;
    }
}
