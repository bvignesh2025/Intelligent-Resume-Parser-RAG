package com.cerebro.intelligent_resume_parser.event;

import java.util.UUID;

public class ResumeIngestedEvent {
    private final UUID resumeId;

    public ResumeIngestedEvent(UUID resumeId) {
        this.resumeId = resumeId;
    }

    // Explicitly declare the missing getter symbol
    public UUID getResumeId() {
        return this.resumeId;
    }
}