package com.boge.utils

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object BeanUtils {

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T : Any> copy(source: Any, targetClass: Class<T>): T {
        val sourceProp = getAllProperties(source::class.java)
        val prop = getAllProperties(targetClass)
        val constructor= targetClass.declaredConstructors.minBy { it.parameterCount }
        constructor.isAccessible = true
        val parameters = constructor.parameters.map { pa->
            sourceProp.find {
                it.name == pa.name
            }?.let {
                it.isAccessible = true
                it.get(source)
            }
        }

//        println( "$targetClass ${constructor.parameterCount} ${constructor.parameters.size} ${constructor.parameters} "  )

        val target = constructor.newInstance(*parameters.toTypedArray())

        prop.forEach {
            it.isAccessible = true
            sourceProp.find { s -> s.name == it.name }?.let { p ->
                p.isAccessible = true
                val value = p.get(source)
                if (value != null) {
                    it.set(target, value)
                }
            }
        }
        return target as T
    }

    @JvmStatic
    fun getAllProperties(cls: Class<*>?): MutableList<Field> {
        var tempClas = cls
        val list = mutableListOf<Field>()
        while (tempClas != null) {
            list.addAll(tempClas.declaredFields.filter {
                !Modifier.isStatic(it.modifiers)
            })
            tempClas = tempClas.superclass
        }
        return list
    }

    @JvmStatic
    fun getField(cls: Class<*>?, name: String, isAccessible: Boolean = false): Field? {
        var tempClas = cls
        var target: Field? = null
        while (tempClas != null && target == null) {
            target = tempClas.declaredFields.find {
                it.name == name
            }
            tempClas = tempClas.superclass
        }
        target?.let { it.isAccessible = isAccessible }
        return target
    }

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T : Any> getField(target: Any, name: String, isAccessible: Boolean = false): T? {
        return getField(target::class.java, name, isAccessible)?.let {
            it.get(target) as? T
        }
    }

    @JvmStatic
    fun getMethod(cls: Class<*>?, name: String, vararg names: Class<*>): Method? {
        var tempClas = cls
        var target: Method? = null
        while (tempClas != null && target == null) {
            target = tempClas.declaredMethods.find {
                var index = 0
                it.name == name && it.parameterTypes.all { c ->
                    (c == names[index]).also { index++ }
                }
            }
            tempClas = tempClas.superclass
        }
        return target
    }

    @JvmStatic
    fun getMethod(cls: Class<*>?, name: String, isAccessible: Boolean = false, vararg names: Class<*>): Method? {
        return getMethod(cls, name, *names)?.also { it.isAccessible = isAccessible }
    }


}