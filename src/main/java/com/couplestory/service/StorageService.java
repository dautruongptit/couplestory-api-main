package com.couplestory.service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

public interface StorageService {
    String store(MultipartFile file, String filename);
    String store(InputStream inputStream, String filename, String contentType);
    void delete(String filename);
    String getFileUrl(String filename);
}
