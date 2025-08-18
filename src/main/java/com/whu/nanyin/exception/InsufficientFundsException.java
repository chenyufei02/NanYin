package com.whu.nanyin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 资金或份额不足异常类
 * 
 * 这是一个自定义的运行时异常，用于处理以下业务场景：
 * 1. 用户申购基金时账户余额不足
 * 2. 用户赎回基金时持有份额不足
 * 3. 其他涉及资金或份额检查的业务操作
 * 
 * 该异常继承自RuntimeException，属于非检查异常，不需要强制捕获
 * 使用@ResponseStatus注解自动将异常映射为HTTP 400状态码
 * 
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST) // 让Spring MVC在遇到此异常时返回400错误码
public class InsufficientFundsException extends RuntimeException {

    /**
     * 构造函数
     * 
     * 创建一个新的资金不足异常实例            
     */
    public InsufficientFundsException(String message) {
        super(message);
    }
}