package com.aigm.common.util;

import lombok.Data;

import java.util.List;

/**
 * 分页响应载体（基线 §3.1）：{list,total,page,size}。
 */
@Data
public class PageResult<T> {

    private List<T> list;
    private long total;
    private long page;
    private long size;

    public static <T> PageResult<T> of(List<T> list, long total, long page, long size) {
        PageResult<T> r = new PageResult<>();
        r.setList(list);
        r.setTotal(total);
        r.setPage(page);
        r.setSize(size);
        return r;
    }
}
