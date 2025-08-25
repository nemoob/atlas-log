package io.github.nemoob.atlas.log.samples.service;

import io.github.nemoob.atlas.log.annotation.ExceptionHandler;
import io.github.nemoob.atlas.log.annotation.Log;
import io.github.nemoob.atlas.log.annotation.LogLevel;
import io.github.nemoob.atlas.log.samples.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 订单服务类
 * 演示Atlas Log的高级功能，包括多注解、复杂条件等
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@Service
@Slf4j
// 类级别注解 - 为所有方法提供基础日志
@Log(
    value = "订单服务基础日志",
    level = LogLevel.DEBUG,
    tags = {"business", "order"},
    logExecutionTime = true,
    group = "business"
)
public class OrderService {
    
    /**
     * 创建订单 - 多注解演示
     */
    @Log(
        value = "订单安全审计",
        condition = "#{args[0].amount.compareTo(new java.math.BigDecimal('10000')) > 0}",
        level = LogLevel.WARN,
        tags = {"security", "high-value"},
        group = "security"
    )
    @Log(
        value = "紧急订单处理",
        condition = "#{args[0].urgent == true}",
        level = LogLevel.INFO,
        tags = {"urgent"},
        enterMessage = "开始处理紧急订单: 用户ID=#{args[0].userId}",
        exitMessage = "紧急订单处理完成: 订单号=#{result.orderNo}",
        group = "urgent"
    )
    @Log(
        value = "创建订单: 用户ID=#{args[0].userId}, 金额=#{args[0].amount}",
        level = LogLevel.INFO,
        tags = {"create"},
        logArgs = true,
        logResult = true
    )
    public Order createOrder(Order order) {
        // 生成订单号
        String orderNo = "ORD" + System.currentTimeMillis();
        
        order.setId((long) (Math.random() * 1000000))
             .setOrderNo(orderNo)
             .setStatus("pending")
             .setCreateTime(LocalDateTime.now())
             .setUpdateTime(LocalDateTime.now());
        
        log.info("Order created successfully: {}", orderNo);
        return order;
    }
    
    /**
     * 支付订单 - 复杂异常处理
     */
    @Log(
        value = "订单支付处理: 订单号=#{args[0]}",
        enterMessage = "开始支付: 订单号=#{args[0]}",
        exitMessage = "支付完成: 订单号=#{args[0]}, 结果=#{result}",
        exceptionHandlers = {
            @ExceptionHandler(
                exception = IllegalStateException.class,
                level = LogLevel.WARN,
                message = "订单状态异常: 订单号=#{args[0]}, 错误=#{exception.message}",
                logStackTrace = false
            ),
            @ExceptionHandler(
                exception = RuntimeException.class,
                level = LogLevel.ERROR,
                message = "支付处理失败: 订单号=#{args[0]}, 错误=#{exception.message}"
            )
        }
    )
    public boolean payOrder(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        
        // 模拟订单状态检查
        if (orderNo.contains("INVALID")) {
            throw new IllegalStateException("订单状态无效，无法支付");
        }
        
        // 模拟支付失败
        if (orderNo.contains("FAIL")) {
            throw new RuntimeException("支付网关异常");
        }
        
        log.info("Order payment successful: {}", orderNo);
        return true;
    }
    
    /**
     * 取消订单 - 条件复杂
     */
    @Log(
        value = "取消订单: 订单号=#{args[0]}",
        condition = "#{args[0] != null and !args[0].trim().isEmpty()}",
        tags = {"cancel"},
        level = LogLevel.WARN
    )
    public void cancelOrder(String orderNo) {
        log.info("Order cancelled: {}", orderNo);
    }
    
    /**
     * 查询订单状态 - 演示返回值访问
     */
    @Log(
        value = "查询订单状态完成",
        condition = "#{result != null}",
        exitMessage = "订单状态查询结果: 订单号=#{args[0]}, 状态=#{result.status}"
    )
    public Order getOrderStatus(String orderNo) {
        // 模拟查询
        return new Order()
                .setOrderNo(orderNo)
                .setStatus("pending")
                .setAmount(new BigDecimal("99.99"))
                .setUserId("user_123");
    }
    
    /**
     * 发货处理 - 演示时间监控
     */
    @Log(
        value = "订单发货处理",
        logExecutionTime = true,
        enterMessage = "开始发货: 订单号=#{args[0]}",
        exitMessage = "发货完成: 订单号=#{args[0]}, 耗时=#{executionTime}ms"
    )
    public void shipOrder(String orderNo) {
        // 模拟耗时操作
        try {
            Thread.sleep(100); // 模拟发货处理时间
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        log.info("Order shipped successfully: {}", orderNo);
    }
    
    /**
     * VIP订单特殊处理
     */
    @Log(
        value = "VIP订单处理",
        condition = "#{@userService.isVipUser(args[0].userId)}",
        tags = {"vip", "special"},
        level = LogLevel.INFO,
        logArgs = true,
        logResult = true
    )
    public Order processVipOrder(Order order) {
        // VIP订单特殊处理逻辑
        order.setStatus("vip_processing");
        log.info("VIP order priority processing: {}", order.getOrderNo());
        return order;
    }
    
    /**
     * 批量处理订单
     */
    @Log(
        value = "批量处理订单",
        condition = "#{args[0] != null and args[0].length > 0}",
        enterMessage = "开始批量处理: 订单数量=#{args[0].length}",
        exitMessage = "批量处理完成: 成功处理=#{result}个订单"
    )
    public int batchProcessOrders(String[] orderNos) {
        if (orderNos == null || orderNos.length == 0) {
            return 0;
        }
        
        int processed = 0;
        for (String orderNo : orderNos) {
            try {
                // 模拟处理单个订单
                log.debug("Processing order: {}", orderNo);
                processed++;
            } catch (Exception e) {
                log.warn("Failed to process order: {}", orderNo, e);
            }
        }
        
        return processed;
    }
}