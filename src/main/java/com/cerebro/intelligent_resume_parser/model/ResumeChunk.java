package com.cerebro.intelligent_resume_parser.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "resume_chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "resume_id", nullable = false)
    private UUID resumeId;

    @Column(name = "section_name", length = 100)
    private String sectionName;

    @Column(name = "chunk_content", columnDefinition = "TEXT")
    private String chunkContent;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(name = "embedding", columnDefinition = "vector", insertable = false, updatable = false)
    private String embedding;
}