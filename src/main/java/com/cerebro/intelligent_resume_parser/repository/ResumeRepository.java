package com.cerebro.intelligent_resume_parser.repository;

import com.cerebro.intelligent_resume_parser.model.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, UUID> {
    long countByProcessingStatus(String processingStatus);
}