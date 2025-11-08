-- V2__create_content_tables.sql
-- Create generated_content, scheduled_posts, publish_history, trend_analysis

CREATE TABLE IF NOT EXISTS generated_content (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    platform VARCHAR(50),

    based_on_keywords JSON,
    based_on_trends JSON,
    trend_score DECIMAL(5,2),

    ai_model VARCHAR(50),
    generation_prompt TEXT,

    status VARCHAR(20) DEFAULT 'DRAFT',
    approved_by BIGINT,
    approved_at TIMESTAMP NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_status ON generated_content(status);
CREATE INDEX idx_content_type ON generated_content(content_type);
CREATE INDEX idx_created_at ON generated_content(created_at);


CREATE TABLE IF NOT EXISTS scheduled_posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content_id BIGINT NOT NULL,

    platform VARCHAR(50) NOT NULL,
    platform_page_id VARCHAR(100),
    scheduled_time TIMESTAMP NOT NULL,

    post_type VARCHAR(50),
    media_urls JSON,
    hashtags JSON,
    call_to_action VARCHAR(100),

    status VARCHAR(20) DEFAULT 'PENDING',
    published_at TIMESTAMP NULL,
    publish_error TEXT NULL,
    retry_count INT DEFAULT 0,

    post_id VARCHAR(100),
    likes_count INT DEFAULT 0,
    comments_count INT DEFAULT 0,
    shares_count INT DEFAULT 0,
    reach INT DEFAULT 0,

    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (content_id) REFERENCES generated_content(id)
);

CREATE INDEX idx_scheduled_time ON scheduled_posts(scheduled_time);
CREATE INDEX idx_status_scheduled ON scheduled_posts(status);
CREATE INDEX idx_platform ON scheduled_posts(platform);


CREATE TABLE IF NOT EXISTS publish_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scheduled_post_id BIGINT NOT NULL,

    action VARCHAR(50) NOT NULL,
    status VARCHAR(20),
    message TEXT,
    metadata JSON,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (scheduled_post_id) REFERENCES scheduled_posts(id)
);

CREATE INDEX idx_scheduled_post_id ON publish_history(scheduled_post_id);
CREATE INDEX idx_action ON publish_history(action);


CREATE TABLE IF NOT EXISTS trend_analysis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    analysis_date DATE NOT NULL,

    trending_keywords JSON,
    trending_topics JSON,
    competitor_activity JSON,

    content_suggestions JSON,
    optimal_posting_times JSON,

    ai_summary TEXT,
    confidence_score DECIMAL(5,2),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_analysis_date ON trend_analysis(analysis_date);
