package com.github.kangmoo.telegram.bot.handler;

import org.slf4j.Logger;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author kangmoo Heo
 */
public class CallbackQueryHandler extends TelegramLongPollingBot {
    private static final Logger logger = getLogger(CallbackQueryHandler.class);

    @Override
    public String getBotUsername() {
        return "SENS_RELAY";
    }

    @Override
    public void onUpdateReceived(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery == null) return;
        logger.debug("CALLBACK : {}", callbackQuery.getData());
    }

    @Override
    public String getBotToken() {
        return "5000673919:AAGnkEQ7MEtykveQw6gKmS0LDujr0fs4tnI";
    }


}
