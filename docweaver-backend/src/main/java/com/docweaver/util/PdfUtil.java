package com.docweaver.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
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
import java.util.List;

@Component
public class PdfUtil {

    public void createSingleImagePdf(Path imagePath, Path outputPdf) throws IOException {
        createMultiImagePdf(List.of(new PageInput(imagePath, 0)), outputPdf);
    }

    public void createMultiImagePdf(List<PageInput> pageInputs, Path outputPdf) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (PageInput pageInput : pageInputs) {
                Path imagePath = pageInput.imagePath();
                int rotation = normalizeRotation(pageInput.rotationDegrees());
                byte[] bytes = loadImageBytes(imagePath, rotation);
                PDImageXObject image = PDImageXObject.createFromByteArray(document, bytes, imagePath.getFileName().toString());
                float imageWidth = image.getWidth();
                float imageHeight = image.getHeight();
                PDRectangle pageSize = new PDRectangle(imageWidth, imageHeight);
                PDPage page = new PDPage(pageSize);
                document.addPage(page);

                float margin = computeMargin(imageWidth, imageHeight);
                float availableWidth = Math.max(1f, pageSize.getWidth() - (margin * 2f));
                float availableHeight = Math.max(1f, pageSize.getHeight() - (margin * 2f));
                float scale = Math.min(availableWidth / imageWidth, availableHeight / imageHeight);
                float drawWidth = imageWidth * scale;
                float drawHeight = imageHeight * scale;
                float drawX = (pageSize.getWidth() - drawWidth) / 2f;
                float drawY = (pageSize.getHeight() - drawHeight) / 2f;

                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.drawImage(image, drawX, drawY, drawWidth, drawHeight);
                }
            }
            document.save(outputPdf.toFile());
        }
    }

    private byte[] loadImageBytes(Path imagePath, int rotation) throws IOException {
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

        BufferedImage rotated = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(rotated, "png", out);
        return out.toByteArray();
    }

    private int normalizeRotation(int rotationDegrees) {
        int normalized = rotationDegrees % 360;
        if (normalized < 0) {
            normalized += 360;
        }
        return normalized;
    }

    private float computeMargin(float imageWidth, float imageHeight) {
        float minDimension = Math.min(imageWidth, imageHeight);
        float adaptive = minDimension * 0.04f;
        return Math.max(8f, Math.min(36f, adaptive));
    }

    public record PageInput(Path imagePath, int rotationDegrees) {
    }
}
