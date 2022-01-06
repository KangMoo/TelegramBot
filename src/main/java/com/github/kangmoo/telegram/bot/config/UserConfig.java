
package com.github.kangmoo.telegram.bot.config;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * User configuration
 *
 * @author kangmoo Heo
 */
@Getter
@Setter
public class UserConfig extends DefaultConfig {
    private static final Logger logger = getLogger(UserConfig.class);
    private static final String SECTION_BOT = "BOT";
    private static final String SECTION_ADDR_INFO = "ADDR_INFO";
    private static final String SECTION_ADMIN = "ADMIN";

    private static final String FIELD_TOKEN = "TOKEN";
    private static final String FIELD_IP_CHECK_URL = "IP_CHECK_URL";
    private static final String FIELD_SSH_PORT = "SSH_PORT";
    private static final String FIELD_FTP_PORT = "FTP_PORT";
    private static final String FIELD_ADMIN_ID = "ID";
    private String botToken;
    private String ipCheckUrl = "http://checkip.amazonaws.com";
    private int sshPort;
    private int ftpPort;
    private long adminId;

    private String sshAddr;
    private String ftpAddr;

    public UserConfig(String fileName) {
        super(fileName);

        if (load()) {
            loadConfig();
        }

        setConfigChangedListener((changed) -> {
            if (changed) {
                logger.warn("user configuration is changed by user.");
                loadConfig();
            }
        });
    }

    private void loadConfig() {
        botToken = getStrValue(SECTION_BOT, FIELD_TOKEN, null);
        ipCheckUrl = getStrValue(SECTION_ADDR_INFO, FIELD_IP_CHECK_URL, null);
        sshPort = getIntValue(SECTION_ADDR_INFO, FIELD_SSH_PORT, -1);
        ftpPort = getIntValue(SECTION_ADDR_INFO, FIELD_FTP_PORT, -1);
        adminId = Long.parseLong(getStrValue(SECTION_ADMIN, FIELD_ADMIN_ID, null));
    }
}
