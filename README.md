# FB Ads Analytics & AI Content Generator

> Spring Boot backend for Facebook Ads analytics and AI-powered content generation with automated scheduling

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## Features

- 📊 **Facebook Ads Management** - Collect and analyze ads from n8n workflow
- 🤖 **AI Content Generation** - Auto-generate posts using Google Gemini
- ⏰ **Smart Scheduling** - Schedule and auto-publish to Facebook
- 📈 **Trend Analysis** - Extract trending keywords and insights
- 🔐 **Secure API** - JWT authentication with role-based access
- 📚 **API Documentation** - Interactive Swagger UI

## Tech Stack

| Category | Technology |
|----------|-----------|
| Framework | Spring Boot 3.5.7 |
| Language | Java 17 |
| Database | MySQL 8.0+ |
| Migration | Flyway 11.15.0 |
| Security | Spring Security + JWT |
| AI | Google Gemini 1.5 Flash |
| Social API | Facebook Graph API v24.0 |
| Documentation | Springdoc OpenAPI 2.8.14 |

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.9+
- MySQL 8.0+
- Google Gemini API Key
- Facebook Access Token (optional, for publishing)

### Installation

1. **Clone the repository**
```bash
git clone <repository-url>
cd fbadsbe2
```

2. **Create database**
```sql
CREATE DATABASE n8n_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. **Configure application**

Create `.env` file or set environment variables:
```env
GEMINI_API_KEY=your_api_key
FACEBOOK_ACCESS_TOKEN=your_token
FACEBOOK_APP_ID=your_app_id
```

Update `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/n8n_db
spring.datasource.username=root
spring.datasource.password=your_password

gemini.api.key=${GEMINI_API_KEY}
facebook.api.access-token=${FACEBOOK_ACCESS_TOKEN}

jwt.secret=your_secret_key_min_256_bits
```

4. **Build and run**
```bash
mvn clean install
mvn spring-boot:run
```

5. **Access the application**
- API: http://localhost:1234
- Swagger UI: http://localhost:1234/swagger-ui.html

### First Steps

Register a user and get JWT token:
```bash
# Register
curl -X POST http://localhost:1234/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123","roles":"ROLE_ADMIN"}'

# Login
curl -X POST http://localhost:1234/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

## Architecture

### Core Components

```
┌─────────────────┐      ┌──────────────────┐      ┌─────────────────┐
│   n8n Workflow  │─────>│  Spring Boot API │─────>│   MySQL 8.0+    │
└─────────────────┘      └──────────────────┘      └─────────────────┘
                               │      │
                         ┌─────┘      └─────┐
                         ▼                   ▼
                  ┌─────────────┐    ┌──────────────┐
                  │ Gemini API  │    │ Facebook API │
                  └─────────────┘    └──────────────┘
```

### Database Schema

8 main tables managed by Flyway migrations:

| Table | Description |
|-------|-------------|
| `ads` | Facebook ads data from n8n |
| `insights` | Weekly analytics reports |
| `keywords` | Trending keywords extraction |
| `generated_content` | AI-generated posts |
| `scheduled_posts` | Posts scheduled for publishing |
| `publish_history` | Publishing action logs |
| `trend_analysis` | Daily trend analysis data |
| `users` | Authentication & authorization |

### Scheduled Jobs

- **PostPublishScheduler**: Every minute (`0 * * * * *`) - Check and publish scheduled posts
- **TrendAnalysisScheduler**: Daily at 6 AM (`0 0 6 * * *`) - Analyze trends and generate insights

## API Reference

**Base URL:** `http://localhost:1234`  
**Documentation:** `http://localhost:1234/swagger-ui.html`

### Authentication
```http
POST /api/auth/register    # Register user
POST /api/auth/login       # Get JWT token
```

### Ads Management
```http
POST   /api/ads                        # Create ad
GET    /api/ads                        # List ads (filters: typeAds, status, pageName)
GET    /api/ads/{id}                   # Get ad by ID
PUT    /api/ads/{id}                   # Update ad
DELETE /api/ads/{id}                   # Delete ad
GET    /api/ads/stats                  # Statistics
```

### Insights & Analytics
```http
POST   /api/insights/generate          # Generate insight (params: startDate, endDate)
GET    /api/insights                   # List insights
GET    /api/insights/latest            # Get latest
GET    /api/insights/{id}/keywords     # Get keywords
```

### Content Generation
```http
POST   /api/content/generate           # Generate with AI
GET    /api/content                    # List content (filters: status, contentType, platform)
GET    /api/content/{id}               # Get content
PUT    /api/content/{id}/approve       # Approve
PUT    /api/content/{id}/reject        # Reject
DELETE /api/content/{id}               # Delete
POST   /api/content/generate-batch     # Batch generate
```

