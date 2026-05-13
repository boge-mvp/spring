package com.boge.condition

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

/**
 * 当前运行环境检测到 JUNIT 将禁止初始化bean
 *
 */
class JunitCondition: Condition {

    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        //
        return !context.environment.containsProperty(ConditionKey.JUNIT)
    }

}

object ConditionKey {
    const val JUNIT = "JUNIT"
}