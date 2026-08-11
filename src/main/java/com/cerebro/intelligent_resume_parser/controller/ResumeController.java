package com.cerebro.intelligent_resume_parser.controller;

import com.cerebro.intelligent_resume_parser.model.Resume;
import com.cerebro.intelligent_resume_parser.repository.CandidateSearchProjection;
import com.cerebro.intelligent_resume_parser.repository.ResumeRepository;
import com.cerebro.intelligent_resume_parser.service.ResumeIngestionService;
import com.cerebro.intelligent_resume_parser.service.ResumeSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeIngestionService resumeIngestionService;
    private final ResumeSearchService resumeSearchService;
    private final ResumeRepository resumeRepository;

    @PostMapping("/upload")
    public ResponseEntity<Resume> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("email") String email) {

        try {
            Path tempFile = Files.createTempFile("upload-", file.getOriginalFilename());
            file.transferTo(tempFile.toFile());

            Resume processedResume = resumeIngestionService.processAndStageResume(tempFile, name, email);
            Files.deleteIfExists(tempFile);

            return ResponseEntity.status(HttpStatus.CREATED).body(processedResume);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<CandidateSearchProjection>> searchCandidates(
            @RequestParam("query") String query,
            @RequestParam(value = "minExperience", required = false) Integer minExperience,
            @RequestParam(value = "skills", required = false) List<String> skills) {

        try {
            List<CandidateSearchProjection> results = resumeSearchService.searchCandidates(query, minExperience, skills);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        long indexed = resumeRepository.countByProcessingStatus("PROCESSED_AND_INDEXED");
        long pending = resumeRepository.countByProcessingStatus("STAGED_PENDING_EMBEDDING");
        return ResponseEntity.ok(Map.of("indexed", indexed, "pending", pending));
    }
}