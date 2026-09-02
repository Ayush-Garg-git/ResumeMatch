-- V9: Add status column to jobs table for async processing
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'COMPLETED';
