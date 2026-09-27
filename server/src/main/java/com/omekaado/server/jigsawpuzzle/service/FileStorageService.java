package com.omekaado.server.jigsawpuzzle.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service 
public class FileStorageService {
    private final Path storageRoot;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) throws IOException {
        this.storageRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(storageRoot); // only create if not exist
         
    }

    public String store(MultipartFile file) throws IOException{
        String originalFilename = StringUtils.cleanPath(
                Objects.requireNonNull(file.getOriginalFilename()));
        String ext = StringUtils.getFilenameExtension(originalFilename);

        String newFilename = UUID.randomUUID() + (ext != null ? "." + ext : "");

        Path targetPath = storageRoot.resolve(newFilename).normalize();

        if (!targetPath.getParent().equals(storageRoot)) {
            throw new IllegalArgumentException("Đường dẫn file không hợp lệ");
        }

        try (InputStream is = file.getInputStream()) {
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } 

        return newFilename; 
    }

    public boolean delete(String filename) throws IOException {
        if (!StringUtils.hasText(filename)) {
            return false;
        }
        Path path = resolve(filename);
        return Files.deleteIfExists(path);
    }

    public Path resolve(String filename) {
        Path path = storageRoot.resolve(filename).normalize();
        if (!path.getParent().equals(storageRoot)) {
            throw new IllegalArgumentException("File path invalid");
        }
        return path;
    }
}
