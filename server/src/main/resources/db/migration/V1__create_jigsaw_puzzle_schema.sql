CREATE TABLE jigsaw_puzzle (
  id BIGSERIAL PRIMARY KEY,
  user_id INT NOT NULL,
  image_path VARCHAR(500) NOT NULL,
  seed INT NOT NULL,
  width INT NOT NULL CHECK (width > 0),
  height INT NOT NULL CHECK (height > 0),
  piece_number INT NOT NULL CHECK (piece_number >= 4 AND piece_number <= 1000),
  row_number INT NOT NULL CHECK (row_number > 0),
  column_number INT NOT NULL CHECK (column_number > 0),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  
  CONSTRAINT chk_piece_number_match_row_col CHECK (row_number * column_number >= piece_number)
);
