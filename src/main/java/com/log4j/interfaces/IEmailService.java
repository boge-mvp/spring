package com.log4j.interfaces;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;

public interface IEmailService {

    /**
     * 发送事件邮箱
     * @param layout 布局
     * @param appendEvent 日志事件
     * @return 成功与否
     */
    boolean sendEvents(Layout<?> layout, LogEvent appendEvent);

    /**
     * 发送文本邮箱
     * @param content 内容
     * @param subject 主题
     * @return 成功与否
     */
    boolean sendText(String content, String subject);

}
