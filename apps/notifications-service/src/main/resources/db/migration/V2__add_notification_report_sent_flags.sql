ALTER TABLE notification_reports
    ADD COLUMN email_sent_at TIMESTAMPTZ NULL,
    ADD COLUMN discord_sent_at TIMESTAMPTZ NULL,
    ADD COLUMN telegram_sent_at TIMESTAMPTZ NULL;
