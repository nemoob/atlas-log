package io.github.nemoob.atlas.log.exception;

/**
 * Atlas Log基础异常类
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
public class LogException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public LogException() {
        super();
    }
    
    public LogException(String message) {
        super(message);
    }
    
    public LogException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public LogException(Throwable cause) {
        super(cause);
    }
}