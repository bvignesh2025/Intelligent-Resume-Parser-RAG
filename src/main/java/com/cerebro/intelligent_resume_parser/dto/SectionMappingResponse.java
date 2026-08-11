package com.cerebro.intelligent_resume_parser.dto;

import java.util.List;

public record SectionMappingResponse(
        List<DiscoveredSection> sections,
        Integer yearsOfExperience,
        List<String> skills
) {
    public record DiscoveredSection(
            String standardizedType, // e.g., "SUMMARY", "EXPERIENCE", "SKILLS", "EDUCATION", "PROJECTS"
            String rawHeadingText,   // The exact heading string text found in the PDF (e.g., "Professional Backstory")
            String sectionContent    // The entire block of text belonging to this section
    ) {}
}