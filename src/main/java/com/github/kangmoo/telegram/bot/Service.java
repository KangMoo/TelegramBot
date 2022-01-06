package com.github.kangmoo.telegram.bot;

import com.github.kangmoo.telegram.bot.config.UserConfig;
import com.github.kangmoo.telegram.bot.scheduler.IntervalTaskManager;
import com.github.kangmoo.telegram.bot.scheduler.handler.IpChecker;
import org.slf4j.Logger;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author kangmoo Heo
 */
public class Service {
    private static final Service INSTANCE = new Service();
    private static final Logger logger = getLogger(Service.class);
    private static final String USER_CONFIG_NAME = "bot.config";
    private UserConfig userConfig;
    private boolean isQuit = false;

    private Service() {
    }

    public static Service getInstance() {
        return INSTANCE;
    }

    public void start(String[] args) {
        if (args.length < 1) {
            logger.error("Bot Configuration path is necessary");
            return;
        }
        userConfig = new UserConfig(args[0] + "/" + USER_CONFIG_NAME);
        IntervalTaskManager.getInstance().init().start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.warn("Process is about to quit (Ctrl+C)");
            isQuit = true;
            stopService();
        }));
        new IpChecker(0).run();

        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(new BotHandler());

            while (!isQuit) {
                Thread.sleep(5000);
            }
        } catch (TelegramApiException | InterruptedException e) {
            logger.warn("Err Occurs", e);
        }
        logger.info("Process End");
    }

    public void stopService() {
    }

    public UserConfig getUserConfig() {
        return userConfig;
    }

    public boolean isQuit() {
        return isQuit;
    }

    public Service setQuit(boolean quit) {
        isQuit = quit;
        return this;
    }
}

