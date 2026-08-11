package com.cerebro.intelligent_resume_parser.service;

import com.cerebro.intelligent_resume_parser.repository.CandidateSearchProjection;
import com.cerebro.intelligent_resume_parser.repository.ResumeChunkRepository;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeSearchService {

    private final ResumeChunkRepository resumeChunkRepository;
    private final EmbeddingModel embeddingModel;

    public List<CandidateSearchProjection> searchCandidates(String query, Integer minExperience, List<String> skills) {
        System.out.println("[Search Service] Computing dense vector for search query: '" + query + "'");
        
        // 1. Generate query embedding vector
        float[] vector = embeddingModel.embed(query).content().vector();
        String queryVectorString = Arrays.toString(vector);

        // 2. Map optional skill filters
        String[] targetSkills = (skills != null && !skills.isEmpty()) ? skills.toArray(new String[0]) : null;
        boolean excludeSkillsFilter = (targetSkills == null);

        System.out.println("[Search Service] Executing hybrid query in database...");
        
        // 3. Perform hybrid lexical + semantic search
        return resumeChunkRepository.searchCandidates(
                queryVectorString,
                query,
                minExperience,
                targetSkills,
                excludeSkillsFilter
        );
    }
}
