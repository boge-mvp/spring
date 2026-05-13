package com.log4j;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.AbstractLookup;
import org.apache.logging.log4j.core.lookup.StrLookup;

@Plugin(name = "CustomLookup", category = StrLookup.CATEGORY)
public class CustomLookup extends AbstractLookup {

    @Override
    public String lookup(LogEvent event, String key) {
//        System.out.println("------------- " + event + " --");
//        System.out.println("------------- " + key + " --");
        return calculateValue(key);
    }

    private String calculateValue(String key) {
        // 实现你的自定义逻辑...

        return key;
    }




}
