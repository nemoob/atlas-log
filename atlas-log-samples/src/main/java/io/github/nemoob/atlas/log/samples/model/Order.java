package io.github.nemoob.atlas.log.samples.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体（纯POJO类）
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class Order {
    
    private Long id;
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 订单金额
     */
    private BigDecimal amount;
    
    /**
     * 订单状态：pending, paid, shipped, completed, cancelled
     */
    private String status;
    
    /**
     * 是否紧急订单
     */
    private Boolean urgent;
    
    /**
     * 支付方式
     */
    private String paymentMethod;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}