CREATE TABLE blocks (
                        block_number BIGINT PRIMARY KEY,
                        block_hash TEXT,
                        parent_hash TEXT,
                        timestamp BIGINT,
                        tx_count INT
);

CREATE TABLE transactions (
                              tx_hash TEXT PRIMARY KEY,
                              block_number BIGINT REFERENCES blocks(block_number),
                              from_address TEXT,
                              to_address TEXT,
                              value NUMERIC,
                              gas BIGINT,
                              gas_price BIGINT,
                              input_data TEXT,
                              status BOOLEAN
);

CREATE TABLE logs (
                      id SERIAL PRIMARY KEY,
                      tx_hash TEXT REFERENCES transactions(tx_hash),
                      log_index INT,
                      address TEXT,
                      topic0 TEXT,
                      topic1 TEXT,
                      topic2 TEXT,
                      topic3 TEXT,
                      data TEXT
);