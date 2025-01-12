CREATE TABLE IF NOT EXISTS accounts (
                                        id VARCHAR(255) PRIMARY KEY,
                                        login VARCHAR(255) NOT NULL,
                                        password VARCHAR(255) NOT NULL
);

INSERT INTO accounts (id, login, password)
VALUES ('1', 'admin', 'admin')
ON CONFLICT (id) DO NOTHING;