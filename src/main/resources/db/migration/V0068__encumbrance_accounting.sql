ALTER TABLE journal_entries
ADD COLUMN type VARCHAR(50) NOT NULL DEFAULT 'ACTUAL';

CREATE INDEX idx_je_type ON journal_entries(type);
