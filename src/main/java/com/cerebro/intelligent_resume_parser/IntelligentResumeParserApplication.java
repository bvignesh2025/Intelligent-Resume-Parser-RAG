package com.cerebro.intelligent_resume_parser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class IntelligentResumeParserApplication {

	public static void main(String[] args) {
		SpringApplication.run(IntelligentResumeParserApplication.class, args);
	}

}
