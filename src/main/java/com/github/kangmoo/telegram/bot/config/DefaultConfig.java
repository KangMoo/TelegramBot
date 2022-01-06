package com.github.kangmoo.telegram.bot.config;

/**
 * @author kangmoo Heo
 */

import org.apache.commons.configuration2.*;
import org.apache.commons.configuration2.builder.ConfigurationBuilderEvent;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.event.Event;
import org.apache.commons.configuration2.event.EventListener;
import org.apache.commons.configuration2.reloading.PeriodicReloadingTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DefaultConfig {
    private static final Logger logger = LoggerFactory.getLogger(DefaultConfig.class);
    private final String fileName;
    private ReloadingFileBasedConfigurationBuilder<FileBasedConfiguration> builder;
    private Configuration config;
    private Consumer<Boolean> configChangedListener = null;

    public DefaultConfig(String fileName) {
        this.fileName = fileName;
    }

    protected boolean load() {
        if (this.fileName == null) {
            logger.error("config's name is null.");
            return false;
        } else {
            Parameters params = new Parameters();
            File file;
            if (!this.fileName.startsWith("/")) {
                file = new File("src/main/resources/config/" + this.fileName);
            } else {
                file = new File(this.fileName);
            }

            this.builder = (new ReloadingFileBasedConfigurationBuilder(INIConfiguration.class)).configure(params.fileBased().setFile(file).setReloadingRefreshDelay(0L));

            try {
                this.config = this.builder.getConfiguration();
            } catch (Exception var4) {
                logger.error("builder exception. {}", var4.getMessage());
            }

            PeriodicReloadingTrigger trigger = new PeriodicReloadingTrigger(this.builder.getReloadingController(), null, 1L, TimeUnit.SECONDS);
            trigger.start();
            this.builder.addEventListener(ConfigurationBuilderEvent.RESET, new DefaultConfig.CustomConfigurationEvent());
            logger.info("Load config [{}]", this.fileName);
            return true;
        }
    }

    public void setConfigChangedListener(Consumer<Boolean> listener) {
        this.configChangedListener = listener;
    }

    public String getStrValue(String section, String key, String defaultValue) {
        String mkey = section + "." + key;
        if (section == null) {
            logger.info("\tConfig [{} = {}]", key, defaultValue);
            return defaultValue;
        } else {
            String value = this.config.getString(mkey, defaultValue);
            logger.info("\tConfig [{} = {}]", key, value);
            return value;
        }
    }

    public int getIntValue(String section, String key, int defaultValue) {
        String mkey = section + "." + key;
        if (section == null) {
            logger.info("\tConfig [{} = {}]", key, defaultValue);
            return defaultValue;
        } else {
            int value = this.config.getInt(mkey, defaultValue);
            logger.info("\tConfig [{} = {}]", key, value);
            return value;
        }
    }

    public float getFloatValue(String section, String key, float defaultValue) {
        String mkey = section + "." + key;
        if (section == null) {
            logger.info("\tConfig [{} = {}]", key, defaultValue);
            return defaultValue;
        } else {
            float value = this.config.getFloat(mkey, defaultValue);
            logger.info("\tConfig [{} = {}]", key, value);
            return value;
        }
    }

    public boolean getBooleanValue(String section, String key, boolean defaultValue) {
        String mkey = section + "." + key;
        if (section == null) {
            logger.info("\tConfig [{} = {}]", key, defaultValue);
            return defaultValue;
        } else {
            boolean value = this.config.getBoolean(mkey, defaultValue);
            logger.info("\tConfig [{} = {}]", key, value);
            return value;
        }
    }

    public String getFileName() {
        return this.fileName;
    }

    private class CustomConfigurationEvent implements EventListener<Event> {
        private CustomConfigurationEvent() {
        }

        public void onEvent(Event event) {
            DefaultConfig.logger.warn("onEvent");

            try {
                Configuration newConfig = DefaultConfig.this.builder.getConfiguration();
                ConfigurationComparator comparator = new StrictConfigurationComparator();
                if (!comparator.compare(DefaultConfig.this.config, newConfig)) {
                    this.diffConfig(DefaultConfig.this.config, newConfig);
                }

                newConfig.clear();
            } catch (Exception var4) {
                DefaultConfig.logger.error("change config exception. {}", var4.getMessage());
            }

        }

        private void diffConfig(Configuration config1, Configuration config2) {
            boolean changed = false;
            Iterator keys = config1.getKeys();

            String key;
            Object v2;
            while (keys.hasNext()) {
                key = (String) keys.next();
                v2 = config1.getProperty(key);
                Object v2x = config2.getProperty(key);
                if (!Objects.equals(v2, v2x)) {
                    DefaultConfig.this.config.setProperty(key, v2x);
                    if (!changed) {
                        changed = true;
                    }
                }
            }

            keys = config2.getKeys();

            while (keys.hasNext()) {
                key = (String) keys.next();
                v2 = config2.getProperty(key);
                if (!config1.containsKey(key)) {
                    DefaultConfig.this.config.setProperty(key, v2);
                    if (!changed) {
                        changed = true;
                    }
                }
            }

            if (changed && DefaultConfig.this.configChangedListener != null) {
                DefaultConfig.this.configChangedListener.accept(true);
            }

        }
    }
}
