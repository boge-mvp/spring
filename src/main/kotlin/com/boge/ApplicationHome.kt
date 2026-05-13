package com.boge

import com.isNull
import java.net.JarURLConnection
import java.net.URL
import java.nio.file.Path
import java.util.*
import java.util.jar.JarFile
import java.util.jar.Manifest
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.toPath

/**
 * @see org.springframework.boot.system.ApplicationHome
 */
class ApplicationHome(sourceClass: Class<*>? = null) {

    companion object {

        private lateinit var applicationHome: ApplicationHome
        fun init(sourceClass: Class<*>? = null): ApplicationHome {
            if (!::applicationHome.isInitialized) applicationHome = ApplicationHome(sourceClass)
            return applicationHome
        }

    }

    /**
     * 返回用于查找主目录的基础源。这通常是 jar 文件或目录。如果无法确定源，则可以返回 null 。
     * @return 基础源或 `null`
     */
    val source: Path?

    /**
     * 返回应用程序主目录。
     * @return 主目录（从不 null）
     */
    val dir: Path

    init {
        this.source = findSource(sourceClass ?: startClass)
        this.dir = findHomeDir(this.source)
    }

    val startClass: Class<*>?
        get() {
            try {
                val classLoader = javaClass.classLoader
                return getStartClass(classLoader.getResources("META-INF/MANIFEST.MF"))
            } catch (ex: Exception) {
                return null
            }
        }

    fun getStartClass(manifestResources: Enumeration<URL>): Class<*>? {
        while (manifestResources.hasMoreElements()) {
            try {
                manifestResources.nextElement().openStream().use { inputStream ->
                    val manifest = Manifest(inputStream)
                    val startClass = manifest.mainAttributes.getValue("Start-Class").isNull {
                        manifest.mainAttributes.getValue("Main-Class")
                    }
                    if (startClass != null) {
                        return Class.forName(startClass, false, javaClass.classLoader)
                    }
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun findSource(sourceClass: Class<*>?): Path? {
        try {
            val domain = if ((sourceClass != null)) sourceClass.protectionDomain else null
            val codeSource = if ((domain != null)) domain.codeSource else null
            val location = if ((codeSource != null)) codeSource.location else null
            val source = if ((location != null)) findSource(location) else null
            if (source != null && source.exists() && !isUnitTest) {
                return source
            }
        } catch (_: Exception) {
        }
        return null
    }

    private val isUnitTest: Boolean
        get() {
            try {
                val stackTrace = Thread.currentThread().stackTrace
                for (i in stackTrace.indices.reversed()) {
                    if (stackTrace[i].className.startsWith("org.junit.")) {
                        return true
                    }
                }
            } catch (_: Exception) {
            }
            return false
        }

    fun findSource(location: URL): Path {
        val connection = location.openConnection()
        if (connection is JarURLConnection) {
            return getRootJarFile(connection.jarFile)
        }
        return location.toURI().toPath()
    }

    fun getRootJarFile(jarFile: JarFile): Path {
        var name = jarFile.name
        val separator = name.indexOf("!/")
        if (separator > 0) {
            name = name.substring(0, separator)
        }
        return Path(name)
    }

    fun findHomeDir(source: Path?): Path {
        var homeDir = source ?: findDefaultHomeDir()
        if (!homeDir.isDirectory()) homeDir = homeDir.parent
        homeDir = if (homeDir.exists()) homeDir else Path(".")
        return homeDir
    }

    fun findDefaultHomeDir(): Path {
        val userDir = System.getProperty("user.dir")
        return Path(if (userDir.isNullOrEmpty()) userDir else ".")
    }

    override fun toString(): String {
        return dir.toString()
    }

}
