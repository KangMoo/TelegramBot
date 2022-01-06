package com.github.kangmoo.telegram.bot.handler;

import org.slf4j.Logger;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author kangmoo Heo
 */
public class MessageHandler extends TelegramLongPollingBot {
    private static final Logger logger = getLogger(MessageHandler.class);
    private static final Set<Long> authorizedUsers = new HashSet<>();

    static {
        authorizedUsers.add(958756619L);
    }

    public static String getMyIp() {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com").openStream()))) {
            return bufferedReader.readLine();
        } catch (Exception e) {
            logger.warn("ERR Occurs while Checking IP", e);
        }
        return null;
    }

    @Override
    public String getBotUsername() {
        return "SENS_RELAY";
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        logger.info("RECV : {}", message);
        if (message == null || !authorizedUsers.contains(message.getFrom().getId())) {
            logger.info("Recv msg from unknown User");
            return;
        }

        String chatId = update.getMessage().getChatId().toString();
        try {
            sampleInlineKeyboardButton(chatId);
        } catch (Exception e) {
            logger.warn("ERR", e);
        }

//        if (update.hasMessage() && update.getMessage().hasText()) {
//            SendMessage sendMessage = new SendMessage();
//            sendMessage.setChatId(update.getMessage().getChatId().toString());
//            sendMessage.setText("자동 응답으로 보낼 메시지\n" + getMyIp()+":88");
//            try {
//                execute(sendMessage);
//            } catch (Exception e) {
//                logger.warn("Fail to send message", e);
//            }
//        }
    }

    @Override
    public String getBotToken() {
        return "5000673919:AAGnkEQ7MEtykveQw6gKmS0LDujr0fs4tnI";
    }

    public void keyboardSample(String chatId) throws TelegramApiException {
        KeyboardRow buttons1 = new KeyboardRow();
        buttons1.addAll(Arrays.asList("A", "B"));
        KeyboardRow buttons2 = new KeyboardRow();
        buttons2.addAll(Arrays.asList("A", "B"));
        ReplyKeyboardMarkup buttons = ReplyKeyboardMarkup.builder()
                .keyboardRow(buttons1)
                .keyboardRow(buttons2)
                .build();
        execute(
                SendMessage.builder().chatId(chatId)
                        .text("WW")
                        .replyMarkup(buttons)
                        .build());
    }

    public void sendPollSample(String chatId) throws TelegramApiException {
        List<String> options = new ArrayList<>();
        options.add("Yes");
        options.add("No");

        SendPoll ourPoll = new SendPoll(chatId, "Some Question", options);
        execute(ourPoll);
    }

    public void sampleInlineKeyboardButton(String chatId) throws TelegramApiException {
        InlineKeyboardButton button = InlineKeyboardButton
                .builder()
                .text("Button1")
                .callbackData("My Callback1")
                .build();
        InlineKeyboardButton button2 = InlineKeyboardButton
                .builder()
                .text("Button2")
                .callbackData("My Callback2")
                .build();
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);
        row.add(button2);
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row);

        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup
                .builder()
                .keyboard(keyboard)
                .build();

        execute(SendMessage.builder().chatId(chatId).text("WOW").replyMarkup(inlineKeyboardMarkup).build());
    }
}
