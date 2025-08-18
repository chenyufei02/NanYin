// 文件位置: src/main/java/com/whu/nanyin/pojo/vo/ApiResponseVO.java
package com.whu.nanyin.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通用API响应对象
 * 
 * 这是一个泛型类，用于统一封装所有API接口的响应数据格式。
 * 采用标准的RESTful API响应结构，确保前后端数据交互的一致性。
 * 
 * 主要特点：
 * 1. 泛型设计 - 可以携带任意类型的数据
 * 2. 统一格式 - 所有API响应都使用相同的结构
 * 3. 状态标识 - 通过success字段明确操作结果
 * 4. 消息提示 - 提供用户友好的操作反馈
 * 5. 数据载荷 - 灵活携带业务数据
 * 
 * 响应格式示例：
 * {
 *   "success": true,
 *   "message": "操作成功",
 *   "data": {
 *     // 具体的业务数据
 *   }
 * }
 * 
 * @param <T> 数据载荷的类型，可以是任意Java对象
 * @author nanyin
 */
@Data  // Lombok注解，自动生成getter、setter、toString、equals和hashCode方法
@Schema(description = "通用API响应对象")  // Swagger文档注解，用于API文档生成
public class ApiResponseVO<T> {  // 使用泛型<T>，使该类可以携带任意类型的数据

    /**
     * 操作是否成功的标识
     * 
     * true表示操作成功，false表示操作失败。
     * 前端可以根据此字段判断请求是否成功处理。
     */
    @Schema(description = "操作是否成功", example = "true")
    private boolean success;

    /**
     * 返回给用户的消息文本
     */
    @Schema(description = "返回的消息文本", example = "操作成功！")
    private String message;

    /**
     * 携带的业务数据
     * 
     * 使用泛型T，可以存放任何类型的数据对象，如用户信息、列表数据、JWT令牌等。
     * 当操作失败时，此字段通常为null；成功时包含具体的业务数据。
     */
    @Schema(description = "携带的额外数据")
    private T data;


    /**
     * 全参数构造函数
     * 
     * 创建一个包含所有字段的ApiResponseVO实例。
     * 通常不直接使用，而是通过静态工厂方法创建实例。
     * 
     * @param success 操作是否成功
     * @param message 返回的消息文本
     * @param data 携带的业务数据
     */
    public ApiResponseVO(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建成功响应的静态工厂方法
     * 
     * 用于创建表示操作成功的响应对象，自动设置success为true。
     * 这是推荐的创建成功响应的方式，代码更简洁易读。
     * 
     * 使用示例：
     * ApiResponseVO.success("登录成功", userInfo);
     * ApiResponseVO.success("获取数据成功", dataList);
     * 
     * @param <T> 数据类型
     * @param message 成功消息
     * @param data 要返回的业务数据
     * @return 成功的ApiResponseVO实例
     */
    public static <T> ApiResponseVO<T> success(String message, T data) {
        return new ApiResponseVO<>(true, message, data);
    }

    /**
     * 创建失败响应的静态工厂方法
     * 
     * 用于创建表示操作失败的响应对象，自动设置success为false，data为null。
     * 这是推荐的创建失败响应的方式，确保错误响应的一致性。
     * 
     * 使用示例：
     * ApiResponseVO.error("用户名或密码错误");
     * ApiResponseVO.error("权限不足");
     * 
     * @param <T> 数据类型（失败时通常不需要指定，因为data为null）
     * @param message 错误消息
     * @return 失败的ApiResponseVO实例
     */
    public static <T> ApiResponseVO<T> error(String message) {
        return new ApiResponseVO<>(false, message, null);
    }
}