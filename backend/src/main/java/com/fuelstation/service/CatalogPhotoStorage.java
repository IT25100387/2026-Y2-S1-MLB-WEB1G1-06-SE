package com.fuelstation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class CatalogPhotoStorage {
    private final Path root;
    public CatalogPhotoStorage(@Value("${app.catalog-photo-directory:uploads/catalog}") String directory) {
        root = Paths.get(directory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile image) {
        if (image == null || image.isEmpty()) return null;
        if (image.getSize() > 5_000_000) throw new IllegalArgumentException("Use a PNG, JPEG or WebP photo under 5 MB");
        try {
            byte[] bytes = image.getBytes();
            String extension;
            if (bytes.length >= 8 && bytes[0] == (byte)137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71 && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) extension = ".png";
            else if (bytes.length >= 3 && bytes[0] == (byte)255 && bytes[1] == (byte)216 && bytes[2] == (byte)255) extension = ".jpg";
            else if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') extension = ".webp";
            else throw new IllegalArgumentException("Select a PNG, JPEG or WebP photo");
            String name = UUID.randomUUID() + extension;
            Files.createDirectories(root);
            Files.write(root.resolve(name), bytes, StandardOpenOption.CREATE_NEW);
            return "/uploads/catalog/" + name;
        } catch (IOException error) {
            throw new IllegalStateException("The photo could not be saved. Try again.", error);
        }
    }

    // Remove a newly uploaded file if the accompanying record was rejected.
    public void discard(String url) {
        if (url == null || !url.matches("/uploads/catalog/[a-f0-9-]+\\.(png|jpg|webp)")) return;
        try { Files.deleteIfExists(root.resolve(url.substring(url.lastIndexOf('/') + 1))); }
        catch (IOException ignored) { /* Keep the original validation error for the caller. */ }
    }
}
