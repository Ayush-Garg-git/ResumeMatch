package com.jobreadiness.copilot.storage;

import com.jobreadiness.copilot.common.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path baseStorageLocation;

    public LocalFileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.baseStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create the base directory where the uploaded files will be stored.", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String subDir) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")) {
            throw new BadRequestException("Filename contains invalid path sequence: " + originalFilename);
        }

        String fileExtension = "";
        int extIndex = originalFilename.lastIndexOf('.');
        if (extIndex >= 0) {
            fileExtension = originalFilename.substring(extIndex);
        }
        String storedFilename = UUID.randomUUID().toString() + fileExtension;

        Path targetSubDir = this.baseStorageLocation.resolve(subDir).normalize();
        Files.createDirectories(targetSubDir);

        Path targetLocation = targetSubDir.resolve(storedFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return Paths.get(subDir).resolve(storedFilename).toString().replace('\\', '/');
    }

    @Override
    public byte[] loadFile(String filePath) throws IOException {
        Path targetPath = this.baseStorageLocation.resolve(filePath).normalize();
        if (!targetPath.startsWith(this.baseStorageLocation)) {
            throw new SecurityException("Cannot load files outside of the allowed directory");
        }
        return Files.readAllBytes(targetPath);
    }

    @Override
    public void deleteFile(String filePath) throws IOException {
        Path targetPath = this.baseStorageLocation.resolve(filePath).normalize();
        if (!targetPath.startsWith(this.baseStorageLocation)) {
            throw new SecurityException("Cannot delete files outside of the allowed directory");
        }
        Files.deleteIfExists(targetPath);
    }
}
