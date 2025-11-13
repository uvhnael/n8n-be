# 📋 Scheduled Posts - Quick Reference Card

## 🚀 Start Application

```bash
.\mvnw.cmd spring-boot:run
```

Access: http://localhost:1234

## 📖 API Quick Reference

### 🎨 Content Generation

```bash
# Generate content
POST /api/content/generate
{
  "contentType": "POST",
  "platform": "FACEBOOK",
  "tone": "FRIENDLY",
  "keywords": ["spa", "beauty"]
}

# List content
GET /api/content?status=DRAFT

# Approve
PUT /api/content/{id}/approve

# Batch generate
POST /api/content/generate-batch
{"count": 3, "contentType": "POST"}
```

### 📅 Post Scheduling

```bash
# Schedule post
POST /api/scheduled-posts
{
  "contentId": 1,
  "platform": "FACEBOOK",
  "scheduledTime": "2024-12-31T15:00:00"
}

# List scheduled
GET /api/scheduled-posts

# Upcoming posts
GET /api/scheduled-posts/upcoming

# Publish now
POST /api/scheduled-posts/{id}/publish-now

# Cancel
DELETE /api/scheduled-posts/{id}
```

## 🔧 Configuration

### Required (application.properties)

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/n8n_be
spring.datasource.username=root
spring.datasource.password=1
```

### Optional

```properties
# Gemini API (for real AI)
gemini.api.key=YOUR_KEY

# Facebook API (for real publishing)
facebook.api.access-token=YOUR_TOKEN
```

## 📊 Workflow

```
Generate Content → Approve → Schedule → Auto-Publish
```

## 🕐 Scheduler

- Runs: Every minute (`0 * * * * *`)
- Checks: Posts in next 5 minutes
- Action: Auto-publishes pending posts

## 📚 Documentation

- Swagger UI: http://localhost:1234/swagger-ui.html
- Full Guide: `SCHEDULED_POSTS_GUIDE.md`
- Summary: `IMPLEMENTATION_SUMMARY.md`

## ✅ Status Values

### Content Status

- `DRAFT` - Created but not approved
- `APPROVED` - Ready for scheduling
- `REJECTED` - Not approved

### Post Status

- `PENDING` - Scheduled, waiting
- `PUBLISHING` - Currently publishing
- `PUBLISHED` - Successfully published
- `FAILED` - Publishing failed
- `CANCELLED` - User cancelled

## 🎯 Common Tasks

### Generate & Schedule Flow

```bash
# 1. Generate
curl -X POST http://localhost:1234/api/content/generate \
  -H "Content-Type: application/json" \
  -d '{"contentType":"POST","platform":"FACEBOOK"}'

# 2. Approve (use ID from step 1)
curl -X PUT http://localhost:1234/api/content/1/approve

# 3. Schedule (use ID from step 1)
curl -X POST http://localhost:1234/api/scheduled-posts \
  -H "Content-Type: application/json" \
  -d '{"contentId":1,"platform":"FACEBOOK","scheduledTime":"2024-12-31T15:00:00"}'

# 4. Wait for auto-publish or use publish-now
curl -X POST http://localhost:1234/api/scheduled-posts/1/publish-now
```

## 🐛 Troubleshooting

### Posts not publishing?

1. Check scheduler enabled: `spring.task.scheduling.enabled=true`
2. Check content approved: Status must be `APPROVED`
3. Check time: Must be in future
4. Check logs for errors

### API not working?

1. Without Gemini key: Mock responses used
2. Without FB token: Simulated publishing
3. Both work for testing!

## 📞 Help

- Logs: Check console output
- Swagger: http://localhost:1234/swagger-ui.html
- Database: Check tables directly
- Guide: Read `SCHEDULED_POSTS_GUIDE.md`

---

**🎉 Everything is ready! Start the app and try it out!**
