package com.docweaver.util;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Component
public class ImageTransformUtil {

    public byte[] readRotatedImage(Path imagePath, int rotationDegrees, String formatHint) throws IOException {
        int rotation = normalizeRotation(rotationDegrees);
        if (rotation == 0) {
            return Files.readAllBytes(imagePath);
        }

        BufferedImage source = ImageIO.read(imagePath.toFile());
        if (source == null) {
            return Files.readAllBytes(imagePath);
        }

        int width = source.getWidth();
        int height = source.getHeight();
        int outputWidth = rotation == 90 || rotation == 270 ? height : width;
        int outputHeight = rotation == 90 || rotation == 270 ? width : height;

        boolean preserveAlpha = supportsAlpha(normalizeFormat(formatHint));
        int imageType = preserveAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage rotated = new BufferedImage(outputWidth, outputHeight, imageType);

        Graphics2D graphics = rotated.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        AffineTransform transform = new AffineTransform();
        switch (rotation) {
            case 90 -> {
                transform.translate(outputWidth, 0);
                transform.rotate(Math.toRadians(90));
            }
            case 180 -> {
                transform.translate(outputWidth, outputHeight);
                transform.rotate(Math.toRadians(180));
            }
            case 270 -> {
                transform.translate(0, outputHeight);
                transform.rotate(Math.toRadians(270));
            }
            default -> {
                return Files.readAllBytes(imagePath);
            }
        }

        graphics.drawImage(source, transform, null);
        graphics.dispose();

        String format = normalizeFormat(formatHint);
        if (format.isBlank()) {
            format = "png";
        }
        if (!ImageIO.getImageWritersByFormatName(format).hasNext()) {
            throw new IOException("Unsupported image format for rotation: " + formatHint);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(rotated, format, out)) {
            throw new IOException("Failed to encode rotated image as " + format);
        }
        return out.toByteArray();
    }

    private boolean supportsAlpha(String format) {
        return "png".equals(format) || "gif".equals(format);
    }

    private String normalizeFormat(String formatHint) {
        if (formatHint == null || formatHint.isBlank()) {
            return "";
        }
        String format = formatHint.trim().toLowerCase(Locale.ROOT);
        if ("jpg".equals(format)) {
            return "jpeg";
        }
        return format;
    }

    private int normalizeRotation(int rotationDegrees) {
        int normalized = rotationDegrees % 360;
        if (normalized < 0) {
            normalized += 360;
        }
        return normalized;
    }
}
