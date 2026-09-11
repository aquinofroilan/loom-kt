ALTER TABLE journal_entry_lines
ADD COLUMN cost_center_id UUID REFERENCES cost_centers(id);

CREATE INDEX idx_jel_cost_center ON journal_entry_lines(cost_center_id);
