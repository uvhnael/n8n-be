# 🚀 FB Ads Analytics & Content Generator Backend

## 📝 Project Overview

Backend API Spring Boot 3.x cho hệ thống:
- ✅ Quản lý & phân tích Facebook Ads từ n8n
- ✅ **Auto tạo bài viết dựa trên trend phân tích**
- ✅ **Hẹn giờ đăng bài tự động**
- ✅ Lưu trữ insights, keywords, CTAs
- ✅ Dashboard analytics

## 🏗️ Tech Stack

- **Spring Boot**: 3.2.x
- **Java**: 17+
- **Database**: MySQL 8.0+
- **Cache**: Redis 7.0+ (cho scheduled posts)
- **Queue**: Spring Scheduler + Database Queue
- **AI Integration**: Google Gemini API
- **Security**: Spring Security + JWT
- **API Docs**: Springdoc OpenAPI

## 📂 Project Structure

```
src/main/java/com/fbads/
├── FbAdsApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── RedisConfig.java
│   ├── SchedulerConfig.java
│   ├── GeminiConfig.java
│   └── SwaggerConfig.java
│
├── controller/
│   ├── AdsController.java              # CRUD Ads từ n8n
│   ├── InsightsController.java         # Insights & Analytics
│   ├── ContentController.java          # NEW: Auto generate content
│   ├── ScheduledPostController.java    # NEW: Schedule posts
│   └── AuthController.java
│
├── service/
│   ├── AdsService.java
│   ├── InsightsService.java
│   ├── ContentGeneratorService.java    # NEW: AI content generation
│   ├── TrendAnalysisService.java       # NEW: Trend analysis
│   ├── ScheduledPostService.java       # NEW: Post scheduling
│   ├── PostPublisherService.java       # NEW: Auto publish
│   └── GeminiService.java              # NEW: Gemini API wrapper
│
├── repository/
│   ├── AdsRepository.java
│   ├── InsightsRepository.java
│   ├── KeywordsRepository.java
│   ├── GeneratedContentRepository.java  # NEW
│   ├── ScheduledPostRepository.java     # NEW
│   └── PublishHistoryRepository.java    # NEW
│
├── model/
│   ├── entity/
│   │   ├── Ad.java
│   │   ├── Insight.java
│   │   ├── Keyword.java
│   │   ├── GeneratedContent.java        # NEW
│   │   ├── ScheduledPost.java           # NEW
│   │   ├── PublishHistory.java          # NEW
│   │   └── User.java
│   ├── dto/
│   │   ├── AdDTO.java
│   │   ├── InsightDTO.java
│   │   ├── ContentGenerateRequest.java  # NEW
│   │   ├── ScheduledPostDTO.java        # NEW
│   │   └── TrendAnalysisDTO.java        # NEW
│   └── enums/
│       ├── PostStatus.java              # NEW
│       ├── ContentType.java             # NEW
│       └── PlatformType.java            # NEW
│
├── scheduler/
│   ├── PostPublishScheduler.java        # NEW: Auto publish scheduler
│   └── TrendAnalysisScheduler.java      # NEW: Daily trend check
│
└── exception/
    ├── GlobalExceptionHandler.java
    └── CustomExceptions.java
```

## 🗄️ Database Schema

### **Existing Tables** (từ n8n workflow)

```sql
-- ads: Lưu ads từ n8n
CREATE TABLE ads (
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
    scraped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type_ads (type_ads),
    INDEX idx_time_created (time_created)
);

-- insights: Báo cáo phân tích tuần
CREATE TABLE insights (
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_report_date (report_date)
);

-- keywords: Keywords từ insight
CREATE TABLE keywords (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    insight_id BIGINT NOT NULL,
    keyword VARCHAR(100) NOT NULL,
    count INT DEFAULT 0,
    percentage DECIMAL(5,2),
    week VARCHAR(20),
    FOREIGN KEY (insight_id) REFERENCES insights(id),
    INDEX idx_keyword (keyword)
);
```

### **NEW Tables** (cho tính năng mới)

