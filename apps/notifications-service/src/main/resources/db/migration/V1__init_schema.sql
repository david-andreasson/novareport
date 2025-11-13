CREATE TABLE notification_reports (
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL UNIQUE,
    report_date DATE NOT NULL UNIQUE,
    summary TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_reports_report_date ON notification_reports(report_date);
