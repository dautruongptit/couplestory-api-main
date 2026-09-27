package com.couplestory.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class LocalDiskStorageService implements StorageService {
    private final Path rootLocation;

    public LocalDiskStorageService(@Value("${app.uploads-dir:uploads}") String uploadsDir) {
        this.rootLocation = Paths.get(uploadsDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    /**
     * Resolves filename under rootLocation and verifies the result cannot escape it.
     * Callers should already be passing sanitized filenames (see PhotoService), but this
     * boundary check is what actually prevents path traversal regardless of what any
     * caller passes in - resolving + normalizing alone does not guarantee containment.
     */
    private Path resolveWithinRoot(String filename) {
        Path destination = rootLocation.resolve(Paths.get(filename)).normalize();
        if (!destination.startsWith(rootLocation)) {
            throw new RuntimeException("Invalid filename");
        }
        return destination;
    }

    @Override
    public String store(MultipartFile file, String filename) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }
            Path destinationFile = resolveWithinRoot(filename);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return getFileUrl(filename);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    @Override
    public String store(InputStream inputStream, String filename, String contentType) {
        try {
            Path destinationFile = resolveWithinRoot(filename);
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return getFileUrl(filename);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            Path file = resolveWithinRoot(filename);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    @Override
    public String getFileUrl(String filename) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(filename)
                .toUriString();
    }
}
