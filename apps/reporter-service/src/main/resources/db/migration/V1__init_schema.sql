CREATE TABLE news_items (
    id UUID PRIMARY KEY,
    source VARCHAR(150) NOT NULL,
    url VARCHAR(512) NOT NULL,
    title VARCHAR(512) NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL,
    summary TEXT,
    hash VARCHAR(128) NOT NULL UNIQUE,
    ingested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_news_items_published_at ON news_items(published_at);
CREATE INDEX idx_news_items_ingested_at ON news_items(ingested_at);

CREATE TABLE daily_reports (
    id UUID PRIMARY KEY,
    report_date DATE NOT NULL UNIQUE,
    summary TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_daily_reports_report_date ON daily_reports(report_date);
