package com.cerebro.intelligent_resume_parser.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import com.cerebro.intelligent_resume_parser.dto.SectionMappingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;

@Configuration
public class AiConfig {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Bean
    public ChatModel geminiChatModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName("gemini-2.5-flash-lite")
                .temperature(0.0)
                .timeout(Duration.ofSeconds(10))
                .build();
    }

    public interface ResumeStructuralParser {
        @UserMessage("""
            Analyze the following unstructured raw text extracted from a resume. 
            Your task is to isolate and map out the structural sections, and extract key metadata.
            
            1. Identify and classify content sections into one of these standardized types: 'SUMMARY', 'EXPERIENCE', 'SKILLS', 'EDUCATION', 'PROJECTS'. 
               Ensure you extract the absolute exact 'rawHeadingText' used as the title anchor in the document text.
            2. Compute the estimated total years of work experience (as an integer). Sum up non-overlapping jobs.
            3. Extract a clean list of technical skills mentioned (e.g. programming languages, frameworks, databases, platforms, tools).
            
            ---
            RAW RESUME TEXT:
            {{rawText}}
            ---
            """)
        SectionMappingResponse segmentResume(@V("rawText") String rawText);
    }

    @Bean
    public ResumeStructuralParser resumeStructuralParser(ChatModel geminiChatModel) {
        return AiServices.builder(ResumeStructuralParser.class)
                .chatModel(geminiChatModel)
                .build();
    }

    @Bean
    public EmbeddingModel geminiEmbeddingModel() {
        // Corrected return mapping and linked to your active properties key
        return GoogleAiEmbeddingModel.builder()
                .apiKey(geminiApiKey)
                .modelName("gemini-embedding-001")
                .outputDimensionality(3072)
                .logRequestsAndResponses(true)
                .build();
    }
}