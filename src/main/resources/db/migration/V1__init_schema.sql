-- 1. Enable the pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Create the resume audit & metadata table
CREATE TABLE resumes (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         candidate_name VARCHAR(255),
                         email VARCHAR(255),
                         years_of_experience INT DEFAULT 0,
                         skills TEXT[],
                         raw_text TEXT,
                         processing_status VARCHAR(50) DEFAULT 'PENDING',
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Create the chunks table for fine-grained retrieval
CREATE TABLE resume_chunks (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               resume_id UUID REFERENCES resumes(id) ON DELETE CASCADE,
                               section_name VARCHAR(100),
                               chunk_content TEXT,
                               embedding vector(3072),
                               chunk_index INT
);

-- 4. B-Tree Indexes for metadata filtering
CREATE INDEX idx_resumes_skills ON resumes USING gin(skills);
CREATE INDEX idx_resumes_experience ON resumes(years_of_experience);