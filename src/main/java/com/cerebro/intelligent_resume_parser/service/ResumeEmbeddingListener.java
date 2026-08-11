package com.cerebro.intelligent_resume_parser.service;

import com.cerebro.intelligent_resume_parser.event.ResumeIngestedEvent;
import com.cerebro.intelligent_resume_parser.model.Resume;
import com.cerebro.intelligent_resume_parser.model.ResumeChunk;
import com.cerebro.intelligent_resume_parser.repository.ResumeChunkRepository;
import com.cerebro.intelligent_resume_parser.repository.ResumeRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ResumeEmbeddingListener {

    private final ResumeRepository resumeRepository;
    private final ResumeChunkRepository resumeChunkRepository;
    private final EmbeddingModel embeddingModel;


    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @EventListener
    public void handleResumeIngestedEvent(ResumeIngestedEvent event) {
        UUID resumeId = event.getResumeId();
        System.out.println("[Embedding Worker] Triggered after main database commit for ID: " + resumeId);
        System.out.println("[Embedding Worker] Asynchronously processing resume ID: " + resumeId);

        // 1. Fetch chunks that need vector calculations
        List<ResumeChunk> chunks = resumeChunkRepository.findByResumeId(resumeId);
        if (chunks.isEmpty()) {
            System.out.println("[Embedding Worker] No chunks found for resume ID: " + resumeId);
            return;
        }

        System.out.println("[Embedding Worker] Computing vector profiles for " + chunks.size() + " segments...");

        for (ResumeChunk chunk : chunks) {
            try {
                float[] vector = embeddingModel.embed(chunk.getChunkContent()).content().vector();
                String vectorString = Arrays.toString(vector);

                // Force a raw SQL UPDATE execution directly by ID
                resumeChunkRepository.updateEmbedding(chunk.getId(), vectorString);

                System.out.println("[Embedding Worker] Directly updated DB for chunk index: " + chunk.getChunkIndex());
            } catch (Exception e) {
                System.err.println("[Embedding Worker] Failed at index " + chunk.getChunkIndex() + ": " + e.getMessage());
            }
        }

        // 4. Flip parent record status to ready
        Resume resume = resumeRepository.findById(resumeId).orElseThrow();
        resume.setProcessingStatus("PROCESSED_AND_INDEXED");
        resumeRepository.save(resume);

        System.out.println("[Embedding Worker] Success! Vector matrix calculated and indexed for resume ID: " + resumeId);
    }
}