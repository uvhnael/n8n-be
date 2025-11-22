    -- V1__create_base_tables.sql
-- Create base tables: ads, insights, keywords

CREATE TABLE IF NOT EXISTS ads (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ad_archive_id VARCHAR(100) UNIQUE NOT NULL,
    page_name VARCHAR(255),
    caption TEXT,
    type_ads VARCHAR(50),                      -- IMAGE, VIDEO, CAROUSEL
    url_ads_post VARCHAR(500),
    ai_analyze TEXT,
    img_url VARCHAR(500),
    video_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    time_created DATE,
    scraped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_type_ads ON ads(type_ads);
CREATE INDEX idx_time_created ON ads(time_created);


CREATE TABLE IF NOT EXISTS insights (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_date DATE NOT NULL,
    week_number INT,
    total_ads INT DEFAULT 0,
    image_count INT DEFAULT 0,
    video_count INT DEFAULT 0,
    carousel_count INT DEFAULT 0,
    dominant_format VARCHAR(50),
    cta_rate DECIMAL(5,2),
    most_active_day VARCHAR(20),
    ai_strategy_report TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_report_date ON insights(report_date);


CREATE TABLE IF NOT EXISTS keywords (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    insight_id BIGINT NOT NULL,
    keyword VARCHAR(100) NOT NULL,
    count INT DEFAULT 0,
    percentage DECIMAL(5,2),
    week VARCHAR(20),
    FOREIGN KEY (insight_id) REFERENCES insights(id)
);

CREATE INDEX idx_keyword ON keywords(keyword);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    roles VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
