package com.example.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class TaskCreateRequest {

    @NotBlank(message = "contentId 不能为空")
    private String contentId;

    @NotNull(message = "contentType 不能为空")
    @Pattern(regexp = "VIDEO|COMMENT|DANMAKU|LIVE",
            message = "contentType 只能是 VIDEO/COMMENT/DANMAKU/LIVE")
    private String contentType;

    @Min(value = 0, message = "priority 最小是 0")
    @Max(value = 2, message = "priority 最大是 2")
    private Integer priority = 2;

    public String getContentId() { return contentId; }
    public void setContentId(String contentId) { this.contentId = contentId; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
}
