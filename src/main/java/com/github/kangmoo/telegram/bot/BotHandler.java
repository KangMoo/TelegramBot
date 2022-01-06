package com.github.kangmoo.telegram.bot;

import com.github.kangmoo.telegram.bot.config.UserConfig;
import org.slf4j.Logger;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashSet;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author kangmoo Heo
 */
public class BotHandler extends TelegramLongPollingBot {
    private static final Logger logger = getLogger(BotHandler.class);
    private static final UserConfig userConfig = Service.getInstance().getUserConfig();
    private static final Set<Long> authorizedUsers = new HashSet<>();

    static {
        authorizedUsers.add(userConfig.getAdminId());
    }

    public static String getMyIp() {
        StringBuilder sb = new StringBuilder();
        if (userConfig.getSshAddr() != null) sb.append("SSH_ADDR\n").append(userConfig.getSshAddr()).append("\n");
        if (userConfig.getFtpAddr() != null) sb.append("FTP_ADDR\n").append(userConfig.getFtpAddr()).append("\n");
        return sb.toString();
    }

    @Override
    public String getBotUsername() {
        return "SENS_RELAY";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(update.getMessage().getChatId().toString());
            sendMessage.setText(getMyIp());
            try {
                execute(sendMessage);
            } catch (Exception e) {
                logger.warn("Fail to send message", e);
            }
        }
    }

    @Override
    public String getBotToken() {
        return userConfig.getBotToken();
    }
}