### Scheduled Posts
```http
POST   /api/scheduled-posts            # Schedule post
GET    /api/scheduled-posts            # List posts (filters: status, platform)
GET    /api/scheduled-posts/{id}       # Get post
PUT    /api/scheduled-posts/{id}       # Update
DELETE /api/scheduled-posts/{id}       # Cancel
POST   /api/scheduled-posts/{id}/publish-now  # Publish immediately
GET    /api/scheduled-posts/upcoming   # Upcoming posts
```

### Trend Analysis
```http
GET    /api/trends/current             # Current trends
GET    /api/trends/keywords            # Trending keywords
GET    /api/trends/suggestions         # Content suggestions
POST   /api/trends/analyze             # Force analysis
```

### Example: Generate Content

**Request:**
```bash
POST /api/content/generate
Authorization: Bearer <token>

{
  "contentType": "POST",
  "platform": "FACEBOOK",
  "keywords": ["spa", "discount"],
  "tone": "FRIENDLY",
  "includeHashtags": true
}
```

**Response:**
```json
{
  "id": 1,
  "title": "Spa Weekend Special!",
  "content": "Relax this weekend...",
  "trendScore": 85.5,
  "status": "DRAFT"
}
```

## Configuration

### Environment Variables

```bash
# Required
GEMINI_API_KEY=your_gemini_key
MYSQL_PASSWORD=your_db_password

# Optional (for Facebook publishing)
FACEBOOK_ACCESS_TOKEN=your_fb_token
FACEBOOK_APP_ID=your_app_id
```

### Application Properties

```properties
# Server
server.port=1234

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/n8n_db
spring.datasource.username=root
spring.datasource.password=${MYSQL_PASSWORD}

# JWT
jwt.secret=your_secret_256_bits_minimum
jwt.expiration-ms=3600000

# Gemini
gemini.api.key=${GEMINI_API_KEY}
gemini.api.model=gemini-1.5-flash

# Facebook (optional)
facebook.api.access-token=${FACEBOOK_ACCESS_TOKEN}
facebook.api.app-id=${FACEBOOK_APP_ID}

# Scheduler
spring.task.scheduling.enabled=true
```

## Frontend Dashboard Requirements

### Overview
Build a modern, responsive dashboard for managing Facebook Ads analytics and AI content generation.

### Tech Stack Suggestions
- **Framework**: React 18+ / Next.js 14+ / Vue 3
- **UI Library**: Material-UI / Ant Design / Shadcn/ui
- **Charts**: Recharts / Chart.js / Apache ECharts
- **State Management**: Redux Toolkit / Zustand / TanStack Query
- **HTTP Client**: Axios / Fetch API
- **Auth**: JWT stored in httpOnly cookies or localStorage

### Core Pages & Features

#### 1. **Dashboard (Home)**
```
📊 Overview Cards:
- Total Ads (current week)
- Generated Content (pending/approved)
- Scheduled Posts (upcoming)
- Trend Score (average)

📈 Charts:
- Ads by Type (pie chart: IMAGE, VIDEO, CAROUSEL)
- Weekly Trends (line chart: ads over time)
- Top 10 Keywords (bar chart with percentages)
- Posting Schedule Calendar (heatmap)
```

#### 2. **Ads Management**
```
Features:
- Data table with filters (type, status, page, date range)
- Search by caption/page name
- Pagination (20 items per page)
- View details modal (full ad info + AI analysis)
- Export to CSV

Table Columns:
| Page Name | Type | Caption (truncated) | Date | Status | Actions |
```

#### 3. **Insights & Analytics**
```
Features:
- Weekly reports list
- Latest insight highlighted
- Generate new insight (date range picker)
- View detailed report with:
  * Ad statistics
  * Dominant format
  * Most active day
  * AI strategy recommendations
  * Keywords table with percentages
```

#### 4. **Content Generator** 🤖
```
Features:
- Form to generate content:
  * Content Type (dropdown: POST, ARTICLE, AD_COPY)
  * Platform (radio: Facebook, Instagram)
  * Keywords (tag input)
  * Tone (select: FRIENDLY, PROFESSIONAL, CASUAL)
  * Length (select: SHORT, MEDIUM, LONG)
  * Include hashtags (checkbox)
  * Include CTA (checkbox)

- Generated content list:
  * Filter by status (DRAFT, APPROVED, REJECTED)
  * Preview card with trend score badge
  * Actions: Approve, Reject, Edit, Delete, Schedule

- Content detail view:
  * Full content preview
  * Based on keywords & trends
  * AI model used
  * Created/updated timestamps
```

