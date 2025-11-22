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

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            long startTime = System.currentTimeMillis();
            request.setAttribute("startTime", startTime);

            String method = request.getMethod();
            String uri = request.getRequestURI();
            String queryString = request.getQueryString();
            String remoteAddr = getClientIP(request);
            String userAgent = request.getHeader("User-Agent");

            log.info("=== Incoming Request ===");
            log.info("Method: {}", method);
            log.info("URI: {}", uri);
            if (queryString != null) {
                log.info("Query String: {}", queryString);
            }
            log.info("Remote IP: {}", remoteAddr);
            log.info("User-Agent: {}", userAgent);

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

            log.info("=== Response ===");
            log.info("Method: {}", method);
            log.info("URI: {}", uri);
            log.info("Status Code: {}", status);
            log.info("Execution Time: {} ms", executeTime);

            if (ex != null) {
                log.error("Exception occurred: ", ex);
            }

            log.info("========================");
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
