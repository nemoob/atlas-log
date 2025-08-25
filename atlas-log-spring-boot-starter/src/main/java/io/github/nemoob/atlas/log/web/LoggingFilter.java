package io.github.nemoob.atlas.log.web;

import io.github.nemoob.atlas.log.config.LogConfigProperties;
import io.github.nemoob.atlas.log.context.TraceIdHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 日志过滤器
 * 记录HTTP请求的基本信息和执行时间
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@Slf4j
public class LoggingFilter implements Filter {
    
    private final LogConfigProperties properties;
    
    public LoggingFilter(LogConfigProperties properties) {
        this.properties = properties;
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Atlas Log filter initialized successfully");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // LoggingFilter 作为请求的入口，直接管理 TraceId
        String traceId = TraceIdHolder.getTraceIdIfPresent();
        if (traceId == null) {
            // 从请求头获取 TraceId
            traceId = httpRequest.getHeader("X-Trace-Id");
            if (traceId == null || traceId.trim().isEmpty()) {
                // 如果请求头中没有，生成新的 TraceId
                traceId = TraceIdHolder.generateTraceId();
                log.debug("LoggingFilter generated new TraceId: {}", traceId);
            } else {
                log.debug("LoggingFilter obtained TraceId from request header: {}", traceId);
            }
            // 设置到当前线程
            TraceIdHolder.setTraceId(traceId);
            // 设置到响应头
            httpResponse.setHeader("X-Trace-Id", traceId);
        }
        log.debug("LoggingFilter started - TraceId: {}", traceId);
        long startTime = System.currentTimeMillis();
        
        try {
            // 记录请求开始日志
            logRequestStart(httpRequest, traceId);
            
            // 执行请求
            chain.doFilter(request, response);
            
        } finally {
            // 使用保存的 traceId，确保开始和结束日志的 TraceId 一致
            String currentTraceId = TraceIdHolder.getTraceIdIfPresent();
            log.debug("LoggingFilter finished - saved TraceId: {}, current TraceId: {}", traceId, currentTraceId);
            long executionTime = System.currentTimeMillis() - startTime;
            logRequestEnd(httpRequest, httpResponse, executionTime, traceId);
        }
    }
    
    /**
     * 记录请求开始日志
     */
    private void logRequestStart(HttpServletRequest request, String traceId) {
        if (log.isDebugEnabled()) {
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String queryString = request.getQueryString();
            String remoteAddr = getClientIpAddress(request);
            
            StringBuilder logMsg = new StringBuilder();
            logMsg.append("TraceId: ").append(traceId).append(" | ");
            logMsg.append("Method: ").append(method).append(" | ");
            logMsg.append("URI: ").append(uri);
            logMsg.append(" | HTTP请求开始");
            
            if (StringUtils.hasText(queryString)) {
                logMsg.append("?").append(queryString);
            }
            
            logMsg.append(" | ");
            logMsg.append("RemoteAddr: ").append(remoteAddr);
            
            log.debug(logMsg.toString());
        }
    }
    
    /**
     * 记录请求结束日志
     */
    private void logRequestEnd(HttpServletRequest request, HttpServletResponse response, 
                              long executionTime, String traceId) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();
        
        StringBuilder logMsg = new StringBuilder();
        logMsg.append("TraceId: ").append(traceId).append(" | ");
        logMsg.append("Method: ").append(method).append(" | ");
        logMsg.append("URI: ").append(uri).append(" | ");
        logMsg.append("HTTP请求完成 | ");
        logMsg.append("Status: ").append(status).append(" | ");
        logMsg.append("ExecutionTime: ").append(executionTime).append("ms");
        
        // 根据执行时间和状态码选择日志级别
        if (status >= 500) {
            log.error(logMsg.toString());
        } else if (status >= 400) {
            log.warn(logMsg.toString());
        } else if (executionTime > properties.getPerformance().getSlowThreshold()) {
            log.warn(logMsg.toString() + " | SlowRequest: true");
        } else if (log.isInfoEnabled()) {
            log.info(logMsg.toString());
        }
    }
    
    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    @Override
    public void destroy() {
        log.info("Atlas Log filter destroyed successfully");
    }
}