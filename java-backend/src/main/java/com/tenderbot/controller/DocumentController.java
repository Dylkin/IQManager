package com.tenderbot.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Value("${document.storage.path:/tmp/tenderbot/documents}")
    private String documentStoragePath;

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String fileName) {
        Path filePath = Paths.get(documentStoragePath, fileName);
        File file = filePath.toFile();
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }
        // Security check: ensure the resolved path is within storage directory
        try {
            if (!filePath.toRealPath().startsWith(Paths.get(documentStoragePath).toRealPath())) {
                return ResponseEntity.status(403).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