```sql
-- generated_content: Nội dung AI tự động tạo
CREATE TABLE generated_content (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    content_type VARCHAR(50) NOT NULL,        -- POST, ARTICLE, AD_COPY
    platform VARCHAR(50),                      -- FACEBOOK, INSTAGRAM, BLOG
    
    -- Trend analysis data
    based_on_keywords JSON,                    -- Keywords sử dụng
    based_on_trends JSON,                      -- Trends phân tích
    trend_score DECIMAL(5,2),                  -- Điểm trend (0-100)
    
    -- AI metadata
    ai_model VARCHAR(50),                      -- gemini-2.0-flash
    generation_prompt TEXT,
    
    -- Status & approval
    status VARCHAR(20) DEFAULT 'DRAFT',        -- DRAFT, APPROVED, REJECTED
    approved_by BIGINT,
    approved_at TIMESTAMP NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_status (status),
    INDEX idx_content_type (content_type),
    INDEX idx_created_at (created_at)
);

-- scheduled_posts: Bài viết hẹn giờ
CREATE TABLE scheduled_posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content_id BIGINT NOT NULL,                -- Link to generated_content
    
    -- Publishing info
    platform VARCHAR(50) NOT NULL,             -- FACEBOOK, INSTAGRAM
    platform_page_id VARCHAR(100),             -- FB Page ID
    scheduled_time TIMESTAMP NOT NULL,         -- Thời gian đăng
    
    -- Post configuration
    post_type VARCHAR(50),                     -- TEXT, IMAGE, VIDEO, CAROUSEL
    media_urls JSON,                           -- URLs của media
    hashtags JSON,                             -- Hashtags
    call_to_action VARCHAR(100),               -- CTA
    
    -- Publishing status
    status VARCHAR(20) DEFAULT 'PENDING',      -- PENDING, PUBLISHED, FAILED, CANCELLED
    published_at TIMESTAMP NULL,
    publish_error TEXT NULL,
    retry_count INT DEFAULT 0,
    
    -- Engagement tracking
    post_id VARCHAR(100),                      -- ID post sau khi đăng
    likes_count INT DEFAULT 0,
    comments_count INT DEFAULT 0,
    shares_count INT DEFAULT 0,
    reach INT DEFAULT 0,
    
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (content_id) REFERENCES generated_content(id),
    INDEX idx_scheduled_time (scheduled_time),
    INDEX idx_status (status),
    INDEX idx_platform (platform)
);

-- publish_history: Lịch sử đăng bài
CREATE TABLE publish_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scheduled_post_id BIGINT NOT NULL,
    
    action VARCHAR(50) NOT NULL,               -- CREATED, PUBLISHED, FAILED, UPDATED
    status VARCHAR(20),
    message TEXT,
    metadata JSON,                             -- Additional data
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (scheduled_post_id) REFERENCES scheduled_posts(id),
    INDEX idx_scheduled_post_id (scheduled_post_id),
    INDEX idx_action (action)
);

-- trend_analysis: Phân tích trend tự động
CREATE TABLE trend_analysis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    analysis_date DATE NOT NULL,
    
    -- Trend data
    trending_keywords JSON,                    -- Top trending keywords
    trending_topics JSON,                      -- Trending topics
    competitor_activity JSON,                  -- Competitor insights
    
    -- Recommendations
    content_suggestions JSON,                  -- Gợi ý nội dung
    optimal_posting_times JSON,                -- Thời gian đăng tốt nhất
    
    -- AI analysis
    ai_summary TEXT,
    confidence_score DECIMAL(5,2),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_analysis_date (analysis_date)
);
```

## 🔌 API Endpoints

### **1. Ads Management** (từ n8n)

```java
// Nhận ads từ n8n workflow
POST   /api/ads                           # Create ad from n8n
GET    /api/ads                           # List with filters
GET    /api/ads/{id}                      # Get by ID
PUT    /api/ads/{id}                      # Update
DELETE /api/ads/{id}                      # Delete
GET    /api/ads/stats                     # Statistics
```