#### 5. **Scheduled Posts** ⏰
```
Features:
- Calendar view (month/week/day)
- List view with filters (status, platform)
- Create new scheduled post:
  * Select generated content (dropdown)
  * Facebook Page ID (input)
  * Scheduled date & time (datetime picker)
  * Media URLs (file upload or URL input)
  * Hashtags (tag input)
  * Call to action (select)

- Post cards showing:
  * Status badge (PENDING, PUBLISHED, FAILED)
  * Platform icon
  * Scheduled time (countdown if upcoming)
  * Content preview
  * Actions: Edit, Cancel, Publish Now

- Publishing history log
```

#### 6. **Trend Analysis** 📈
```
Features:
- Current trends overview:
  * Trending keywords (word cloud or table)
  * Trending topics (cards)
  * Optimal posting times (timeline visualization)
  * Confidence score (gauge chart)

- Content suggestions:
  * Cards with suggested topics
  * Associated keywords
  * Trend score
  * "Generate Content" quick action

- Historical trends:
  * Date selector
  * Trend comparison charts
```

#### 7. **Settings**
```
Features:
- Profile management (username, password)
- API keys configuration (Gemini, Facebook)
- Scheduler settings (enable/disable)
- Notification preferences
- Export/Import settings
```

### API Integration Examples

```javascript
// Authentication
const login = async (username, password) => {
  const response = await axios.post('http://localhost:1234/api/auth/login', {
    username, password
  });
  localStorage.setItem('token', response.data.token);
  return response.data;
};

// Get Ads with Filters
const getAds = async (filters = {}) => {
  const params = new URLSearchParams(filters);
  const response = await axios.get(
    `http://localhost:1234/api/ads?${params}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  return response.data;
};

// Generate Content
const generateContent = async (data) => {
  const response = await axios.post(
    'http://localhost:1234/api/content/generate',
    data,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  return response.data;
};

// Schedule Post
const schedulePost = async (postData) => {
  const response = await axios.post(
    'http://localhost:1234/api/scheduled-posts',
    postData,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  return response.data;
};
```

### UI/UX Guidelines

**Color Scheme:**
- Primary: Blue (#1976d2) - Trust, stability
- Success: Green (#4caf50) - Approved, published
- Warning: Orange (#ff9800) - Pending, scheduled
- Error: Red (#f44336) - Failed, rejected
- Info: Cyan (#00bcd4) - Insights, trends

**Typography:**
- Headers: Poppins / Inter (bold)
- Body: Roboto / Open Sans (regular)
- Code: Fira Code / JetBrains Mono

**Layouts:**
- Sidebar navigation (collapsible on mobile)
- Top bar with user profile, notifications
- Responsive grid system (12 columns)
- Card-based design for content blocks

**Interactions:**
- Loading states (skeletons, spinners)
- Error handling (toast notifications)
- Confirmation modals for destructive actions
- Real-time updates for scheduled posts
- Drag-and-drop for calendar scheduling

### Data Flow Example

```
User Action → Frontend → API Call → Backend Processing → Database
     ↓                                                        ↓
  UI Update ← Response ← JSON Data ← Service Layer ← Query Result
```

### Responsive Breakpoints
```css
/* Mobile */
@media (max-width: 640px) { /* Stack cards, hamburger menu */ }

/* Tablet */
@media (min-width: 641px) and (max-width: 1024px) { /* 2-column grid */ }

/* Desktop */
@media (min-width: 1025px) { /* Full sidebar, 3-column grid */ }
```

### Security Considerations
- Store JWT in httpOnly cookies (recommended) or secure localStorage
- Implement token refresh mechanism
- Add CSRF protection
- Sanitize user inputs
- Implement rate limiting on client side
- Use HTTPS in production

## Development

### Run Tests
```bash
mvn test
```

### Build JAR
```bash
mvn clean package
```

### Run with Profile
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### View Logs
```bash
tail -f logs/application.log
```

## Deployment

### Using Docker (optional)
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/fbadsbe2-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

### Production Checklist
- [ ] Set strong JWT secret (256+ bits)
- [ ] Configure HTTPS
- [ ] Set up database backups
- [ ] Configure log rotation
- [ ] Set appropriate HikariCP pool size
- [ ] Enable monitoring (health checks)
- [ ] Secure sensitive endpoints

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Author

**uvhnael** - [GitHub Profile](https://github.com/uvhnael)
