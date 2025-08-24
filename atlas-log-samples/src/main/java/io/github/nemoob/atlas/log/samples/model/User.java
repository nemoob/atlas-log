package io.github.nemoob.atlas.log.samples.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户实体（纯POJO类）
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class User {
    
    private Long id;
    
    /**
     * 用户ID（业务ID）
     */
    private String uid;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 用户类型：normal, vip, admin
     */
    private String userType;
    
    /**
     * 用户状态：active, inactive, locked
     */
    private String status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}