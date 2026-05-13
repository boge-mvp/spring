package com.log4j.filter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

/**
 * 根据包名过滤
 * <p>例子：<p/>
 * <pre>
 * &lt;Console name=&quot;Console&quot; target=&quot;SYSTEM_OUT&quot;&gt;
 *     &lt;PatternLayout pattern=&quot;%d{yyyy-MM-dd HH:mm:ss,SSS} [%t] %-5p %c : %m%n&quot;/&gt;
 *     &lt;Filters&gt;
 *         &lt;PackageFilter packageName=&quot;com.socket.&quot; onMatch=&quot;DENY&quot; onMismatch=&quot;NEUTRAL&quot;/&gt;
 *     &lt;/Filters&gt;
 * &lt;/Console&gt;
 * <pre/>
 */
@Plugin(name = "PackageFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
public class PackageFilter extends AbstractFilter {

    private final String packageName;

    public PackageFilter(String packageName, Result onMatch, Result onMismatch) {
        super(onMatch, onMismatch);
        this.packageName = packageName;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return super.filter(logger, level, marker, null);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return super.filter(logger, level, marker, null);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        if (logger.getName().startsWith(packageName)) {
            return onMatch;
        } else return onMismatch;
    }

    @Override
    public Result filter(LogEvent event) {
        if (event.getLoggerName().startsWith(packageName)) {
            return onMatch;
        } else return onMismatch;
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AbstractFilterBuilder<Builder> implements org.apache.logging.log4j.core.util.Builder<PackageFilter> {

        @PluginBuilderAttribute
        private String packageName = "";

        @Override
        public PackageFilter build() {
            return new PackageFilter(packageName, getOnMatch(), getOnMismatch());
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }
    }

}
