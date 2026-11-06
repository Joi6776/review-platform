package com.example.review.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DisposalMapper {
    @Insert("INSERT INTO review_case (task_id, violation_type, status) " +
            "VALUES (#{taskId}, #{violationType}, 'OPEN')")
    int insertCase(@Param("taskId") Long taskId,
                   @Param("violationType") String violationType);

    @Insert("INSERT INTO disposition (case_id, action, operator_id, reviewer_id, reason, status) " +
            "VALUES (#{caseId}, #{action}, #{operatorId}, #{reviewerId}, #{reason}, 'APPLIED')")
    int insertDisposition(@Param("caseId") Long caseId,
                          @Param("action") String action,
                          @Param("operatorId") Long operatorId,
                          @Param("reviewerId") Long reviewerId,
                          @Param("reason") String reason);

    @Insert("INSERT INTO audit_log (operator_id, action, target_type, target_id, before_json, after_json) " +
            "VALUES (#{operatorId}, #{action}, #{targetType}, #{targetId}, #{beforeJson}, #{afterJson})")
    int insertAudit(@Param("operatorId") Long operatorId,
                    @Param("action") String action,
                    @Param("targetType") String targetType,
                    @Param("targetId") Long targetId,
                    @Param("beforeJson") String beforeJson,
                    @Param("afterJson") String afterJson);

    @Select("SELECT LAST_INSERT_ID()")
    Long lastInsertId();
}
