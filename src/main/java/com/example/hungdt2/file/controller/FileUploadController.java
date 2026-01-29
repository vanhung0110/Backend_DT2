package com.example.hungdt2.file.controller;

import com.example.hungdt2.common.ApiResponse;
import com.example.hungdt2.file.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        String fileUrl = fileUploadService.uploadFile(file);
        return ResponseEntity.ok(new ApiResponse<>(fileUrl));
    }

    @PostMapping("/upload-multiple")
    public ResponseEntity<ApiResponse<String[]>> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files)
            throws IOException {
        String[] fileUrls = fileUploadService.uploadMultipleFiles(files);
        return ResponseEntity.ok(new ApiResponse<>(fileUrls));
    }
}
