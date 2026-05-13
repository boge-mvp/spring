package com

import org.apache.logging.log4j.core.Core
import org.apache.logging.log4j.core.config.plugins.util.PluginRegistry
import org.apache.logging.log4j.core.lookup.StrLookup
import org.junit.jupiter.api.Test

class Log4jPluginsTest {

    @Test
    fun test() {

        // 指定Log4j2Plugins.dat文件的路径
        val pluginsDatFilePath =
            "target/classes/META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat"
        // 读取Log4j2Plugins.dat文件
        // 使用PluginRegistry解析数据
        val pluginRegistry = PluginRegistry.getInstance()
        val call = pluginRegistry.loadFromMainClassLoader()

        // 遍历解析后的插件信息
        println(" ${call.size} ")
        println( call.keys.joinToString(",") )

//        call["core"]
//            ?.also { println("  ${it.size} ") }
//            ?.filter {
//                it.pluginClass.packageName.startsWith("com.log4j", true)
//            }
//            ?.forEach {
//
//                // 输出插件类型和名称
//                println("输出： ${it.elementName} : ${it.pluginClass}")
//            }
//        call["lookup"]
//            ?.also { println("  ${it.size} ") }
//            ?.filter {
//                it.pluginClass.packageName.startsWith("com.log4j", true)
//            }
//            ?.forEach {
//
//                // 输出插件类型和名称
//                println("输出： ${it.elementName} : ${it.pluginClass}")
//            }

        println("   ****************************   ")

        call.forEach { (t, pluginTypes) ->
            println("---------------------   $t ${pluginTypes.size}")
            pluginTypes.forEach {
                // 输出插件类型和名称
                println("Plugin Type: " + it.pluginClass)
                println("Plugin Name: " + it.elementName)
            }


        }

    }


    @Test
    fun testCustom() {

        // 指定Log4j2Plugins.dat文件的路径
        val pluginsDatFilePath =
            "target/classes/META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat"
        // 读取Log4j2Plugins.dat文件
        // 使用PluginRegistry解析数据
        val pluginRegistry = PluginRegistry.getInstance()
        val call = pluginRegistry.loadFromMainClassLoader()

        // 遍历解析后的插件信息
        println(" ${call.size} ")
        println( call.keys.joinToString(",") )

//        call["core"]
//            ?.also { println("  ${it.size} ") }
//            ?.filter {
//                it.pluginClass.packageName.startsWith("com.log4j", true)
//            }
//            ?.forEach {
//
//                // 输出插件类型和名称
//                println("输出： ${it.elementName} : ${it.pluginClass}")
//            }
//        call["lookup"]
//            ?.also { println("  ${it.size} ") }
//            ?.filter {
//                it.pluginClass.packageName.startsWith("com.log4j", true)
//            }
//            ?.forEach {
//
//                // 输出插件类型和名称
//                println("输出： ${it.elementName} : ${it.pluginClass}")
//            }

        println("--------------------------------------------------------------   ")

        call.forEach { (t, pluginTypes) ->
            println("---------------------   $t ${pluginTypes.size}")
            pluginTypes
                .filter {
                    it.pluginClass.packageName.startsWith("com.log4j", true)
                }
                .forEach {
                // 输出插件类型和名称
                println("Plugin Type: " + it.pluginClass)
                println("Plugin Name: " + it.elementName)
            }


        }

    }


}