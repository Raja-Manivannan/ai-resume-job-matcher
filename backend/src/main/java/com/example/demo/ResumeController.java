package com.example.demo;

import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resumes")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class ResumeController {

    private final GeminiService geminiService;
    private final ResumeScanRepository repository;
    private final Tika tika = new Tika();

    public ResumeController(GeminiService geminiService, ResumeScanRepository repository) {
        this.geminiService = geminiService;
        this.repository = repository;
    }

    @PostMapping("/upload-pdf")
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please upload a valid PDF file."));
        }

        try (InputStream inputStream = file.getInputStream()) {
            String extractedText = tika.parseToString(inputStream).trim();

            if (extractedText.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(Map.of("error", "The uploaded PDF contains no readable text."));
            }

            return ResponseEntity.ok(Map.of("text", extractedText));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to parse document: " + e.getMessage()));
        }
    }

    @PostMapping("/analyze")
    public ResponseEntity<ScanResponse> analyze(@RequestBody ScanRequest request) {
        ScanResponse response = geminiService.evaluateMatch(request.getResumeText(), request.getJobDescription());

        ResumeScan scan = new ResumeScan(
                request.getResumeText(),
                request.getJobDescription(),
                response.getMatchScore(),
                response.getFeedback()
        );
        repository.save(scan);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ResumeScan>> getScanHistory() {
        return ResponseEntity.ok(repository.findAll());
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<?> deleteScan(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Scan not found with id: " + id));
        }
        repository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Scan deleted successfully"));
    }
}