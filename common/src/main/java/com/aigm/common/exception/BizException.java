package com.aigm.common.exception;

import com.aigm.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常（基线 §5.1，三构造器）。由 GlobalExceptionHandler 统一转 R。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
    }

    public BizException(ResultCode rc, String message) {
        super(message);
        this.code = rc.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
