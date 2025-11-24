package org.uvhnael.fbadsbe2.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class LoggingConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestLoggingInterceptor());
    }

    @Slf4j
    public static class RequestLoggingInterceptor implements HandlerInterceptor {

        // ANSI Color Codes
        private static final String RESET = "\u001B[0m";
        private static final String BLUE = "\u001B[34m";
        private static final String GREEN = "\u001B[32m";
        private static final String YELLOW = "\u001B[33m";
        private static final String RED = "\u001B[31m";
        private static final String CYAN = "\u001B[36m";
        private static final String BOLD = "\u001B[1m";

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            long startTime = System.currentTimeMillis();
            request.setAttribute("startTime", startTime);

            String method = request.getMethod();
            String uri = request.getRequestURI();
            String queryString = request.getQueryString();
            String remoteAddr = getClientIP(request);

            String logMessage = String.format("%s%s🔵 REQUEST%s | %s%s%s %s%s | IP: %s", 
                BOLD, BLUE, RESET,
                CYAN, method, RESET,
                uri,
                queryString != null ? "?" + queryString : "",
                remoteAddr
            );
            log.info(logMessage);

            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            long startTime = (Long) request.getAttribute("startTime");
            long endTime = System.currentTimeMillis();
            long executeTime = endTime - startTime;

            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();

            String statusIcon;
            String statusColor;
            if (status >= 200 && status < 300) {
                statusIcon = "✅";
                statusColor = GREEN;
            } else if (status >= 400 && status < 500) {
                statusIcon = "⚠️";
                statusColor = YELLOW;
            } else if (status >= 500) {
                statusIcon = "❌";
                statusColor = RED;
            } else {
                statusIcon = "ℹ️";
                statusColor = CYAN;
            }
            
            String logMessage = String.format("%s%s%s RESPONSE%s | %s%s%s %s | Status: %s%d%s | Time: %s%dms%s", 
                BOLD, statusColor, statusIcon, RESET,
                CYAN, method, RESET,
                uri,
                statusColor, status, RESET,
                YELLOW, executeTime, RESET
            );
            
            if (ex != null) {
                log.error(logMessage + " | " + RED + "Exception: " + ex.getMessage() + RESET, ex);
            } else {
                log.info(logMessage);
            }
        }

        private String getClientIP(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIP = request.getHeader("X-Real-IP");
            if (xRealIP != null && !xRealIP.isEmpty()) {
                return xRealIP;
            }

            return request.getRemoteAddr();
        }
    }
}
