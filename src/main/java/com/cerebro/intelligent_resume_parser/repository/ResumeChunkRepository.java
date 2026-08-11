package com.cerebro.intelligent_resume_parser.repository;

import com.cerebro.intelligent_resume_parser.model.ResumeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResumeChunkRepository extends JpaRepository<ResumeChunk, UUID> {

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO resume_chunks (id, resume_id, section_name, chunk_content, embedding, chunk_index) " +
            "VALUES (gen_random_uuid(), :resumeId, :sectionName, :chunkContent, cast(:embeddingString as vector), :chunkIndex)",
            nativeQuery = true)
    void insertVectorChunk(@Param("resumeId") UUID resumeId,
                           @Param("sectionName") String sectionName,
                           @Param("chunkContent") String chunkContent,
                           @Param("embeddingString") String embeddingString,
                           @Param("chunkIndex") Integer chunkIndex);

    @Modifying
    @Transactional
    @Query(value = "UPDATE resume_chunks SET embedding = cast(:embedding as vector) WHERE id = :id", nativeQuery = true)
    void updateEmbedding(@Param("id") UUID id, @Param("embedding") String embedding);
    List<ResumeChunk> findByResumeId(UUID resumeId);

    @Query(value = "WITH chunk_matches AS (" +
            "    SELECT " +
            "        r.id as resume_id," +
            "        rc.section_name as section_name," +
            "        rc.chunk_content as chunk_content," +
            "        (1 - (rc.embedding <=> cast(:queryVectorString as vector))) as similarity_score," +
            "        ts_rank_cd(to_tsvector('english', rc.chunk_content), plainto_tsquery('english', :rawQuery)) as keyword_score" +
            "    FROM resume_chunks rc" +
            "    JOIN resumes r ON rc.resume_id = r.id" +
            "    WHERE r.processing_status = 'PROCESSED_AND_INDEXED'" +
            "      AND (:minExperience IS NULL OR r.years_of_experience >= :minExperience)" +
            "      AND (:excludeSkillsFilter = true OR r.skills && cast(:targetSkills as text[]))" +
            ")" +
            "SELECT " +
            "    r.id as resumeId," +
            "    r.candidate_name as candidateName," +
            "    r.email as email," +
            "    r.years_of_experience as yearsOfExperience," +
            "    r.skills as skills," +
            "    MAX(cm.similarity_score) as similarityScore," +
            "    MAX(cm.keyword_score) as keywordScore," +
            "    (MAX(cm.similarity_score) * 0.7 + MAX(cm.keyword_score) * 0.3) as hybridScore," +
            "    COALESCE((SELECT section_name FROM chunk_matches WHERE resume_id = r.id ORDER BY (similarity_score * 0.7 + keyword_score * 0.3) DESC LIMIT 1), 'SUMMARY') as matchedSection," +
            "    COALESCE((SELECT chunk_content FROM chunk_matches WHERE resume_id = r.id ORDER BY (similarity_score * 0.7 + keyword_score * 0.3) DESC LIMIT 1), '') as matchedSnippet " +
            "FROM chunk_matches cm " +
            "JOIN resumes r ON cm.resume_id = r.id " +
            "GROUP BY r.id, r.candidate_name, r.email, r.years_of_experience, r.skills " +
            "ORDER BY hybridScore DESC", nativeQuery = true)
    List<CandidateSearchProjection> searchCandidates(@Param("queryVectorString") String queryVectorString,
                                                     @Param("rawQuery") String rawQuery,
                                                     @Param("minExperience") Integer minExperience,
                                                     @Param("targetSkills") String[] targetSkills,
                                                     @Param("excludeSkillsFilter") boolean excludeSkillsFilter);
}