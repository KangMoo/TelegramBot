package com.github.kangmoo.telegram.bot.scheduler.handler;

import com.github.kangmoo.telegram.bot.Service;
import com.github.kangmoo.telegram.bot.config.UserConfig;
import com.github.kangmoo.telegram.bot.scheduler.IntervalTaskUnit;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author kangmoo Heo
 */
public class IpChecker extends IntervalTaskUnit {
    private static final Logger logger = getLogger(IpChecker.class);
    private static final UserConfig userConfig = Service.getInstance().getUserConfig();

    public IpChecker(int interval) {
        super(interval);
    }

    public static String getMyIp() {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(userConfig.getIpCheckUrl()).openStream()))) {
            return bufferedReader.readLine();
        } catch (Exception e) {
            logger.warn("ERR Occurs while Checking IP", e);
        }
        return null;
    }

    @Override
    public synchronized void run() {
        try {
            String myIp = getMyIp();
            if (myIp == null) {
                userConfig.setSshAddr(null);
                userConfig.setFileBrowserAddr(null);
                return;
            }
            userConfig.setSshAddr(userConfig.getSshPort() > 1023 ? myIp + ":" + userConfig.getSshPort() : null);
            userConfig.setFileBrowserAddr(userConfig.getFileBrowserPort() > 1023 ? myIp + ":" + userConfig.getFileBrowserPort() : null);
        } catch (Exception e) {
            logger.warn("Err Occurs. [{}]", this.getClass().getSimpleName(), e);
        }
    }
}