### **2. Insights & Analytics**

```java
POST   /api/insights/generate             # Generate từ ads data
GET    /api/insights/latest               # Latest insight
GET    /api/insights                      # List insights
GET    /api/insights/{id}                 # Get by ID
GET    /api/insights/{id}/keywords        # Keywords của insight
```

### **3. NEW: Content Generator**

```java
// Auto generate content dựa trên trend
POST   /api/content/generate              # Generate new content
GET    /api/content                       # List generated content
GET    /api/content/{id}                  # Get by ID
PUT    /api/content/{id}/approve          # Approve content
PUT    /api/content/{id}/reject           # Reject content
DELETE /api/content/{id}                  # Delete

POST   /api/content/generate-batch        # Generate nhiều content
GET    /api/content/suggestions           # Content suggestions từ trends
```

**Request: Generate Content**
```json
POST /api/content/generate
{
  "contentType": "POST",           // POST, ARTICLE, AD_COPY
  "platform": "FACEBOOK",          // FACEBOOK, INSTAGRAM, BLOG
  "basedOnInsightId": 123,         // Optional: insight ID
  "keywords": ["giảm giá", "spa"], // Optional: custom keywords
  "tone": "FRIENDLY",              // PROFESSIONAL, FRIENDLY, CASUAL
  "length": "MEDIUM",              // SHORT, MEDIUM, LONG
  "includeHashtags": true,
  "includeCTA": true
}
```

**Response:**
```json
{
  "id": 1,
  "title": "🌟 Ưu đãi spa cuối tuần...",
  "content": "Full generated content here...",
  "contentType": "POST",
  "platform": "FACEBOOK",
  "basedOnKeywords": ["giảm giá", "spa", "làm đẹp"],
  "trendScore": 85.5,
  "status": "DRAFT",
  "createdAt": "2024-01-15T10:00:00"
}
```

### **4. NEW: Scheduled Posts**

```java
// Schedule posts
POST   /api/scheduled-posts               # Schedule new post
GET    /api/scheduled-posts                # List scheduled
GET    /api/scheduled-posts/{id}          # Get by ID
PUT    /api/scheduled-posts/{id}          # Update schedule
DELETE /api/scheduled-posts/{id}          # Cancel schedule

// Bulk operations
POST   /api/scheduled-posts/bulk          # Schedule multiple
GET    /api/scheduled-posts/calendar      # Calendar view
GET    /api/scheduled-posts/upcoming      # Upcoming posts

// Manual actions
POST   /api/scheduled-posts/{id}/publish-now  # Publish immediately
POST   /api/scheduled-posts/{id}/reschedule   # Reschedule
```

**Request: Schedule Post**
```json
POST /api/scheduled-posts
{
  "contentId": 123,                     // ID của generated content
  "platform": "FACEBOOK",
  "platformPageId": "102250089256484",  // FB Page ID
  "scheduledTime": "2024-01-20T15:00:00",
  
  "postType": "IMAGE",
  "mediaUrls": ["https://..."],
  "hashtags": ["#spa", "#beauty"],
  "callToAction": "BOOK_NOW",
  
  "autoPublish": true                    // Tự động đăng khi đến giờ
}
```

**Response:**
```json
{
  "id": 1,
  "contentId": 123,
  "platform": "FACEBOOK",
  "scheduledTime": "2024-01-20T15:00:00",
  "status": "PENDING",
  "content": {
    "title": "...",
    "content": "..."
  },
  "createdAt": "2024-01-15T10:00:00"
}
```

### **5. NEW: Trend Analysis**

```java
GET    /api/trends/current                # Current trends
GET    /api/trends/keywords               # Trending keywords
GET    /api/trends/suggestions            # Content suggestions
POST   /api/trends/analyze                # Force analyze now
```

