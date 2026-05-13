package com.log4j.appender;

import com.Consts;
import com.log4j.interfaces.IEmailService;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.HtmlLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Plugin(name = "EmailAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class EmailAppender extends AbstractAppender {

    private final List<LogEvent> cacheLogEvent = new ArrayList<>(20);

    private final String beanName;
    /**
     * 默认容量1000个
     */
    private final int cacheSize;

    protected EmailAppender(String name, int cacheSize, String beanName, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
        this.beanName = beanName;
        this.cacheSize = cacheSize;
    }

    public static class Builder extends AbstractAppender.Builder<Builder>
            implements org.apache.logging.log4j.core.util.Builder<EmailAppender> {

        @PluginBuilderAttribute
        private String beanName;
        @PluginBuilderAttribute
        private Integer cacheSize;

        @Override
        public EmailAppender build() {
            if (getLayout() == null) setLayout(HtmlLayout.createDefaultLayout());
            if (getFilter() == null) setFilter(ThresholdFilter.createFilter(null, null, null));
            if (cacheSize == null) cacheSize = 1000;
            return new EmailAppender(getName(), cacheSize, beanName, getFilter(), getLayout(), isIgnoreExceptions(), getPropertyArray());
        }

        public void setBeanName(String beanName) {
            this.beanName = beanName;
        }

    }

    /**
     * @since 2.13.2
     */
    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public void append(LogEvent event) {
        if (beanName != null) {
            IEmailService emailService = Consts.getBean(beanName);
            if (emailService != null) {
                boolean result = emailService.sendEvents(getLayout(), event);
                if (result) { // 如果发送成功 发送缓存的所有日志事件
                    if (!cacheLogEvent.isEmpty()) pushCacheEvent();
                } else {// 将失败邮件放入缓存
                    addCache(event);
                }
            }
        } else addCache(event);
    }

    /**
     * 添加新的缓存日志事件
     * @param event 日志事件
     */
    public void addCache(LogEvent event) {
        cacheLogEvent.add(event);
        if (cacheLogEvent.size() > cacheSize) cacheLogEvent.remove(cacheLogEvent.size() - 1);
    }

    /**
     * 将所有的缓存日志事件发送出去
     */
    private void pushCacheEvent() {
        if (!this.cacheLogEvent.isEmpty()) {
            List<LogEvent> cache = this.cacheLogEvent.subList(0, this.cacheLogEvent.size());
            List<LogEvent> list = cache.stream().toList();
            cache.clear();
            list.forEach(this::pushEvent);
        }
    }

    /**
     * 发送邮箱命令
     * @param event 日志事件
     */
    private void pushEvent(LogEvent event) {
        IEmailService emailService = Consts.getBean(beanName);
        if (emailService != null) {
            boolean result = emailService.sendEvents(getLayout(), event);
            if (!result) { // 将失败邮件放入缓存
                addCache(event);
            }
        }
    }

}
