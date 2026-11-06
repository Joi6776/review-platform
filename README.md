# review-platform

内容审核平台的实习项目：人审工作台、派单调度、处置与审计。

面向的岗位要求：后台服务开发、任务派单逻辑、审核指标监控、内容处置与操作可追溯。

## 技术栈

| 层次 | 选型 | 版本 |
|---|---|---|
| 语言 | Java | 21 (LTS) |
| 框架 | Spring Boot | 3.5.16 |
| 持久层 | MyBatis-Plus | 3.5.18.1 |
| 数据库 | MySQL | 8.0 |
| 缓存/队列 | Redis | 7.x |
| 消息 | Kafka | 计划第 6 阶段引入 |

## 数据模型

五张表，职责不重叠：

| 表 | 作用 | 关键设计 |
|---|---|---|
| `task` | 审核任务，派单的基本单位 | `version` 做乐观锁；`deadline_at` 做超时回补；`uniq(content_id, content_type)` 保证同一条内容不重复进审 |
| `review_case` | 判定违规后生成的案件 | 处置与申诉的主体是案件，不是任务 |
| `disposition` | 处置记录：删除/限流/打标/封禁 | 撤销不修改旧记录，而是新增一条 `revert_of` 指向它 |
| `audit_log` | 审计日志 | 只追加，不更新不删除；存 before/after JSON 快照 |
| `task_event` | 状态流转事件 | 每次跳转一条，能还原完整时间线 |

## 状态机

所有状态跳转只经过 `TaskStateMachine` 一个入口，非法跳转直接抛异常。

```
PENDING --CLAIM--> REVIEWING --APPROVE--> APPROVED(终态)
                     |  |
              RELEASE|  |DISPOSE
                     |  v
                  PENDING   DISPOSED --APPEAL--> APPEALING --UPHOLD--> DISPOSED
                                                      |
                                                  REVERT
                                                      v
                                                  REVERTED(终态)
```

| 当前状态 | 可用动作 | 下一状态 |
|---|---|---|
| PENDING | CLAIM | REVIEWING |
| REVIEWING | RELEASE / APPROVE / DISPOSE | PENDING / APPROVED / DISPOSED |
| DISPOSED | APPEAL | APPEALING |
| APPEALING | UPHOLD / REVERT | DISPOSED / REVERTED |
| APPROVED、REVERTED | 无（终态） | — |

## 本地启动

前置：JDK 21、Maven、Docker（或本地 MySQL + Redis）。

```bash
# 1. 起中间件（在 dev-setup 目录）
docker compose up -d mysql redis

# 2. 建表（首次）
docker cp src/main/resources/schema.sql review-mysql:/tmp/schema.sql
docker exec review-mysql sh -c "mysql -uroot -proot1234 < /tmp/schema.sql"

# 3. 跑测试
mvn test

# 4. 启动应用
mvn spring-boot:run
```

连接信息默认写在 `application.yml`：MySQL `localhost:3306/review_system`（root/root1234），Redis `localhost:6379`。

## 目录结构

```
src/main/java/com/example/review/
├── ReviewApplication.java        启动类
├── domain/
│   ├── TaskStatus.java           状态枚举（含终态标记）
│   └── TaskAction.java           动作枚举
└── service/
    └── TaskStateMachine.java     状态流转表与校验
src/test/java/...                 状态机规则单测
src/main/resources/
├── application.yml               数据源、Redis、MyBatis 配置
└── schema.sql                    建表脚本
```

## 设计取舍

- **状态机单独抽成静态类**，不放进 Service：它没有依赖，可以脱离 Spring 和数据库跑单测，规则改动时反馈最快。
- **撤销不删除记录**：`disposition.revert_of` 指回被撤销的那条，审计链不会断。删除操作在内容安全场景里等于销毁证据。
- **重放用事件表而不是日志文件**：`task_event` 每次跳转落一条，出问题时能还原某条任务的完整生命周期。
- **暂时不接 Kafka**：v1 用 HTTP 接口模拟内容进审，先让主链路跑通；Kafka 放在第 6 阶段，替换成本极低。

## Roadmap

- [x] 数据模型与建表脚本
- [x] 状态机与单测
- [ ] 任务接入接口（模拟内容进审）
- [ ] 派单调度：优先级队列 + Redis zset 延迟队列 + 乐观锁领取
- [ ] 人审工作台：待审列表、批量提交
- [ ] 处置与审计：四种处置动作、双人复核、撤销回滚
- [ ] 指标看板：积压量、平均处理时长、P99
- [ ] Kafka 接入与压测
