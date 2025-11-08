-- V3__create_indexes.sql
-- Additional or explicitly named indexes (if needed)

-- Indexes for ads
CREATE INDEX IF NOT EXISTS idx_ads_type_ads ON ads(type_ads);
CREATE INDEX IF NOT EXISTS idx_ads_time_created ON ads(time_created);

-- Indexes for insights/keywords
CREATE INDEX IF NOT EXISTS idx_insights_report_date ON insights(report_date);
CREATE INDEX IF NOT EXISTS idx_keywords_keyword ON keywords(keyword);

-- Indexes for generated content and scheduled posts
CREATE INDEX IF NOT EXISTS idx_generated_content_status ON generated_content(status);
CREATE INDEX IF NOT EXISTS idx_generated_content_type ON generated_content(content_type);
CREATE INDEX IF NOT EXISTS idx_scheduled_posts_time ON scheduled_posts(scheduled_time);
CREATE INDEX IF NOT EXISTS idx_scheduled_posts_status ON scheduled_posts(status);
CREATE INDEX IF NOT EXISTS idx_scheduled_posts_platform ON scheduled_posts(platform);

-- Indexes for publish history and trend analysis
CREATE INDEX IF NOT EXISTS idx_publish_history_scheduled_post_id ON publish_history(scheduled_post_id);
CREATE INDEX IF NOT EXISTS idx_publish_history_action ON publish_history(action);
CREATE INDEX IF NOT EXISTS idx_trend_analysis_date ON trend_analysis(analysis_date);
