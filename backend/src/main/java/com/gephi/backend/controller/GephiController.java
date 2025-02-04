package com.gephi.backend.controller;

import com.gephi.backend.service.GephiService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@RestController
@RequestMapping("/api/graph")
public class GephiController {

    private final GephiService gephiService;

    public GephiController(GephiService gephiService) {
        this.gephiService = gephiService;
    }

    @PostMapping("/process")
    public ResponseEntity<byte[]> processGraph(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        try {
            // Geçici dosya oluştur ve dosyayı kaydet
            Path tempFilePath = Files.createTempFile("uploaded-graph", file.getOriginalFilename());
            Files.copy(file.getInputStream(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);
            
            File inputFile = tempFilePath.toFile();
            String outputFileName = tempFilePath.getParent().toString() + "/output.pdf";
            
            File outputFile = gephiService.processGraphAndExportPdf(inputFile, outputFileName);
            
            byte[] pdfContent = Files.readAllBytes(outputFile.toPath());
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=graph.pdf");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/pdf");
            
            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}