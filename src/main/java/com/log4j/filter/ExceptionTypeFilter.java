package com.log4j.filter;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import java.util.Arrays;


/**
 * 根据错误事件类忽略
 *
 * <pre>
 * &lt;RollingFile name=&quot;mail&quot; fileName=&quot;info.log&quot; filePattern=&quot;info-%d{yyyy-MM-dd}-%i.log&quot;&gt;
 *      &lt;PatternLayout pattern=&quot;%d{yyyy-MM-dd HH:mm:ss,SSS} [%t] %-5p %c : %m%n&quot;/&gt;
 *      &lt;StringMatchFilter text=&quot;Connection reset by peer&quot; onMatch=&quot;DENY&quot; onMismatch=&quot;NEUTRAL&quot;/&gt;
 *      &lt;ExceptionTypeFilter onMatch=&quot;DENY&quot; onMismatch=&quot;NEUTRAL&quot;&gt;
 *          &lt;TypeValue value=&quot;java.net.UnknownHostException&quot;/&gt;
 *          &lt;TypeValue value=&quot;com.mysql.cj.jdbc.exceptions.CommunicationsException&quot;/&gt;
 *      &lt;/ExceptionTypeFilter&gt;
 * &lt;/RollingFile&gt;
 * </pre>
 */
@Plugin(name = "ExceptionTypeFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
public class ExceptionTypeFilter extends AbstractFilter {

    private final Class<?>[] exceptionTypes;

    public ExceptionTypeFilter(Result onMatch, Result onMismatch, Class<?>[] exceptionTypes) {
        super(onMatch, onMismatch);
        this.exceptionTypes = exceptionTypes;
    }

    @Override
    public Result filter(LogEvent event) {
        Throwable thrown = event.getThrown();
        if (thrown != null) {
            for (Class<?> exceptionType : exceptionTypes) {
                if (exceptionType.isAssignableFrom(thrown.getClass())) {
                    return Result.DENY;
                }
            }
        }
        return Result.NEUTRAL;
    }

    @PluginFactory
    public static ExceptionTypeFilter createFilter(
            @PluginElement("TypeValue") final TypeValue[] typeValues,
            @PluginAttribute("onMatch") final Result match,
            @PluginAttribute("onMismatch") final Result mismatch) {
        if (typeValues == null || typeValues.length == 0) {
            LOGGER.error("null");
            return null;
        }
        return new ExceptionTypeFilter(match, mismatch, Arrays.stream(typeValues).map((mapp) -> {
            try {
                LOGGER.debug("filter class：" + mapp.value);
                return Class.forName(mapp.getValue());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).toArray(Class[]::new));
    }


    @Plugin(name = "TypeValue", category = Node.CATEGORY, printObject = true)
    public static class TypeValue {

        private String value;

        public TypeValue(String key) {
            this.value = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @PluginBuilderFactory
        public static Builder newBuilder() {
            return new Builder();
        }

        public static class Builder implements org.apache.logging.log4j.core.util.Builder<TypeValue> {

            @PluginBuilderAttribute
            private String value;

            public void setValue(String value) {
                this.value = value;
            }

            @Override
            public TypeValue build() {
                return new TypeValue(value);
            }

        }

    }


}


