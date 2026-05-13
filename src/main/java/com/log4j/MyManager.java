package com.log4j;

import jakarta.annotation.Nullable;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.ConfigurationFactoryData;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.*;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MyManager extends RollingFileManager {

    private static final MyManagerFactory factory = new MyManagerFactory();
    private static final FileTime EPOCH = FileTime.fromMillis(0);
    private FactoryData data;

    private final ThreadLocal<LoggerConfig> threadLocal = new ThreadLocal<>();
    private final ThreadLocal<AbstractAppender> appenderThreadLocal = new ThreadLocal<>();
    /**
     * key: LoggerConfig.name
     * value: LoggerConfig
     */
    ThreadLocal<Map<LoggerConfig, FileConfig>> mapThreadLocal = ThreadLocal.withInitial(HashMap::new);

    public MyManager(LoggerContext loggerContext, String fileName, String pattern, OutputStream os, boolean append, boolean createOnDemand, long size, long initialTime, TriggeringPolicy triggeringPolicy, RolloverStrategy rolloverStrategy, String advertiseURI, Layout<? extends Serializable> layout, String filePermissions, String fileOwner, String fileGroup, boolean writeHeader, ByteBuffer buffer) {
        super(loggerContext, fileName, pattern, os, append, createOnDemand, size, initialTime, triggeringPolicy, rolloverStrategy, advertiseURI, layout, filePermissions, fileOwner, fileGroup, writeHeader, buffer);
    }

    public MyManager(FactoryData data, OutputStream os, long size, long initialTime, boolean writeHeader, ByteBuffer buffer) {
        this(data.getLoggerContext(), data.fileName, data.pattern, os,
                data.append, data.createOnDemand, size, initialTime, data.getTriggeringPolicy(), data.getRolloverStrategy(),
                data.advertiseURI, data.layout, data.filePermissions, data.fileOwner, data.fileGroup, writeHeader, buffer);
        if (data.createOnDemand) {
            if (data.policy instanceof OnStartupTriggeringPolicy) {
                LoggerFactory.getLogger(MyManager.class).warn("createOnDemand=true 使用 OnStartupTriggeringPolicy 不能生效，改用 OnStartupPolicy");
            } else if (data.policy instanceof CompositeTriggeringPolicy) {
                List<TriggeringPolicy> triggeringPolicies = Arrays.stream(((CompositeTriggeringPolicy) data.policy).getTriggeringPolicies()).filter(p -> p instanceof OnStartupTriggeringPolicy).toList();
                if (!triggeringPolicies.isEmpty()) {
                    LoggerFactory.getLogger(MyManager.class).warn("createOnDemand=true 使用 OnStartupTriggeringPolicy 不能生效，改用 OnStartupPolicy");
                }
            }
        }
        this.data = data;
    }

    @Override
    public long getFileTime() {
        FileConfig config = getFileConfig();
        return config == null ? 0 : config.initialTime;
    }

    @Override
    public String getFileName() {
        LoggerConfig loggerConfig = getThreadLoggerConfig();
        FileConfig config = mapThreadLocal.get().get(loggerConfig);
        String fileName = data.fileName;
        if (config != null) {
            if (config.fileName != null) {
                LOGGER.debug("get the file name from thread local：" + config.fileName);
                setPatternProcessor(new PatternProcessor(config.pattern, this.getPatternProcessor()));
                return config.fileName;
            }
            String p = data.pattern;
            if (config.propertyList != null && (fileName.contains("${") || p.contains("${"))) {
                for (Property it : config.propertyList) {
                    fileName = fileName.replace("${" + it.getName() + "}", it.getValue());
                    p = p.replace("${" + it.getName() + "}", it.getValue());
                }
                config.fileName = fileName;
                config.pattern = p;
                setPatternProcessor(new PatternProcessor(p, this.getPatternProcessor()));
            }
            LOGGER.debug("fileName: " + fileName + " pattern: " + p);
        }
        return fileName;
    }

    @Override
    public synchronized void checkRollover(LogEvent event) {
        initProperty();
        super.checkRollover(event);
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        LoggerConfig loggerConfig = getThreadLoggerConfig();
        FileConfig config = getOrPut(loggerConfig, () -> new FileConfig(loggerConfig));
        OutputStream out = config.outputStream;
        if (out == null) {
            out = createOutputStream();
            config.outputStream = out;
        }
        return out;

    }

    @Override
    protected OutputStream createOutputStream() throws IOException {
//        LOGGER.debug(appender.getName() + "  " + getFileName() + " " + getPatternProcessor().getPattern());
        initProperty();
        return super.createOutputStream();
    }

    private void initProperty() {
        LoggerConfig loggerConfig = getThreadLoggerConfig();
        FileConfig config = getOrPut(loggerConfig, () -> new FileConfig(loggerConfig));
        if (config.propertyList == null || config.propertyList.isEmpty()) {
            Collection<LoggerConfig> values = threadLocal.get() != null ? List.of(threadLocal.get()) : data.configuration.getLoggers().values();
            if (!values.isEmpty()) {
                String appenderName = appenderThreadLocal.get() != null ? appenderThreadLocal.get().getName() : loggerConfig.getName();
                config.propertyList = values.stream()
                        .filter(
                                it -> it.getAppenderRefs().stream().anyMatch(v -> Objects.equals(v.getRef(), appenderName))
                        ).flatMap(mapper -> mapper.getPropertyList().stream()).collect(Collectors.toList());

                // 第一次初始化 初始化部分基础信息
                File path = Path.of(this.getFileName()).toFile();
                if (path.exists()) {
                    if (data.append) size = config.size = path.length();
                    config.initialTime = initialFileTime(path);
                    config.writeHeader = path.length() == 0L;
                    getPatternProcessor().setPrevFileTime(config.initialTime);
//                    if (writeHeader) {
//                        writeHeader();
//                    }
                    LOGGER.debug("初始化文件信息： $size $fileInitialTime $writeHeader");
                }
            }
        }
    }

    /**
     * 获取当前线程 解析的FileConfig信息
     */
    @Nullable
    protected FileConfig getFileConfig() {
        return mapThreadLocal.get().get(getThreadLoggerConfig());
    }

    /**
     * 获取当前线程正在使用的 LoggerConfig
     * @return LoggerConfig
     */
    @NotNull
    protected LoggerConfig getThreadLoggerConfig() {
        return threadLocal.get() == null ? this.getLoggerContext().getRootLogger().get() : threadLocal.get();
    }

    @NotNull
    protected FileConfig getOrPut(LoggerConfig key, Supplier<? extends FileConfig> defaultValue) {
        FileConfig config = mapThreadLocal.get().get(key);
        if (config == null) {
            config = defaultValue.get();
            mapThreadLocal.get().put(key, config);
        }
        return config;
    }

    @Override
    public void updateData(Object data) {
        final FactoryData factoryData = (FactoryData) data;
        setRolloverStrategy(factoryData.getRolloverStrategy());
        setPatternProcessor(new PatternProcessor(factoryData.getPattern(), getPatternProcessor()));
        setTriggeringPolicy(factoryData.getTriggeringPolicy());
    }

    public static MyManager getFileManager(final String fileName, final String pattern, final boolean append,
                                           final boolean bufferedIO, final TriggeringPolicy policy, final RolloverStrategy strategy,
                                           final String advertiseURI, final Layout<? extends Serializable> layout, final int bufferSize,
                                           final boolean immediateFlush, final boolean createOnDemand,
                                           final String filePermissions, final String fileOwner, final String fileGroup,
                                           final Configuration configuration) {

        if (strategy instanceof DirectWriteRolloverStrategy && fileName != null) {
            LOGGER.error("The fileName attribute must not be specified with the DirectWriteRolloverStrategy");
            return null;
        }
        final String name = fileName == null ? pattern : fileName;
        // 名字设置一下  防止被复制对象的时候  复用了MyManager
        return narrow(MyManager.class, getManager("MyManager_" + name, new FactoryData(fileName, pattern, append,
                bufferedIO, policy, strategy, advertiseURI, layout, bufferSize, immediateFlush, createOnDemand,
                filePermissions, fileOwner, fileGroup, configuration), factory));
    }

    @Override
    protected synchronized void write(byte[] bytes, int offset, int length, boolean immediateFlush) {
        super.write(bytes, offset, length, immediateFlush);
        threadLocal.remove();
    }

    public void write(final byte[] bytes, final boolean immediateFlush)  {
        super.write(bytes, 0, bytes.length, immediateFlush);
    }

    public MyManager addLoggerConfig(LoggerConfig loggerConfig, AbstractAppender appender) {
        threadLocal.set(loggerConfig);
        if (appender != null) appenderThreadLocal.set(appender);
        return this;
    }

    /**
     * Factory to create a RollingFileManager.
     */
    private static class MyManagerFactory implements ManagerFactory<MyManager, FactoryData> {

        /**
         * Creates a RollingFileManager.
         *
         * @param name The name of the entity to manage.
         * @param data The data required to create the entity.
         * @return a RollingFileManager.
         */
        @Override
        public MyManager createManager(final String name, final FactoryData data) {
            long size = 0;
            File file = null;
            if (data.fileName != null) {
                file = new File(data.fileName);

                try {
                    FileUtils.makeParentDirs(file);
                    final boolean created = !data.createOnDemand && file.createNewFile();
                    LOGGER.trace("New file '{}' created = {}", name, created);
                } catch (final IOException ioe) {
                    LOGGER.error("Unable to create file " + name, ioe);
                    return null;
                }
                size = data.append ? file.length() : 0;
            }

            try {
                final int actualSize = data.bufferedIO ? data.bufferSize : Constants.ENCODER_BYTE_BUFFER_SIZE;
                final ByteBuffer buffer = ByteBuffer.wrap(new byte[actualSize]);
                final OutputStream os = data.createOnDemand || data.fileName == null ? null :
                        new FileOutputStream(data.fileName, data.append);
                // LOG4J2-531 create file first so time has valid value.
                final long initialTime = file == null || !file.exists() ? 0 : initialFileTime(file);
                final boolean writeHeader = file != null && file.exists() && file.length() == 0;

                final MyManager rm = new MyManager(data, os, size, initialTime, writeHeader, buffer);
                if (os != null && rm.isAttributeViewEnabled()) {
                    rm.defineAttributeView(file.toPath());
                }
                return rm;
            } catch (final IOException ex) {
                LOGGER.error("RollingFileManager (" + name + ") " + ex, ex);
            }
            return null;
        }
    }

    private static long initialFileTime(final File file) {
        final Path path = file.toPath();
        if (Files.exists(path)) {
            try {
                final BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                final FileTime fileTime = attrs.creationTime();
                if (fileTime.compareTo(EPOCH) > 0) {
                    LOGGER.debug("Returning file creation time for {}", file.getAbsolutePath());
                    return fileTime.toMillis();
                }
                LOGGER.info("Unable to obtain file creation time for " + file.getAbsolutePath());
            } catch (final Exception ex) {
                LOGGER.info("Unable to calculate file creation time for " + file.getAbsolutePath() + ": " + ex.getMessage());
            }
        }
        return file.lastModified();
    }


    /**
     * Factory data.
     */
    public static class FactoryData extends ConfigurationFactoryData {
        private final String fileName;
        private final String pattern;
        private final boolean append;
        private final boolean bufferedIO;
        private final int bufferSize;
        private final boolean immediateFlush;
        private final boolean createOnDemand;
        private final TriggeringPolicy policy;
        private final RolloverStrategy strategy;
        private final String advertiseURI;
        private final Layout<? extends Serializable> layout;
        private final String filePermissions;
        private final String fileOwner;
        private final String fileGroup;

        /**
         * Creates the data for the factory.
         *
         * @param pattern         The pattern.
         * @param append          The append flag.
         * @param bufferedIO      The bufferedIO flag.
         * @param advertiseURI
         * @param layout          The Layout.
         * @param bufferSize      the buffer size
         * @param immediateFlush  flush on every write or not
         * @param createOnDemand  true if you want to lazy-create the file (a.k.a. on-demand.)
         * @param filePermissions File permissions
         * @param fileOwner       File owner
         * @param fileGroup       File group
         * @param configuration   The configuration
         */
        public FactoryData(final String fileName, final String pattern, final boolean append, final boolean bufferedIO,
                           final TriggeringPolicy policy, final RolloverStrategy strategy, final String advertiseURI,
                           final Layout<? extends Serializable> layout, final int bufferSize, final boolean immediateFlush,
                           final boolean createOnDemand, final String filePermissions, final String fileOwner, final String fileGroup,
                           final Configuration configuration) {
            super(configuration);
            this.fileName = fileName;
            this.pattern = pattern;
            this.append = append;
            this.bufferedIO = bufferedIO;
            this.bufferSize = bufferSize;
            this.policy = policy;
            this.strategy = strategy;
            this.advertiseURI = advertiseURI;
            this.layout = layout;
            this.immediateFlush = immediateFlush;
            this.createOnDemand = createOnDemand;
            this.filePermissions = filePermissions;
            this.fileOwner = fileOwner;
            this.fileGroup = fileGroup;
        }

        public TriggeringPolicy getTriggeringPolicy() {
            return this.policy;
        }

        public RolloverStrategy getRolloverStrategy() {
            return this.strategy;
        }

        public String getPattern() {
            return pattern;
        }

        @Override
        public String toString() {
            return super.toString() +
                    "[pattern=" +
                    pattern +
                    ", append=" +
                    append +
                    ", bufferedIO=" +
                    bufferedIO +
                    ", bufferSize=" +
                    bufferSize +
                    ", policy=" +
                    policy +
                    ", strategy=" +
                    strategy +
                    ", advertiseURI=" +
                    advertiseURI +
                    ", layout=" +
                    layout +
                    ", filePermissions=" +
                    filePermissions +
                    ", fileOwner=" +
                    fileOwner +
                    "]";
        }
    }


    public static class FileConfig {

        /**
         * 未使用 自定义的logger会用null
         */
        final LoggerConfig loggerConfig;
        OutputStream outputStream;
        List<Property> propertyList;
        String fileName;
        String pattern;
        long size;
        long initialTime;
        boolean writeHeader;

        public FileConfig(LoggerConfig loggerConfig) {
            this.loggerConfig = loggerConfig;
        }

        public FileConfig(LoggerConfig loggerConfig, OutputStream outputStream, List<Property> propertyList) {
            this.loggerConfig = loggerConfig;
            this.outputStream = outputStream;
            this.propertyList = propertyList;
        }
    }

}
