package com.example.review.domain;

/**
 * 审核任务上可以发生的动作。动作和状态分开定义，
 * 是为了让「谁在什么状态下能做什么」变成一张可以读的表格，而不是散落在 if 里。
 */
public enum TaskAction {

    /** 领取：审核员从队列里拿走一条任务 */
    CLAIM,

    /** 释放：超时未处理被系统回收，或审核员主动放回 */
    RELEASE,

    /** 通过：内容没问题 */
    APPROVE,

    /** 处置：判定违规，随后产生 disposition 记录 */
    DISPOSE,

    /** 申诉：用户对处置结果提出异议 */
    APPEAL,

    /** 维持：复核后认为原处置正确 */
    UPHOLD,

    /** 撤销：复核后认为误判，处置作废 */
    REVERT
}
