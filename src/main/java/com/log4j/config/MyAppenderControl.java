package com.log4j.config;

import com.log4j.appender.MyAppender;
import com.log4j.async.AsyncMyLoggerConfig;
import com.log4j.filter.LoggerFilter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.Filterable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MyAppenderControl extends AppenderControl {
    protected ThreadLocal<AppenderControl> recursive;
    protected Method shouldSkipMethod;
    protected Method handleError;
    protected Method handleAppenderError;

    /**
     * Constructor.
     *
     * @param appender The target Appender.
     * @param level    the Level to filter on.
     * @param filter   the Filter(s) to apply.
     */
    public MyAppenderControl(Appender appender, Level level, Filter filter) {
        super(appender, level, filter);
        try {
            shouldSkipMethod = this.getClass().getSuperclass().getDeclaredMethod("shouldSkip", LogEvent.class);
            shouldSkipMethod.setAccessible(true);

            handleError = this.getClass().getSuperclass().getDeclaredMethod("handleError", String.class);
            handleError.setAccessible(true);

            handleAppenderError = this.getClass().getSuperclass().getDeclaredMethod("handleAppenderError", LogEvent.class, RuntimeException.class);
            handleAppenderError.setAccessible(true);

            Field recursive = this.getClass().getSuperclass().getDeclaredField("recursive");
            recursive.setAccessible(true);
            this.recursive = (ThreadLocal<AppenderControl>) recursive.get(this);
        } catch (Exception e) {
            LOGGER.error("MyLoggerConfig", e);
        }
    }

    /**
     * 判断是否应该跳过给定的日志事件。
     *
     * @param event 要判断的日志事件
     * @return 如果都没有处理这个事件 则返回true 否则 false
     */
    public boolean shouldSkip(final LogEvent event) {
        try {
            return (boolean) shouldSkipMethod.invoke(this, event);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean callAppender(LogEvent event, LoggerConfig loggerConfig) {
//        super.callAppender(event);
        try {
            if (shouldSkip(event)) {
                return false;
            }
            return callAppenderPreventRecursion(event, loggerConfig);
        } catch (Exception e) {
            LOGGER.error("callAppender", e);
        }
        return false;
    }

    private boolean callAppenderPreventRecursion(final LogEvent event, LoggerConfig loggerConfig) throws InvocationTargetException, IllegalAccessException {
        try {
            recursive.set(this);
            return callAppender0(event, loggerConfig);
        } finally {
            recursive.remove();
        }
    }

    private boolean callAppender0(final LogEvent event, LoggerConfig loggerConfig) throws InvocationTargetException, IllegalAccessException {
        ensureAppenderStarted();
        if (!isFilteredByAppender(event, loggerConfig)) {
            tryCallAppender(event, loggerConfig);
            return true;
        }
        return false;
    }

    private void tryCallAppender(final LogEvent event, LoggerConfig loggerConfig) throws InvocationTargetException, IllegalAccessException {
        try {
            if (getAppender() instanceof MyAppender) {
                ((MyAppender) getAppender()).append(event, loggerConfig);
            } else {
                getAppender().append(event);
            }
        } catch (final RuntimeException error) {
            handleAppenderError.invoke(this, event, error);
        } catch (final Throwable throwable) {
            handleAppenderError.invoke(this, event, new AppenderLoggingException(throwable));
        }
    }

    /**
     *
     * @param event
     * @param loggerConfig
     * @return 当没有一个来处理事件 返回true否则false
     */
    public boolean isFilteredByAppender(final LogEvent event, LoggerConfig loggerConfig) {
        LoggerFilter loggerFilter = null;
        if (loggerConfig instanceof AsyncMyLoggerConfig) {
            loggerFilter = ((AsyncMyLoggerConfig) loggerConfig).loggerFilter;
        } else if (loggerConfig instanceof MyLoggerConfig) {
            loggerFilter = ((MyLoggerConfig) loggerConfig).loggerFilter;
        }
        boolean r = loggerFilter != null && loggerFilter.filter(event) == Filter.Result.DENY;
        if (r) {
            return true;
        }
        return getAppender() instanceof Filterable && ((Filterable) getAppender()).isFiltered(event);
    }

    private void ensureAppenderStarted() throws InvocationTargetException, IllegalAccessException {
        if (!this.getAppender().isStarted()) {
            handleError.invoke(this, "Attempted to append to non-started appender ");
        }
    }


}
