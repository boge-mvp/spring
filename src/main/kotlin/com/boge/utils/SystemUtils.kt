package com.boge.utils

import com.boge.ApplicationHome
import com.logger
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.lang.management.ManagementFactory
import java.net.ServerSocket
import java.nio.file.Path
import java.util.*
import java.util.jar.Attributes
import java.util.jar.JarInputStream
import java.util.jar.Manifest
import kotlin.io.path.*
import kotlin.reflect.KClass

object SystemUtils {

    var javaClass: Class<*>? = null
    fun initStartUp(cls: KClass<*>) {
        javaClass = cls.java
    }

    // 指定 IP 范围
    @JvmStatic
    val range = arrayOf(
        intArrayOf(607649792, 608174079),
        intArrayOf(1038614528, 1039007743),
        intArrayOf(1783627776, 1784676351),
        intArrayOf(2035023872, 2035154943),
        intArrayOf(2078801920, 2079064063),
        intArrayOf(-1950089216, -1948778497),
        intArrayOf(-1425539072, -1425014785),
        intArrayOf(-1236271104, -1235419137),
        intArrayOf(-770113536, -768606209),
        intArrayOf(-569376768, -564133889)
    )

    @JvmStatic
    val PID_FILE by lazy { createKeyPID() }

    @JvmStatic
    val applicationHome by lazy {
        ApplicationHome.init(javaClass)
    }

    /**
     * 获取运行路径
     */
    @JvmStatic
    val runPath: Path
        get() = applicationHome.dir

    /**
     * 随机生成一个ip
     */
    @JvmStatic
    val randomIp: String
        get() {
            val random = Random()
            val index = random.nextInt(10)
            return num2ip(range[index][0] + random.nextInt(range[index][1] - range[index][0]))
        }

    /**
     * 将十进制转换成IP地址
     */
    @JvmStatic
    fun num2ip(ip: Int): String {
        val b = IntArray(4)
        b[0] = ip shr 24 and 0xff
        b[1] = ip shr 16 and 0xff
        b[2] = ip shr 8 and 0xff
        b[3] = ip and 0xff
        // 拼接 IP
        return b[0].toString() + "." + b[1] + "." + b[2] + "." + b[3]
    }

    /**
     * 检查端口是否可用
     */
    @JvmStatic
    fun isPortInUse(port: Int): Boolean {
        return try {
            val serverSocket = ServerSocket(port)
            serverSocket.close()
            false
        } catch (e: IOException) {
            logger.error("isPortInUse", e)
            true
        }
    }

    /**
     * 获取端口使用的进程信息
     */
    @JvmStatic
    fun getUsePortInfo(port: Int): String? {
        try {
            val p = Runtime.getRuntime().exec(arrayOf("lsof", "-i", ":$port"))
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            return reader.readLine()
        } catch (ignored: IOException) {
            logger.error("getUsePortInfo", ignored)
        }
        return null
    }

