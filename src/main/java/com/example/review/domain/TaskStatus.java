package com.example.review.domain;

/**
 * 审核任务状态。状态数量刻意保持很少——每多一个状态，流转规则就成倍增加，
 * 后面加功能时最容易失控的就是这张表。
 */
public enum TaskStatus {

    /** 待审：已进库，还没人领取 */
    PENDING("待审", false),

    /** 审核中：被某个审核员领取，超时会被回收 */
    REVIEWING("审核中", false),

    /** 审核通过：内容没问题，进入终态 */
    APPROVED("审核通过", true),

    /** 已处置：判定违规并执行了删除/限流/打标等动作 */
    DISPOSED("已处置", false),

    /** 申诉中：用户对处置结果不服 */
    APPEALING("申诉中", false),

    /** 已撤销：复核认为误判，处置被撤销，进入终态 */
    REVERTED("已撤销", true);

    private final String desc;
    private final boolean terminal;

    TaskStatus(String desc, boolean terminal) {
        this.desc = desc;
        this.terminal = terminal;
    }

    public String getDesc() {
        return desc;
    }

    /** 终态：不再接受任何动作 */
    public boolean isTerminal() {
        return terminal;
    }
}
