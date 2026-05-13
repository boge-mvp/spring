package com.log4j.config;

import com.boge.utils.BeanUtils;
import com.boge.utils.Log4jKit;
import com.log4j.filter.LoggerFilter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.*;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.filter.Filterable;
import org.apache.logging.log4j.util.Strings;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 本logger的不一样之初在于:<br/>
 * 当设置additivity=false 的时候 如果所有的Appender中没有一个有处理logEvent(也就是说都被各自的Filter拒绝了)，logEvent日志会继续像父节点传递 而不会抛弃
 * <p>
 * 只要自己未处理的  都要传递给 父Logger
 * </p>
 */
@Plugin(name = "MyLogger", category = Node.CATEGORY, printObject = true)
public class MyLoggerConfig extends LoggerConfig {

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newAsyncBuilder2() {
        return new Builder<B>().asBuilder();
    }

    public static class Builder<B extends Builder<B>> extends LoggerConfig.Builder<B> {

        /**
         * logger给所有appender添加的过滤器
         */
        @PluginElement("LoggerFilter")
        protected LoggerFilter loggerFilter;
        /**
         * 是否启动单独处理 开启后当事件被首次消费后，立即结束，后续监听将抛弃这条事件
         */
        @PluginBuilderAttribute
        protected String single;

        @PluginBuilderAttribute
        protected String ignoreSingle;

        @Override
        public LoggerConfig build() {
            final String name = getLoggerName().equals(ROOT) ? Strings.EMPTY : getLoggerName();
            LevelAndRefs container = LoggerConfig.getLevelAndRefs(getLevel(), getRefs(), getLevelAndRefs(),
                    getConfig());
            return new MyLoggerConfig(name, ignoreSingle, Boolean.parseBoolean(single), loggerFilter, container.refs, getFilter(), container.level, isAdditivity(),
                    getProperties(), getConfig(), includeLocation(getIncludeLocation()));
        }
    }

    /**
     * logger给所有appender添加的过滤器
     */
    public LoggerFilter loggerFilter;
    /**
     * 是否启动单独处理 开启后当事件被首次消费后，立即结束，后续监听将抛弃这条事件 Console除外
     */
    private final Boolean single;
    @PluginBuilderAttribute
    protected String ignoreSingle;
    protected Method allowMethod;
    protected AppenderControlArraySet appenderControlArraySet;

    public MyLoggerConfig(String name, String ignoreSingle, Boolean single, LoggerFilter loggerFilter, List<AppenderRef> appenders, Filter filter, Level level, boolean additive, Property[] properties, Configuration config, boolean includeLocation) {
        super(name, appenders, filter, level, additive, properties, config, includeLocation);
        this.ignoreSingle = ignoreSingle;
        this.loggerFilter = loggerFilter;
        this.single = single;
        try {
            allowMethod = BeanUtils.getMethod(LoggerConfigPredicate.class, "allow", true, LoggerConfig.class);

            this.appenderControlArraySet = BeanUtils.getField(this, "appenders", true);
        } catch (Exception e) {
            LOGGER.error("MyLoggerConfig", e);
        }
    }

    @Override
    protected void log(LogEvent event, LoggerConfigPredicate predicate) {
//        super.log(event, predicate);
        if (isFiltered(event)) {
            logParent(event, predicate);
        } else {
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

    public void logParent(final LogEvent event, final LoggerConfigPredicate predicate) {
        parentLog(this, event, predicate);
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
                        if (Arrays.stream(singles).filter(v -> v.equalsIgnoreCase(control.getAppenderName())).toList().isEmpty()) {
                            break;
                        }
                    } else break;
                }
            } else control.callAppender(event, this);
        }
    }

    public static void parentLog(LoggerConfig loggerConfig, LogEvent event, LoggerConfigPredicate predicate) {
        LoggerConfig parent = loggerConfig.getParent();
        if (parent != null) {
            if ((loggerConfig.isAdditive() || Log4jKit.isAppenderFiltered(loggerConfig, event))) {
                try {
                    Method parentLog = BeanUtils.getMethod(parent.getClass(), "log", true, LogEvent.class, LoggerConfigPredicate.class);
                    if (parentLog != null) {
                        parentLog.invoke(parent, event, predicate);
                    }
                } catch (Exception e) {
                    LOGGER.error("logParent error", e);
                }
            }
//            else {
//                if (parent.getParent() != null) {
//                    if (parent instanceof AsyncMyLoggerConfig) {
//                        ((AsyncMyLoggerConfig) parent).logParent(event, predicate);
//                    } else if (parent instanceof MyLoggerConfig) {
//                        ((MyLoggerConfig) parent).logParent(event, predicate);
//                    } else {
//                        parentLog(parent, event, predicate);
//                    }
//                }
//            }
        }
    }


    @Plugin(name = ROOT, category = Core.CATEGORY_NAME, printObject = true)
    public static class MyRootLogger extends MyLoggerConfig {

        public MyRootLogger(String name, String ignoreSingle, Boolean single, LoggerFilter loggerFilter, List<AppenderRef> appenders, Filter filter, Level level, boolean additive, Property[] properties, Configuration config, boolean includeLocation) {
            super(name, ignoreSingle, single, loggerFilter, appenders, filter, level, additive, properties, config, includeLocation);
        }

        @PluginBuilderFactory
        public static <B extends Builder<B>> B newRootBuilder2() {
            return new Builder<B>().asBuilder();
        }


        @Override
        protected void callAppenders(LogEvent event) {
            super.callAppenders(event);
        }

        /**
         * Builds LoggerConfig instances.
         *
         * @param <B> The type to build
         */
        public static class Builder<B extends Builder<B>>
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
            public MyRootLogger build() {
                final LevelAndRefs container = LoggerConfig.getLevelAndRefs(level, refs, levelAndRefs, config);
                return new MyRootLogger(LogManager.ROOT_LOGGER_NAME, ignoreSingle, Boolean.parseBoolean(single), null,
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
