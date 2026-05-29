package com.aigm.common.util;

import com.aigm.common.exception.BizException;
import com.aigm.common.result.ResultCode;
import lombok.Data;

/**
 * 分页请求载体（基线 §3.7）：page 从 1，size 默认 10、最大 100，超界抛 1103。
 */
@Data
public class PageQuery {

    private long page = 1;
    private long size = 10;

    public PageQuery() {}

    public PageQuery(long page, long size) {
        this.page = page;
        this.size = size;
    }

    public static PageQuery of(Long page, Long size) {
        long p = (page == null || page < 1) ? 1 : page;
        long s = (size == null) ? 10 : size;
        if (s < 1 || s > 100) {
            throw new BizException(ResultCode.PAGE_PARAM_INVALID);
        }
        return new PageQuery(p, s);
    }

    public long offset() {
        return (page - 1) * size;
    }
}
