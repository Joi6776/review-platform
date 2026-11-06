package com.example.review.service;

import com.example.review.domain.TaskAction;
import com.example.review.domain.TaskStatus;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 任务状态机：把「当前状态 + 动作 → 下一状态」集中成一张表。
 *
 * 为什么要单独抽出来：
 * 1. 所有状态跳转只有这一个入口，不会有某段代码偷偷把 status 改掉；
 * 2. 非法跳转在这里统一拒绝，错误信息一致，排查时不用到处找；
 * 3. 可以脱离数据库和 Spring 单独写单测，跑一遍就能确认规则没错。
 */
public final class TaskStateMachine {

    private static final Map<TaskStatus, Map<TaskAction, TaskStatus>> TRANSITIONS = build();

    private TaskStateMachine() {
    }

    private static Map<TaskStatus, Map<TaskAction, TaskStatus>> build() {
        Map<TaskStatus, Map<TaskAction, TaskStatus>> map = new EnumMap<>(TaskStatus.class);

        map.put(TaskStatus.PENDING, transitions(
                TaskAction.CLAIM, TaskStatus.REVIEWING));

        map.put(TaskStatus.REVIEWING, transitions(
                TaskAction.RELEASE, TaskStatus.PENDING,
                TaskAction.APPROVE, TaskStatus.APPROVED,
                TaskAction.DISPOSE, TaskStatus.DISPOSED));

        map.put(TaskStatus.DISPOSED, transitions(
                TaskAction.APPEAL, TaskStatus.APPEALING));

        map.put(TaskStatus.APPEALING, transitions(
                TaskAction.UPHOLD, TaskStatus.DISPOSED,
                TaskAction.REVERT, TaskStatus.REVERTED));

        // APPROVED 与 REVERTED 是终态，不配置任何出口

        return Collections.unmodifiableMap(map);
    }

    private static Map<TaskAction, TaskStatus> transitions(Object... pairs) {
        Map<TaskAction, TaskStatus> m = new EnumMap<>(TaskAction.class);
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((TaskAction) pairs[i], (TaskStatus) pairs[i + 1]);
        }
        return Collections.unmodifiableMap(m);
    }

    /**
     * 计算下一个状态。非法跳转直接抛异常，调用方不需要自己判断。
     */
    public static TaskStatus next(TaskStatus from, TaskAction action) {
        if (from == null || action == null) {
            throw new IllegalTransitionException("状态和动作都不能为空");
        }
        if (from.isTerminal()) {
            throw new IllegalTransitionException(
                    "状态 " + from + " 是终态，不能再执行 " + action);
        }
        Map<TaskAction, TaskStatus> exits = TRANSITIONS.get(from);
        TaskStatus to = (exits == null) ? null : exits.get(action);
        if (to == null) {
            throw new IllegalTransitionException(
                    "状态 " + from + " 不允许执行 " + action + "，可用动作：" + allowedActions(from));
        }
        return to;
    }

    /** 当前状态下能不能做这个动作，用于接口层的快速校验 */
    public static boolean canApply(TaskStatus from, TaskAction action) {
        if (from == null || action == null || from.isTerminal()) {
            return false;
        }
        Map<TaskAction, TaskStatus> exits = TRANSITIONS.get(from);
        return exits != null && exits.containsKey(action);
    }

    /** 当前状态下可用的动作集合，返回给前端按钮用 */
    public static Set<TaskAction> allowedActions(TaskStatus from) {
        if (from == null) {
            return Collections.emptySet();
        }
        Map<TaskAction, TaskStatus> exits = TRANSITIONS.get(from);
        return exits == null ? Collections.emptySet() : exits.keySet();
    }

    /** 非法状态跳转，属于业务规则违例，不是系统故障 */
    public static class IllegalTransitionException extends RuntimeException {
        public IllegalTransitionException(String message) {
            super(message);
        }
    }
}
