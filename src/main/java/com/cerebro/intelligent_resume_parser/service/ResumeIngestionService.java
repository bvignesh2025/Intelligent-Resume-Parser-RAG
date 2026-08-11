package com.cerebro.intelligent_resume_parser.service;

import com.cerebro.intelligent_resume_parser.config.AiConfig.ResumeStructuralParser;
import com.cerebro.intelligent_resume_parser.dto.SectionMappingResponse;
import com.cerebro.intelligent_resume_parser.event.ResumeIngestedEvent;
import com.cerebro.intelligent_resume_parser.model.Resume;
import com.cerebro.intelligent_resume_parser.model.ResumeChunk;
import com.cerebro.intelligent_resume_parser.repository.ResumeChunkRepository;
import com.cerebro.intelligent_resume_parser.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeIngestionService {

    private final ResumeRepository resumeRepository;
    private final ResumeChunkRepository resumeChunkRepository;
    private final ResumeStructuralParser structuralParser;
    private final ApplicationEventPublisher eventPublisher;

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    @Transactional
    public Resume processAndStageResume(Path pdfPath, String candidateName, String email) throws Exception {
        System.out.println("[Ingestion] Starting Tika text extraction for: " + candidateName);
        String extractedText;
        try (InputStream inputStream = Files.newInputStream(pdfPath)) {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(inputStream, handler, metadata);
            extractedText = handler.toString().replaceAll("[ \\t]+", " ").trim();
        }
        System.out.println("[Ingestion] Tika successfully extracted " + extractedText.length() + " characters.");

        // 1. Stage parent record
        Resume resume = Resume.builder()
                .candidateName(candidateName)
                .email(email)
                .rawText(extractedText)
                .processingStatus("PROCESSING")
                .build();
        resume = resumeRepository.save(resume);

        // 2. Pass One: Dynamic LLM Layout Segmentation
        System.out.println("[Ingestion] Sending payload to Gemini API... (This might take 2-4 seconds)");
        SectionMappingResponse mappedLayout = structuralParser.segmentResume(extractedText);
        System.out.println("[Ingestion] Gemini responded successfully! Discovered sections: " + mappedLayout.sections().size());

        int totalChunkCounter = 0;

        // 3. Pass Two: Local Text Slicing Overlap Window
        for (SectionMappingResponse.DiscoveredSection section : mappedLayout.sections()) {
            String cleanText = section.sectionContent().replaceAll("\\s+", " ").trim();
            List<String> textSlices = sliceTextWithOverlap(cleanText);

            for (String slice : textSlices) {
                ResumeChunk chunk = ResumeChunk.builder()
                        .resumeId(resume.getId())
                        .sectionName(section.standardizedType())
                        .chunkContent(slice)
                        .chunkIndex(totalChunkCounter++)
                        .build();

                resumeChunkRepository.save(chunk);
            }
        }

        // 4. Outside both loops! Process tracking and background event fire
        System.out.println("[Ingestion] Successfully stored " + totalChunkCounter + " text slices in pgvector staging.");
        resume.setYearsOfExperience(mappedLayout.yearsOfExperience() != null ? mappedLayout.yearsOfExperience() : 0);
        resume.setSkills(mappedLayout.skills());
        resume.setProcessingStatus("STAGED_PENDING_EMBEDDING");
        Resume savedResume = resumeRepository.save(resume);

        // Fire and forget into the background thread pool!
        System.out.println("[Ingestion] Dispatching background event for async embedding generation...");
        eventPublisher.publishEvent(new ResumeIngestedEvent(savedResume.getId()));

        return savedResume;
    }

    private List<String> sliceTextWithOverlap(String text) {
        List<String> chunks = new ArrayList<>();
        int textLength = text.length();
        int start = 0;

        while (start < textLength) {
            int end = Math.min(start + CHUNK_SIZE, textLength);
            chunks.add(text.substring(start, end));

            start += (CHUNK_SIZE - CHUNK_OVERLAP);
            if (start >= textLength || end == textLength) {
                break;
            }
        }
        return chunks;
    }
}