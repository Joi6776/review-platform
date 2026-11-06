package com.example.review.service;

import com.example.review.domain.TaskAction;
import com.example.review.domain.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 状态机的规则测试。
 * 这里断言的不是「代码能跑」，而是「业务规则没被写错」——
 * 后面谁改坏了流转表，这几行会立刻红。
 */
class TaskStateMachineTest {

    @Test
    void 待审任务可以被领取() {
        assertEquals(TaskStatus.REVIEWING,
                TaskStateMachine.next(TaskStatus.PENDING, TaskAction.CLAIM));
    }

    @Test
    void 审核中可以被释放回队列() {
        assertEquals(TaskStatus.PENDING,
                TaskStateMachine.next(TaskStatus.REVIEWING, TaskAction.RELEASE));
    }

    @Test
    void 审核中可以处置() {
        assertEquals(TaskStatus.DISPOSED,
                TaskStateMachine.next(TaskStatus.REVIEWING, TaskAction.DISPOSE));
    }

    @Test
    void 已处置可以被申诉再撤销() {
        TaskStatus appealing = TaskStateMachine.next(TaskStatus.DISPOSED, TaskAction.APPEAL);
        assertEquals(TaskStatus.APPEALING, appealing);
        assertEquals(TaskStatus.REVERTED, TaskStateMachine.next(appealing, TaskAction.REVERT));
    }

    @Test
    void 已处置不能直接跳回待审() {
        assertThrows(TaskStateMachine.IllegalTransitionException.class,
                () -> TaskStateMachine.next(TaskStatus.DISPOSED, TaskAction.CLAIM));
    }

    @Test
    void 终态不接受任何动作() {
        assertTrue(TaskStatus.APPROVED.isTerminal());
        assertTrue(TaskStatus.REVERTED.isTerminal());
        assertThrows(TaskStateMachine.IllegalTransitionException.class,
                () -> TaskStateMachine.next(TaskStatus.APPROVED, TaskAction.DISPOSE));
        assertTrue(TaskStateMachine.allowedActions(TaskStatus.APPROVED).isEmpty());
    }

    @Test
    void 待审状态不允许直接处置() {
        assertFalse(TaskStateMachine.canApply(TaskStatus.PENDING, TaskAction.DISPOSE));
        assertThrows(TaskStateMachine.IllegalTransitionException.class,
                () -> TaskStateMachine.next(TaskStatus.PENDING, TaskAction.DISPOSE));
    }
}
