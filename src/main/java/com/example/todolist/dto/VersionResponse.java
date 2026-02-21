package com.example.todolist.dto;

/**
 * 清单版本响应
 * 用于前端轮询检测清单是否有更新
 */
public class VersionResponse {
    private Long version;

    public VersionResponse() {}

    public VersionResponse(Long version) {
        this.version = version;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
