package com.example.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DisposeRequest {
    @NotBlank(message = "action 不能为空")
    @Pattern(regexp = "DELETE|LIMIT|TAG|BAN",
            message = "action 只能是 DELETE/LIMIT/TAG/BAN")
    private String action;

    @NotBlank(message = "violationType 不能为空")
    private String violationType;

    @Size(max = 255, message = "reason 太长")
    private String reason;

    /** 复核人，高风险动作（删除、封禁）应该填 */
    private Long reviewerId;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getViolationType() { return violationType; }
    public void setViolationType(String violationType) { this.violationType = violationType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
}
