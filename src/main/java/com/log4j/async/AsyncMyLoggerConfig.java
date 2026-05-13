package com.log4j.async;

import com.boge.utils.BeanUtils;
import com.boge.utils.Log4jKit;
import com.log4j.config.MyAppenderControl;
import com.log4j.config.MyLoggerConfig;
import com.log4j.filter.LoggerFilter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.async.AsyncLoggerConfig;
import org.apache.logging.log4j.core.config.*;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.filter.Filterable;
import org.apache.logging.log4j.util.Strings;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Plugin(name = "asyncMyLogger", category = Node.CATEGORY, printObject = true)
public class AsyncMyLoggerConfig extends AsyncLoggerConfig {

    private final Configuration config;

    @PluginBuilderFactory
    public static <B extends AsyncMyLoggerConfig.Builder<B>> B newAsyncBuilder2() {
        return new AsyncMyLoggerConfig.Builder<B>().asBuilder();
    }

    public static class Builder<B extends AsyncMyLoggerConfig.Builder<B>> extends MyLoggerConfig.Builder<B> {
        @Override
        public LoggerConfig build() {
            final String name = getLoggerName().equals(ROOT) ? Strings.EMPTY : getLoggerName();
            LevelAndRefs container = LoggerConfig.getLevelAndRefs(getLevel(), getRefs(), getLevelAndRefs(),
                    getConfig());
            return new AsyncMyLoggerConfig(name, ignoreSingle, Boolean.parseBoolean(single), loggerFilter, container.refs, getFilter(), container.level, isAdditivity(),
                    getProperties(), getConfig(), includeLocation(getIncludeLocation()));
        }
    }

    protected Method logToAsyncDelegate;
    protected Method allowMethod;
    protected ThreadLocal<Boolean> ASYNC_LOGGER_ENTERED2;
    protected AppenderControlArraySet appenderControlArraySet;
    /**
     * logger给所有appender添加的过滤器
     */
    public LoggerFilter loggerFilter;
    /**
     * 是否启动单独处理 开启后当事件被首次消费后，立即结束，后续监听将抛弃这条事件 Console除外
     */
    private final Boolean single;

    @PluginBuilderAttribute
    private final String ignoreSingle;

    protected AsyncMyLoggerConfig(String name, String ignoreSingle, Boolean single, LoggerFilter loggerFilter, List<AppenderRef> appenders, Filter filter, Level level, boolean additive, Property[] properties, Configuration config, boolean includeLocation) {
        super(name, appenders, filter, level, additive, properties, config, includeLocation);
        LOGGER.debug("createLogger--:" + name);
        this.ignoreSingle = ignoreSingle;
        this.config = config;
        this.single = single;
        try {
            this.loggerFilter = loggerFilter;
            logToAsyncDelegate = BeanUtils.getMethod(this.getClass().getSuperclass(), "logToAsyncDelegate", true, LogEvent.class);

            ASYNC_LOGGER_ENTERED2 = BeanUtils.getField(this, "ASYNC_LOGGER_ENTERED", true);

            allowMethod = LoggerConfigPredicate.class.getDeclaredMethod("allow", LoggerConfig.class);
            allowMethod.setAccessible(true);

            this.appenderControlArraySet = BeanUtils.getField(this, "appenders", true);
        } catch (Exception e) {
            LOGGER.error("get logToAsyncDelegate", e);
        }
    }

    @Override
    protected void log(LogEvent event, LoggerConfigPredicate predicate) {
        if (predicate == LoggerConfigPredicate.ALL && ASYNC_LOGGER_ENTERED2.get() == Boolean.FALSE &&
                // Optimization: AsyncLoggerConfig is identical to LoggerConfig
                // when no appenders are present. Avoid splitting for synchronous
                // and asynchronous execution paths until encountering an
                // AsyncLoggerConfig with appenders.
                hasAppenders()) {
            // This is the first AsnycLoggerConfig encountered by this LogEvent
            ASYNC_LOGGER_ENTERED2.set(Boolean.TRUE);
            try {
                // Detect the first time we encounter an AsyncLoggerConfig. We must log
                // to all non-async loggers first.
                superLog(event, LoggerConfigPredicate.SYNCHRONOUS_ONLY);
                // Then pass the event to the background thread where
                // all async logging is executed. It is important this
                // happens at most once and after all synchronous loggers
                // have been invoked, because we lose parameter references
                // from reusable messages.

                logToAsyncDelegate.invoke(this, event);
            } catch (Exception e) {
                LOGGER.error("AsyncMyLoggerConfig.log", e);
            } finally {
                ASYNC_LOGGER_ENTERED2.set(Boolean.FALSE);
            }
        } else {
            superLog(event, predicate);
        }
    }

    protected void superLog(LogEvent event, LoggerConfigPredicate predicate) {
        if (isFiltered(event)) { // 不处理这个事件  直接传递给父事件
            logParent(event, predicate);
        } else {// 要亲自处理
            processLogEvent(event, predicate);
        }
    }

    boolean runMethod(LoggerConfigPredicate predicate) {
        try {
            return (boolean) allowMethod.invoke(predicate, this);
        } catch (Exception ignored) {
        }
        return false;
    }

    protected void processLogEvent(final LogEvent event, final LoggerConfigPredicate predicate) {
        event.setIncludeLocation(isIncludeLocation());
        if (runMethod(predicate)) {
            callAppenders(event);
        }
        logParent(event, predicate);
    }

