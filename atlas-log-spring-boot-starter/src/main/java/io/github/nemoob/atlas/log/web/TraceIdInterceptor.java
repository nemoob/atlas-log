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
        // 从请求头获取TraceId
        String traceId = request.getHeader(headerName);
        
        if (!StringUtils.hasText(traceId)) {
            // 如果请求头中没有TraceId，生成一个新的
            traceId = TraceIdHolder.generateTraceId();
        }
        
        // 设置到当前线程
        TraceIdHolder.setTraceId(traceId);
        
        // 将TraceId设置到响应头中
        response.setHeader(headerName, traceId);
        
        log.debug("设置TraceId: {}", traceId);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        // 清理当前线程的TraceId
        TraceIdHolder.clear();
        log.debug("清理TraceId");
    }
}