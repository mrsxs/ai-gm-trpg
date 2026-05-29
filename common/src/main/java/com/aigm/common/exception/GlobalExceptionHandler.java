package com.aigm.common.exception;

import com.aigm.common.result.R;
import com.aigm.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理（基线 §5.2）。仅对引入 spring-web(MVC) 的业务服务生效；gateway(WebFlux) 不装配。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e) {
        log.warn("[BizException] code={}, msg={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public R<Void> handleValid(Exception e) {
        FieldError fe = null;
        if (e instanceof MethodArgumentNotValidException me) {
            fe = me.getBindingResult().getFieldError();
        } else if (e instanceof BindException be) {
            fe = be.getBindingResult().getFieldError();
        }
        String msg = fe != null ? fe.getDefaultMessage() : ResultCode.PARAM_INVALID.getMessage();
        return R.fail(ResultCode.PARAM_INVALID.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleOther(Exception e) {
        log.error("[UnhandledException]", e);
        return R.fail(ResultCode.SYSTEM_ERROR);
    }
}
