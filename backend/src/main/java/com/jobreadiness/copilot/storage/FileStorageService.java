package com.jobreadiness.copilot.storage;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface FileStorageService {
    String storeFile(MultipartFile file, String subDir) throws IOException;
    byte[] loadFile(String filePath) throws IOException;
    void deleteFile(String filePath) throws IOException;
}
