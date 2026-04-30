package com.hng.stage3.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class VersioningInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        
        // Only apply to /api/** endpoints
        if (path.startsWith("/api/")) {
            String version = request.getHeader("X-API-Version");
            
            if (version == null || !version.equals("1")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                
                Map<String, String> error = Map.of(
                        "status", "error",
                        "message", "Missing or invalid X-API-Version header. Expected '1'."
                );
                
                response.getWriter().write(objectMapper.writeValueAsString(error));
                return false;
            }
        }
        
        return true;
    }
}
