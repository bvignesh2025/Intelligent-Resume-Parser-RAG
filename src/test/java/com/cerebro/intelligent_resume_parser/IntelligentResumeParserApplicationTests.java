package com.cerebro.intelligent_resume_parser;

import com.cerebro.intelligent_resume_parser.repository.CandidateSearchProjection;
import com.cerebro.intelligent_resume_parser.service.ResumeSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class IntelligentResumeParserApplicationTests {

	@Autowired
	private ResumeSearchService resumeSearchService;

	@Test
	void contextLoads() {
	}

	@Test
	void testSearchCandidatesEmptyDatabase() {
		List<CandidateSearchProjection> results = resumeSearchService.searchCandidates(
				"Senior Software Engineer", 
				2, 
				List.of("Java", "Spring Boot")
		);
		assertNotNull(results, "Search results should not be null even if the database is empty");
		System.out.println("[Test] Candidate search returned results count: " + results.size());
	}

}