    /**
     * 根据端口 获取进程并杀死
     */
    @JvmStatic
    fun killProcessUsingPort(port: Int): Boolean {
        if (port == 0) return true
        try {
            val command = if (System.getProperty("os.name").lowercase().contains("windows")) {
                arrayOf("cmd", "/c", "netstat", "-ano", "|", "findstr", "$port")
            } else {
                arrayOf("lsof", "-i", ":$port")
            }
            println("查询进程：${command.joinToString(" ")}")
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                println("获得端口信息：$line")
                if (line!!.contains(port.toString())) {
                    val processId = line!!.trim { it <= ' ' }.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1]
                    println("kill进程Id $processId")
                    killProcessId(processId)
                }
            }
        } catch (e: IOException) {
            logger.error("killProcessUsingPort", e)
            return false
        }
        return true
    }

    /**
     * 获取build-info 构建信息
     */
    @JvmStatic
    fun getBuildInfo(clazz: Class<*>): Properties? {
        val packageName = clazz.`package`.name
        val packageInfo =
            clazz.classLoader.getResourceAsStream("${packageName.replace('.', '/')}/build-info.properties")
        return packageInfo?.let {
            Properties().apply {
                this.load(packageInfo)
//            val buildVersion = properties.getProperty("Implementation-Version")
//            val buildTimestamp = properties.getProperty("Build-Timestamp")
//            println("Build Version: $buildVersion")
//            println("Build Timestamp: $buildTimestamp")
            }
        }
    }

    /**
     * 终止某个进程命令
     * @param processId 进程id
     */
    @JvmStatic
    private fun killProcessId(processId: String) {
        if (System.getProperty("os.name").lowercase().contains("windows")) {
            Runtime.getRuntime().exec(arrayOf("taskkill", "/F", "/PID", processId))
        } else {
            Runtime.getRuntime().exec(arrayOf("kill", "-9", processId))
        }
    }

    /**
     * 获取 MANIFEST.MF 配置信息
     */
    @JvmStatic
    val manifest by lazy {
        val inputStream = (javaClass ?: SystemUtils::class.java).protectionDomain.codeSource?.location?.openStream()
        var manifest: Manifest? = null
        if (inputStream != null) manifest = JarInputStream(inputStream).manifest
        manifest
    }

    /**
     * 获取 MANIFEST.MF 配置信息
     */
    @JvmStatic
    val mainAttributes by lazy {
        manifest?.mainAttributes
    }

    /**
     * 获取 MANIFEST.MF 配置信息
     */
    @JvmStatic
    fun getManifestValue(name: Attributes.Name?): String {
//        val groupId = attributes.getValue("Implementation-Vendor-Id");
//        val artifactId = attributes.getValue("Implementation-Title");
//        println("Group ID: " + groupId);
//        println("Artifact ID: " + artifactId);
        return mainAttributes?.getValue(name) ?: ""
    }

    /**
     * 获取jar文件的名称（不包括扩展名）。
     * @return 返回jar文件的名称字符串。
     */
    @JvmStatic
    val jarName: String
        get() {
            return jarFileName.substringBefore(".")
        }

    /**
     * 获取jar文件的完整名称。
     * @return 返回jar文件的完整名称字符串。
     */
    @JvmStatic
    val jarFileName: String
        get() {
            // 判断jarRunPath代表的是目录还是文件，据此获取相应的名称
            val jarFileName = if (Path(jarRunPath).isDirectory()) {
                dir.substringAfterLast(File.separator)
            } else jarRunPath.substringAfterLast(File.separator)
            return jarFileName
        }

    /**
     * 获取jar文件的运行路径。
     * @return 返回jar文件的运行路径字符串。
     */
    @JvmStatic
    val jarRunPath: String
        get() {
            // 通过运行时管理接口获取当前运行的jar文件路径
            val path = ManagementFactory.getRuntimeMXBean().classPath.split(pathSeparator)[0]
            return path
        }


    @JvmStatic
    fun createKeyPID(): String {
        val attributes = mainAttributes
        return if (attributes != null) {
            attributes.getValue("Implementation-Title") + ".pid"
        } else "$jarName.pid"
    }

    /**
     * 使得程序只有一个在运行 pid
     */
    @JvmStatic
    fun uniquePID(): Boolean {
        val path = runPath.resolve(PID_FILE)
        try {
            val currentPID = pid
            if (path.exists()) {
                println("readFile ${path.pathString}")
                val pid = path.readText()
                if (pid != currentPID.toString() && isProcessRunning(pid)) {
                    println("此应用程序的另一个实例已在运行 (pid=$pid).")
                    killProcessId(pid)
                }
            }
            // Write the current PID to a file
            path.writeText(currentPID.toString())
        } catch (e: Exception) {
            logger.error("uniquePID", e)
            return false
        }
        return true
    }

    /**
     * 获取当前程序的pid
     */
    @JvmStatic
    val pid: Long
        get() = ProcessHandle.current().pid()

    /**
     * 判断进程是否存在
     * @param pid 进程id
     */
    private fun isProcessRunning(pid: String): Boolean {
        val process = if (System.getProperty("os.name").lowercase().contains("windows")) {
            Runtime.getRuntime().exec(arrayOf("tasklist", "/fi", "\"pid eq $pid\""))
        } else {
            Runtime.getRuntime().exec(arrayOf("ps", "-p", pid))
        }
        process.waitFor()
        return process.exitValue() == 0
    }


    /**
     * 获取用户的主目录路径。
     * @return 返回当前用户主目录的字符串路径。
     */
    val home: String
        get() = System.getProperty("user.home")

    /**
     * 获取当前Java进程的当前工作目录。
     * @return 返回当前工作目录的字符串路径。
     */
    val dir: String
        get() = System.getProperty("user.dir")

    /**
     * 获取Java安装目录路径。
     * @return 返回Java安装目录的字符串路径。
     */
    val javaHome: String
        get() = System.getProperty("java.home")

    /**
     * 获取系统指定的临时目录路径。
     * @return 返回系统指定临时目录的字符串路径。
     */
    val tempDir: String
        get() = System.getProperty("java.io.tmpdir")

    /**
     * 获取用户指定的时区。
     * @return 返回用户指定时区的字符串表示。
     */
    val timeZone: String
        get() = System.getProperty("user.timezone")

    /**
     * 获取与系统相关的路径分隔符。此字段初始化为包含系统属性 path.separator值的第一个字符。
     *
     * 此字符用于分隔作为 路径列表给出的文件序列中的文件名。
     *
     * 在 UNIX 系统上，此字符为 ':'
     *
     * 在 Microsoft Windows 系统上为 ';'
     * @return 返回一个字符串，代表系统文件路径的分隔符。
     */
    val pathSeparator: String
        get() = File.pathSeparator

    /**
     * 获取当前操作系统的名称。
     * @return 返回当前操作系统名称的字符串。
     */
    val osName: String
        get() = System.getProperty("os.name")

    /**
     * 获取当前操作系统的简写名称。
     * 通过查询系统属性"os.name"，并根据不同的操作系统关键字进行匹配，返回对应的操作系统简写。
     * 如果无法匹配到明确的操作系统类型，则默认返回"win"。
     *
     * @return 操作系统的简写名称，可能的值为"win"、"mac"、"linux"。
     */
    val os: String
        get() {
            val name = System.getProperty("os.name").lowercase()
            return when {
                name.contains("window") -> "win" // 检测是否为Windows系统
                name.contains("mac") -> "mac" // 检测是否为Mac系统
                name.contains("linux") -> "linux" // 检测是否为Linux系统
                else -> "win" // 默认为Windows
            }
        }


    /**
     * 判断当前系统是否为Windows系统。
     *
     * @return 如果当前系统为Windows系统，则返回true，否则返回false。
     */
    val isWin: Boolean
        get() = System.getProperty("os.name").lowercase().contains("window")

    /**
     * 判断当前系统是否为Mac系统。
     *
     * @return 如果当前系统为Mac系统，则返回true，否则返回false。
     */
    val isMac: Boolean
        get() = System.getProperty("os.name").lowercase().contains("mac")

    /**
     * 判断当前系统是否为Linux系统。
     *
     * @return 如果当前系统为Linux系统，则返回true，否则返回false。
     */
    val isLinux: Boolean
        get() = System.getProperty("os.name").lowercase().contains("linux")


}