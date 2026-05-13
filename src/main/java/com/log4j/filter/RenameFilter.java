package com.log4j.filter;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;

/**
 * 用来替换占位符的过滤器
 * 匹配成功就会替换成指定的值
 */
@Plugin(name = "RenameFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
public class RenameFilter extends AbstractFilter {

    private final String name;
    private final String value;

    public RenameFilter(String name, String value) {
        super();
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @PluginBuilderFactory
    public static RenameFilter.Builder newBuilder() {
        return new RenameFilter.Builder();
    }

    public static class Builder extends AbstractFilterBuilder<RenameFilter.Builder> implements org.apache.logging.log4j.core.util.Builder<RenameFilter> {

        @PluginBuilderAttribute
        private String name = "";
        @PluginBuilderAttribute
        private String value = "";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public RenameFilter build() {
            return new RenameFilter(name, value);
        }
    }

}