**Response: Current Trends**
```json
{
  "analysisDate": "2024-01-15",
  "trendingKeywords": [
    {"keyword": "giảm giá", "count": 45, "growth": "+25%"},
    {"keyword": "spa", "count": 38, "growth": "+15%"}
  ],
  "trendingTopics": [
    "Ưu đãi cuối tuần",
    "Chăm sóc da mùa đông"
  ],
  "contentSuggestions": [
    {
      "topic": "Ưu đãi spa cuối tuần",
      "keywords": ["giảm giá", "spa", "cuối tuần"],
      "trendScore": 92,
      "suggestedPostingTime": "2024-01-19T14:00:00"
    }
  ],
  "optimalPostingTimes": [
    {"day": "Thứ 6", "time": "14:00-16:00", "score": 95},
    {"day": "Chủ Nhật", "time": "10:00-12:00", "score": 88}
  ]
}
```

## 🎯 Service Implementation Examples

### **ContentGeneratorService.java**

```java
@Service
@RequiredArgsConstructor
public class ContentGeneratorService {
    private final GeminiService geminiService;
    private final TrendAnalysisService trendService;
    private final GeneratedContentRepository contentRepo;
    
    /**
     * Generate content dựa trên trends và keywords
     */
    public GeneratedContent generateContent(ContentGenerateRequest request) {
        // 1. Lấy trend data
        TrendAnalysis trends = trendService.getCurrentTrends();
        
        // 2. Build prompt cho AI
        String prompt = buildPrompt(request, trends);
        
        // 3. Call Gemini API
        String generatedText = geminiService.generateText(prompt);
        
        // 4. Parse và format content
        GeneratedContent content = parseAndFormat(generatedText, request);
        
        // 5. Calculate trend score
        content.setTrendScore(calculateTrendScore(content, trends));
        
        // 6. Save to database
        return contentRepo.save(content);
    }
    
    private String buildPrompt(ContentGenerateRequest request, TrendAnalysis trends) {
        return String.format("""
            Bạn là chuyên gia content marketing.
            
            Nhiệm vụ: Tạo %s cho platform %s
            Tone: %s
            Độ dài: %s
            
            Trending keywords hiện tại: %s
            Trending topics: %s
            
            Yêu cầu:
            - Sử dụng trending keywords một cách tự nhiên
            - Tạo content hấp dẫn, dễ viral
            - %s
            - %s
            
            Format: JSON với fields {title, content, hashtags, cta}
            """,
            request.getContentType(),
            request.getPlatform(),
            request.getTone(),
            request.getLength(),
            trends.getTrendingKeywords(),
            trends.getTrendingTopics(),
            request.isIncludeHashtags() ? "Thêm 5-7 hashtags phù hợp" : "",
            request.isIncludeCTA() ? "Thêm CTA cuối bài" : ""
        );
    }
}
```

### **ScheduledPostService.java**

```java
@Service
@RequiredArgsConstructor
public class ScheduledPostService {
    private final ScheduledPostRepository postRepo;
    private final GeneratedContentRepository contentRepo;
    private final PublishHistoryRepository historyRepo;
    
    /**
     * Schedule một post
     */
    @Transactional
    public ScheduledPost schedulePost(ScheduledPostDTO dto) {
        // Validate content exists
        GeneratedContent content = contentRepo.findById(dto.getContentId())
            .orElseThrow(() -> new NotFoundException("Content not found"));
        
        // Check content is approved
        if (!content.getStatus().equals(ContentStatus.APPROVED)) {
            throw new ValidationException("Content must be approved first");
        }
        
        // Create scheduled post
        ScheduledPost post = ScheduledPost.builder()
            .content(content)
            .platform(dto.getPlatform())
            .scheduledTime(dto.getScheduledTime())
            .status(PostStatus.PENDING)
            .build();
        
        post = postRepo.save(post);
        
        // Log history
        logHistory(post, "CREATED", "Post scheduled successfully");
        
        return post;
    }
    
    /**
     * Lấy posts cần publish trong 5 phút tới
     */
    public List<ScheduledPost> getPostsToPublish() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next5Min = now.plusMinutes(5);
        
        return postRepo.findByScheduledTimeBetweenAndStatus(
            now, next5Min, PostStatus.PENDING
        );
    }
}
```

