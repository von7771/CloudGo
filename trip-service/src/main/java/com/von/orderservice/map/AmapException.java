package com.von.orderservice.map;

/**
 * 高德地图业务异常。
 * <p>
 * 继承 {@link RuntimeException}，表示调用高德 API 或地图相关业务校验失败。
 * 会被 {@link com.von.orderservice.controller.GlobalExceptionHandler} 捕获，
 * 统一返回 HTTP 400 和 JSON 错误信息，而不是 500 堆栈。
 * </p>
 */
public class AmapException extends RuntimeException {

    /**
     * @param message 面向用户的错误描述，如「未配置高德 API Key」「地址找不到」
     */
    public AmapException(String message) {
        super(message); // 把错误信息传给父类 RuntimeException，便于 getMessage() 获取
    }
}