    /**
     * @param event     事件
     * @param predicate
     */
    public void logParent(final LogEvent event, final LoggerConfigPredicate predicate) {
        MyLoggerConfig.parentLog(this, event, predicate);
    }

    /**
     * 查询事件是否没有被处理<br/>
     *
     * @param event 日志
     * @return true表示没有任何Appender去处理， false表示已被处理
     */
    public boolean isAppenderFiltered(LogEvent event) {
        boolean result = true;
        try {
            AppenderControl[] appenderControls = appenderControlArraySet.get();
            if (appenderControls.length > 0) {
                boolean r = loggerFilter != null && loggerFilter.filter(event) == Filter.Result.DENY;
                if (r) return true;
                // 验证所有的AppenderControl既appender-ref 是否也被过滤
                r = Arrays.stream(appenderControls).anyMatch(p -> {
                    if (p instanceof MyAppenderControl) {
                        return ((MyAppenderControl) p).shouldSkip(event);
                    } else {
                        try {
                            Method shouldSkipMethod = BeanUtils.getMethod(p.getClass(), "shouldSkip", true, LogEvent.class);
                            if (shouldSkipMethod != null) {
                                return (boolean) shouldSkipMethod.invoke(p, event);
                            }
                        } catch (Exception ignored) {
                        }
                        return false;
                    }
                });
                if (r) return true;
                // 最后验证所有Appender自身的过滤器是否有过滤
                Collection<Appender> values = getAppenders().values();
                if (!values.isEmpty()) {
                    result = values.stream().anyMatch(p -> {
                        if (p instanceof Filterable)
                            return ((Filterable) p).isFiltered(event);
                        else return false;
                    });
                }
            }
            LOGGER.trace("isAppenderFiltered: " + result);
        } catch (Exception e) {
            LOGGER.error("isAppenderFiltered", e);
        }
        return result;
    }

    @Override
    public void addAppender(Appender appender, Level level, Filter filter) {
//        super.addAppender(appender, level, filter);
        appender = Log4jKit.convers(this, appender, filter);
        appenderControlArraySet.add(new MyAppenderControl(appender, level, filter));
    }

    @Override
    protected void callAppenders(LogEvent event) {
        AppenderControl[] controls = appenderControlArraySet.get();
        if (single && ignoreSingle != null) {
            List<String> singles = Arrays.stream(ignoreSingle.split(",")).map(String::toLowerCase).toList();
            controls = Arrays.stream(controls).sorted((o1, o2) -> {
                boolean a = singles.contains(o1.getAppenderName().toLowerCase());
                boolean b = singles.contains(o2.getAppenderName().toLowerCase());
                if (a && !b) {
                    return -1;
                }
                if (!a && b) {
                    return 1;
                }
                return 0;
            }).toArray(AppenderControl[]::new);
        }
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < controls.length; i++) {
            MyAppenderControl control = (MyAppenderControl) controls[i];
            if (single) {
                if (control.callAppender(event, this)) {
                    if (ignoreSingle != null) {
                        String[] singles = ignoreSingle.split(",");
                        if(Arrays.stream(singles).filter(v -> v.equalsIgnoreCase(control.getAppenderName())).toList().isEmpty()) {
                            break;
                        }
                    } else break;
                }
            } else control.callAppender(event, this);
        }
    }


    @Plugin(name = "asyncRoot", category = Core.CATEGORY_NAME, printObject = true)
    public static class AsyncMyRootLogger extends MyLoggerConfig {

        public AsyncMyRootLogger(String name, String ignoreSingle, Boolean single, LoggerFilter loggerFilter, List<AppenderRef> appenders, Filter filter, Level level, boolean additive, Property[] properties, Configuration config, boolean includeLocation) {
            super(name, ignoreSingle, single, loggerFilter, appenders, filter, level, additive, properties, config, includeLocation);
        }

        @PluginBuilderFactory
        public static <B extends AsyncMyRootLogger.Builder<B>> B newAsyncRootBuilder2() {
            return new AsyncMyRootLogger.Builder<B>().asBuilder();
        }

        @Override
        protected void callAppenders(LogEvent event) {
            super.callAppenders(event);
        }

        public static class Builder<B extends AsyncMyRootLogger.Builder<B>>
                implements org.apache.logging.log4j.core.util.Builder<MyLoggerConfig> {

            @PluginBuilderAttribute
            protected boolean additivity;
            @PluginBuilderAttribute
            protected Level level;
            @PluginBuilderAttribute
            protected String levelAndRefs;
            @PluginBuilderAttribute
            protected String includeLocation;
            @PluginElement("AppenderRef")
            protected AppenderRef[] refs;
            @PluginElement("Properties")
            protected Property[] properties;
            @PluginConfiguration
            protected Configuration config;
            @PluginElement("Filter")
            protected Filter filter;
            /**
             * 是否启动单独处理 开启后当事件被首次消费后，立即结束，后续监听将抛弃这条事件
             */
            @PluginBuilderAttribute
            protected String single;

            @PluginBuilderAttribute
            protected String ignoreSingle;

            @Override
            public AsyncMyRootLogger build() {
                final LevelAndRefs container = LoggerConfig.getLevelAndRefs(level, refs, levelAndRefs, config);
                return new AsyncMyRootLogger(LogManager.ROOT_LOGGER_NAME, ignoreSingle, Boolean.parseBoolean(single), null,
                        container.refs, filter, container.level,
                        additivity, properties, config,
                        includeLocation(includeLocation, config));
            }

            @SuppressWarnings("unchecked")
            public B asBuilder() {
                return (B) this;
            }

        }

    }

}