### **PostPublishScheduler.java**

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PostPublishScheduler {
    private final ScheduledPostService scheduledPostService;
    private final PostPublisherService publisherService;
    
    /**
     * Chạy mỗi phút để check posts cần publish
     */
    @Scheduled(cron = "0 * * * * *")  // Every minute
    public void checkAndPublishPosts() {
        log.info("Checking for posts to publish...");
        
        List<ScheduledPost> posts = scheduledPostService.getPostsToPublish();
        
        if (posts.isEmpty()) {
            log.info("No posts to publish");
            return;
        }
        
        log.info("Found {} posts to publish", posts.size());
        
        for (ScheduledPost post : posts) {
            try {
                publisherService.publish(post);
                log.info("Published post {}", post.getId());
            } catch (Exception e) {
                log.error("Failed to publish post {}: {}", 
                    post.getId(), e.getMessage());
                handlePublishError(post, e);
            }
        }
    }
    
    /**
     * Chạy hàng ngày lúc 6h sáng để analyze trends
     */
    @Scheduled(cron = "0 0 6 * * *")  // 6:00 AM daily
    public void analyzeDailyTrends() {
        log.info("Starting daily trend analysis...");
        // Implementation
    }
}
```

### **PostPublisherService.java**

```java
@Service
@RequiredArgsConstructor
public class PostPublisherService {
    private final FacebookApiClient facebookClient;  // Custom client
    private final ScheduledPostRepository postRepo;
    
    /**
     * Publish post lên platform
     */
    @Transactional
    public void publish(ScheduledPost post) {
        try {
            // Update status to PUBLISHING
            post.setStatus(PostStatus.PUBLISHING);
            postRepo.save(post);
            
            // Publish based on platform
            String postId = switch (post.getPlatform()) {
                case FACEBOOK -> publishToFacebook(post);
                case INSTAGRAM -> publishToInstagram(post);
                default -> throw new UnsupportedOperationException(
                    "Platform not supported: " + post.getPlatform()
                );
            };
            
            // Update success
            post.setStatus(PostStatus.PUBLISHED);
            post.setPostId(postId);
            post.setPublishedAt(LocalDateTime.now());
            postRepo.save(post);
            
            log.info("Successfully published post {} to {}", 
                post.getId(), post.getPlatform());
                
        } catch (Exception e) {
            // Update failure
            post.setStatus(PostStatus.FAILED);
            post.setPublishError(e.getMessage());
            post.setRetryCount(post.getRetryCount() + 1);
            postRepo.save(post);
            
            // Retry logic if needed
            if (post.getRetryCount() < 3) {
                scheduleRetry(post);
            }
            
            throw e;
        }
    }
    
    private String publishToFacebook(ScheduledPost post) {
        // Implement Facebook Graph API call
        FacebookPostRequest request = FacebookPostRequest.builder()
            .pageId(post.getPlatformPageId())
            .message(post.getContent().getContent())
            .link(post.getMediaUrls())
            .build();
            
        return facebookClient.createPost(request);
    }
}
```

## 🔄 N8N Integration Flow

### **Workflow Update** (thêm vào workflow hiện tại)

```json
{
  "nodes": [
    {
      "name": "Save to Backend API",
      "type": "n8n-nodes-base.httpRequest",
      "parameters": {
        "method": "POST",
        "url": "http://localhost:8080/api/ads",
        "authentication": "headerAuth",
        "sendBody": true,
        "contentType": "json",
        "body": "={{ JSON.stringify({\n  adArchiveId: $json.adArchiveId,\n  pageName: $json.pageName,\n  caption: $json.caption,\n  typeAds: $json.typeAds,\n  urlAdsPost: $json.urlAdsPost,\n  aiAnalyze: $json.aiAnalyze,\n  imgUrl: $json.imgUrl,\n  status: 'ACTIVE',\n  timeCreated: $now.format('yyyy-MM-dd')\n}) }}"
      }
    },
    {
      "name": "Trigger Content Generation",
      "type": "n8n-nodes-base.httpRequest",
      "parameters": {
        "method": "POST",
        "url": "http://localhost:8080/api/content/generate-batch",
        "body": "={{ JSON.stringify({\n  count: 3,\n  contentType: 'POST',\n  platform: 'FACEBOOK',\n  basedOnLatestInsight: true\n}) }}"
      }
    }
  ]
}
```

## 🚀 Getting Started

### **1. Prerequisites**
```bash
- Java 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+
- Google Gemini API Key
```

### **2. Setup Database**
```sql
CREATE DATABASE fbads_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Run migration scripts in order:
-- V1__create_base_tables.sql
-- V2__create_content_tables.sql
-- V3__create_indexes.sql
```

### **3. Configuration**

`application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fbads_db
    username: root
    password: your_password
    
  redis:
    host: localhost
    port: 6379
    
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true

