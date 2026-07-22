package com.von.common.dto;

import java.util.List;

/**
 * 分页结果，管理后台列表接口通用。
 */
public record PageResult<T>(
        List<T> records,
        long total,
        int page,
        int size
) {
}
