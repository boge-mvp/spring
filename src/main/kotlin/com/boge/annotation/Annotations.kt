package com.boge.annotation

/**
 * 用来忽略属性值对比
 * @see com.applyPropEd
 * @see com.applyProp
 * @see com.unionProp
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS)
annotation class FilterIgnore