# Gemini API
gemini:
  api:
    key: ${GEMINI_API_KEY}
    base-url: https://generativelanguage.googleapis.com
    
# Scheduler
scheduler:
  post-publisher:
    enabled: true
    cron: "0 * * * * *"  # Every minute
  trend-analysis:
    enabled: true
    cron: "0 0 6 * * *"  # 6 AM daily
    
# Facebook API (optional)
facebook:
  api:
    access-token: ${FB_ACCESS_TOKEN}
    app-id: ${FB_APP_ID}
```

### **4. Run Application**
```bash
mvn clean install
mvn spring-boot:run
```

## 📊 Usage Examples

### **1. n8n gửi ads vào backend:**
```bash
curl -X POST http://localhost:8080/api/ads \
  -H "Content-Type: application/json" \
  -d '{
    "adArchiveId": "123456",
    "pageName": "Beauty Spa",
    "caption": "Get 50% off...",
    "typeAds": "IMAGE",
    "aiAnalyze": "Promotional ad for spa services",
    "status": "ACTIVE"
  }'
```

### **2. Auto generate content từ trends:**
```bash
curl -X POST http://localhost:8080/api/content/generate \
  -H "Content-Type: application/json" \
  -d '{
    "contentType": "POST",
    "platform": "FACEBOOK",
    "tone": "FRIENDLY",
    "includeHashtags": true
  }'
```

### **3. Schedule post:**
```bash
curl -X POST http://localhost:8080/api/scheduled-posts \
  -H "Content-Type: application/json" \
  -d '{
    "contentId": 123,
    "platform": "FACEBOOK",
    "scheduledTime": "2024-01-20T15:00:00",
    "postType": "IMAGE",
    "autoPublish": true
  }'
```

## 🎯 Key Features

### **✅ Auto Content Generation**
- Phân tích trends từ insights
- Generate content phù hợp với trending keywords
- Support multiple platforms (FB, IG, Blog)
- Multiple content types (Post, Article, Ad Copy)
- AI-powered với Gemini 2.0

### **✅ Smart Scheduling**
- Hẹn giờ đăng bài chính xác
- Auto publish khi đến giờ
- Retry logic khi fail
- Calendar view
- Bulk scheduling

### **✅ Trend Analysis**
- Daily auto analysis
- Trending keywords detection
- Optimal posting times
- Content suggestions
- Competitor insights

### **✅ Full Integration với n8n**
- Seamless data flow
- Real-time analytics
- Auto trigger content generation
- Webhook support

## 📈 Monitoring & Logs

```java
// Check scheduled posts
GET /api/scheduled-posts/upcoming

// Check system health
GET /actuator/health

// Scheduler status
GET /actuator/scheduledtasks

// Logs location
tail -f logs/application.log
```

## 🔒 Security

- JWT Authentication
- Role-based access (ADMIN, EDITOR, VIEWER)
- API rate limiting
- Secure credential storage
- HTTPS only in production

---

**🎉 Ready to use with GitHub Copilot!**

Copy toàn bộ README này vào project, Copilot sẽ generate toàn bộ code dựa trên specifications trên!