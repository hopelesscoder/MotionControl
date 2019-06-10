package com.hopelesscoder.motioncontrol;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;

/**
 *  The class to send messages using TelegramBot library
 *
 * @author hopelesscoder (pasqdaniele@gmail.com)
 */
public class SendMessageImpl {
    static TelegramBot bot = new TelegramBot("Your token");
    static String chatId = "Your Chat ID";

    /**
     * Send a text message
     *
     * @param message the message to send
     *
     * @author hopelesscoder (pasqdaniele@gmail.com)
     */
    public static void sendToTelegram(String message) {
        bot.execute(new SendMessage(chatId,message));
    }

    /**
     * Send a picture and a caption
     *
     * @param caption the caption of the picture
     * @param picture the picture to send
     *
     * @author hopelesscoder (pasqdaniele@gmail.com)
     */
    public static void sendToTelegram(String caption, byte[] picture) {
        SendPhoto sendPhoto = new SendPhoto(chatId, picture);
        sendPhoto.caption(caption);
        new Thread(new Runnable() {
            @Override
            public void run() {
                bot.execute(sendPhoto);
            }
        }).start();
    }
}