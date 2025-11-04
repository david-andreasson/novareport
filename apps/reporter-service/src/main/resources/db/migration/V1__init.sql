CREATE TABLE news_items (
    id UUID PRIMARY KEY,
    source VARCHAR(150) NOT NULL,
    url VARCHAR(512) NOT NULL,
    title VARCHAR(512) NOT NULL,
    published_at TIMESTAMP NOT NULL,
    summary TEXT,
    hash VARCHAR(128) NOT NULL UNIQUE,
    ingested_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_news_items_published_at ON news_items (published_at);
CREATE INDEX idx_news_items_ingested_at ON news_items (ingested_at);

CREATE TABLE daily_reports (
    id UUID PRIMARY KEY,
    report_date DATE NOT NULL UNIQUE,
    summary TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
