package com.cerebro.intelligent_resume_parser.repository;

import java.util.List;
import java.util.UUID;

public interface CandidateSearchProjection {
    UUID getResumeId();
    String getCandidateName();
    String getEmail();
    Integer getYearsOfExperience();
    List<String> getSkills();
    Double getSimilarityScore();
    Double getKeywordScore();
    Double getHybridScore();
    String getMatchedSection();
    String getMatchedSnippet();
}
