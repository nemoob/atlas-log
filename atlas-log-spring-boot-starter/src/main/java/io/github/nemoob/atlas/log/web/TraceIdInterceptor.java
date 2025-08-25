package io.github.nemoob.atlas.log.web;

import io.github.nemoob.atlas.log.context.TraceIdHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * TraceId拦截器
 * 从HTTP请求头中提取TraceId，如果不存在则生成一个新的
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@Slf4j
public class TraceIdInterceptor implements HandlerInterceptor {
    
    private final String headerName;
    
    public TraceIdInterceptor(String headerName) {
        this.headerName = headerName;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.debug("TraceIdInterceptor.preHandle called - URI: {}", request.getRequestURI());
        
        // 检查是否已有 TraceId（由 LoggingFilter 设置）
        String existingTraceId = TraceIdHolder.getTraceIdIfPresent();
        if (existingTraceId != null) {
            // 如果已有，直接使用，不重新生成
            response.setHeader(headerName, existingTraceId);
            log.debug("Reusing existing TraceId: {}", existingTraceId);
            return true;
        }
        
        // 如果没有，按原逻辑处理
        String traceId = request.getHeader(headerName);
        log.debug("Obtained TraceId from request header: {} (header: {})", traceId, headerName);
        
        if (!StringUtils.hasText(traceId)) {
            // 如果请求头中没有TraceId，生成一个新的
            traceId = TraceIdHolder.generateTraceId();
            log.debug("Generated new TraceId: {}", traceId);
        }
        
        // 设置到当前线程
        TraceIdHolder.setTraceId(traceId);
        
        // 将TraceId设置到响应头中
        response.setHeader(headerName, traceId);
        
        log.debug("TraceId setup completed: {}", traceId);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        // 临时注释掉清理，调试TraceId不一致问题
        String currentTraceId = TraceIdHolder.getTraceIdIfPresent();
        log.debug("TraceIdInterceptor completed - current TraceId: {}", currentTraceId);
        // TraceIdHolder.clear();
        // log.debug("TraceId cleared");
    }
}