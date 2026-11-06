-- 内容审核平台 · 建表脚本
-- 执行方式：mysql -uroot -p < schema.sql
-- 说明：所有表都用 utf8mb4，时间统一 Asia/Shanghai

CREATE DATABASE IF NOT EXISTS review_system
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE review_system;

-- ---------------------------------------------------------------------------
-- 1. 审核任务：内容进审后落成一条任务，是派单的基本单位
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS task (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    content_id    VARCHAR(64)  NOT NULL                COMMENT '业务侧内容 ID',
    content_type  VARCHAR(16)  NOT NULL                COMMENT 'VIDEO/COMMENT/DANMAKU/LIVE',
    priority      TINYINT      NOT NULL DEFAULT 2      COMMENT '0=P0 最高 1=P1 2=P2',
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '见 TaskStatus 状态机',
    assignee_id   BIGINT       NULL                    COMMENT '领取的审核员，NULL 表示未领取',
    version       INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    deadline_at   DATETIME     NULL                    COMMENT '超时回补时间，到点未处理则回收',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_content (content_id, content_type)   COMMENT '同一条内容不重复进审',
    KEY idx_pick (status, priority, created_at)        COMMENT '派单时按状态+优先级捞取',
    KEY idx_assignee (assignee_id, status)             COMMENT '工作台按人查自己的队列',
    KEY idx_deadline (status, deadline_at)             COMMENT '超时回补扫描'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='审核任务';

-- ---------------------------------------------------------------------------
-- 2. 案件：一条任务被判定违规后生成，是处置与申诉的主体
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS review_case (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id        BIGINT       NOT NULL                COMMENT '来源任务',
    violation_type VARCHAR(32)  NOT NULL                COMMENT '违规类型：色情/暴恐/广告...',
    status         VARCHAR(16)  NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CLOSED',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_task (task_id) COMMENT '一条任务最多一个案件',
    KEY idx_violation (violation_type, created_at) COMMENT '按违规类型统计'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='审核案件';

-- ---------------------------------------------------------------------------
-- 3. 处置记录：删除、限流、打标、封禁
--    回滚只新增一条 revert_of 指向原记录，绝不修改旧数据
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS disposition (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    case_id      BIGINT       NOT NULL                COMMENT '所属案件',
    action       VARCHAR(16)  NOT NULL                COMMENT 'DELETE/LIMIT/TAG/BAN',
    operator_id  BIGINT       NOT NULL                COMMENT '执行人',
    reviewer_id  BIGINT       NULL                    COMMENT '复核人，高风险动作必填',
    reason       VARCHAR(255) NULL                    COMMENT '处置理由',
    status       VARCHAR(16)  NOT NULL DEFAULT 'APPLIED' COMMENT 'APPLIED/REVERTED',
    revert_of    BIGINT       NULL                    COMMENT '指向被撤销的那条处置',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_case (case_id, created_at),
    KEY idx_operator (operator_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='处置记录';

-- ---------------------------------------------------------------------------
-- 4. 审计日志：只追加，不更新不删除
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    operator_id BIGINT       NOT NULL                COMMENT '操作人',
    action      VARCHAR(32)  NOT NULL                COMMENT '动作：CLAIM/DISPOSE/REVERT...',
    target_type VARCHAR(16)  NOT NULL                COMMENT 'TASK/CASE/DISPOSITION',
    target_id   BIGINT       NOT NULL                COMMENT '目标主键',
    before_json JSON         NULL                    COMMENT '操作前快照',
    after_json  JSON         NULL                    COMMENT '操作后快照',
    request_id  VARCHAR(64)  NULL                    COMMENT '链路追踪用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_target (target_type, target_id, created_at) COMMENT '查某个对象的历史',
    KEY idx_operator (operator_id, created_at)          COMMENT '查某个人干了什么'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='审计日志（只追加）';

-- ---------------------------------------------------------------------------
-- 5. 任务流转事件：每次状态跳转记一条，用于还原全过程
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS task_event (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id     BIGINT      NOT NULL                COMMENT '任务',
    from_status VARCHAR(16) NOT NULL                COMMENT '跳转前状态',
    to_status   VARCHAR(16) NOT NULL                COMMENT '跳转后状态',
    action      VARCHAR(16) NOT NULL                COMMENT '触发动作',
    operator_id BIGINT      NULL                    COMMENT '操作人，系统触发为空',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_task (task_id, id) COMMENT '按任务还原时间线'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='任务状态流转事件';
