package io.github.nemoob.atlas.log.web;

import io.github.nemoob.atlas.log.config.LogConfigProperties;
import io.github.nemoob.atlas.log.context.TraceIdHolder;
import io.github.nemoob.atlas.log.serializer.ArgumentFormatConfig;
import io.github.nemoob.atlas.log.serializer.ArgumentFormatType;
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
import java.util.Enumeration;
import java.util.Map;

/**
 * 日志过滤器
 * 记录HTTP请求的基本信息和执行时间
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Slf4j
public class LoggingFilter implements Filter {
    
    private final LogConfigProperties properties;
    private final ArgumentFormatConfig argumentFormatConfig;
    
    public LoggingFilter(LogConfigProperties properties, ArgumentFormatConfig argumentFormatConfig) {
        this.properties = properties;
        this.argumentFormatConfig = argumentFormatConfig != null ? argumentFormatConfig : new ArgumentFormatConfig();
        
        // ✅ 添加调试日志
        log.debug("=== LoggingFilter Constructor Debug ===");
        log.debug("Received LogConfigProperties: {}", properties);
        if (properties != null && properties.getHttpLog() != null) {
            log.debug("HTTP Log urlFormat: '{}'", properties.getHttpLog().getUrlFormat());
            log.debug("HTTP Log includeQueryString: {}", properties.getHttpLog().isIncludeQueryString());
        } else {
            log.debug("No HTTP log configuration found in properties");
        }
        log.debug("===========================================");
    }
    
    // 兼容旧的构造函数
    public LoggingFilter(LogConfigProperties properties) {
        this.properties = properties;
        this.argumentFormatConfig = new ArgumentFormatConfig();
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Atlas Log filter initialized successfully");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        // 添加递归检测，防止 StackOverflowError
        String recursionKey = "ATLAS_LOG_FILTER_PROCESSED";
        if (request.getAttribute(recursionKey) != null) {
            // 已经处理过，直接传递，避免无限循环
            chain.doFilter(request, response);
            return;
        }
        
        // 标记已处理
        request.setAttribute(recursionKey, true);
        
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
            
            // 使用自定义URL格式
            String formattedUrl = formatUrl(method, uri, queryString, remoteAddr);
            logMsg.append("URL: ").append(formattedUrl);
            
            logMsg.append(" | HTTP请求开始");
            
            // 默认记录请求参数（可通过配置控制格式）
            if (properties.getHttpLog().isLogFullParameters()) {
                // 记录查询参数
                if (StringUtils.hasText(queryString) && properties.getHttpLog().isIncludeQueryString()) {
                    String formattedParams = formatRequestParameters(request);
                    if (StringUtils.hasText(formattedParams)) {
                        logMsg.append(" | Parameters: ").append(formattedParams);
                    }
                }
                
                // 记录请求头（如果启用）
                if (properties.getHttpLog().isIncludeHeaders()) {
                    String headers = getFilteredHeaders(request);
                    if (StringUtils.hasText(headers)) {
                        logMsg.append(" | Headers: ").append(headers);
                    }
                }
            }
            
            logMsg.append(" | RemoteAddr: ").append(remoteAddr);
            
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
        String queryString = request.getQueryString();
        String remoteAddr = getClientIpAddress(request);
        int status = response.getStatus();
        
        StringBuilder logMsg = new StringBuilder();
        logMsg.append("TraceId: ").append(traceId).append(" | ");
        
        // 使用自定义URL格式
        String formattedUrl = formatUrl(method, uri, queryString, remoteAddr);
        logMsg.append("URL: ").append(formattedUrl).append(" | ");
        
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
     * 格式化URL
     */
    private String formatUrl(String method, String uri, String queryString, String remoteAddr) {
        String format = properties.getHttpLog().getUrlFormat();
        
        // 替换占位符
        String result = format
                .replace("{method}", method != null ? method : "")
                .replace("{uri}", uri != null ? uri : "")
                .replace("{remoteAddr}", remoteAddr != null ? remoteAddr : "");
        
        // 处理查询字符串
        if (properties.getHttpLog().isIncludeQueryString() && StringUtils.hasText(queryString)) {
            result = result.replace("{queryString}", "?" + queryString);
        } else {
            result = result.replace("{queryString}", "");
        }
        
        return result.trim();
    }
    
    /**
     * 获取过滤后的请求头信息
     */
    private String getFilteredHeaders(HttpServletRequest request) {
        StringBuilder headers = new StringBuilder();
        
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            
            // 检查是否在排除列表中
            boolean shouldExclude = properties.getHttpLog().getExcludeHeaders().stream()
                    .anyMatch(excludeHeader -> headerName.toLowerCase().contains(excludeHeader.toLowerCase()));
            
            if (!shouldExclude) {
                if (headers.length() > 0) {
                    headers.append(", ");
                }
                headers.append(headerName).append("=").append(request.getHeader(headerName));
            }
        }
        
        return headers.toString();
    }
    
    /**
     * 格式化请求参数
     */
    private String formatRequestParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap == null || parameterMap.isEmpty()) {
            return "";
        }
        
        if (argumentFormatConfig.getType() == ArgumentFormatType.KEY_VALUE) {
            return formatParametersAsKeyValue(parameterMap);
        } else {
            return formatParametersAsJson(parameterMap);
        }
    }
    
    /**
     * 将参数格式化为 key=value 格式
     */
    private String formatParametersAsKeyValue(Map<String, String[]> parameterMap) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();
            
            if (!first) {
                sb.append(argumentFormatConfig.getSeparator());
            }
            first = false;
            
            sb.append(key).append(argumentFormatConfig.getKeyValueSeparator());
            
            if (values.length == 1) {
                sb.append(values[0]);
            } else {
                sb.append("[");
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append(values[i]);
                }
                sb.append("]");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 将参数格式化为 JSON 格式
     */
    private String formatParametersAsJson(Map<String, String[]> parameterMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            
            sb.append("\"").append(entry.getKey()).append("\":");
            
            String[] values = entry.getValue();
            if (values.length == 1) {
                sb.append("\"").append(values[0]).append("\"");
            } else {
                sb.append("[");
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append("\"").append(values[i]).append("\"");
                }
                sb.append("]");
            }
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor) && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp) && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    @Override
    public void destroy() {
        log.info("Atlas Log filter destroyed");
    }
}