package io.project.SpringBot.service;

import io.project.SpringBot.config.BotConfig;
import io.project.SpringBot.model.User;
import io.project.SpringBot.model.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;

    final BotConfig config;

    static final String HELP_TEXT = "Demo test";

    public TelegramBot(BotConfig config){
        this.config = config;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start" -> {
                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                }
                case "/send" -> {
                    var users = userRepository.findAll();
                    for (User user : users) {
                        sendMessage(user.getChatId(),
                                "Hello brother " + user.getFirstName() + ", shall we meet and go for a walk? Send + or -");
                    }
                }
                default -> sendMessage(chatId,
                        userRepository.findById(chatId).get().getFirstName() + ", sorry, but the engineer has not yet added such a function, write to the developer by mail to get feedback");
            }

        }
    }

    private void startCommandReceived(long chatId, String name){
        String answer = "Hi, " + name + ", go gyliat?";
        log.info("Replied to user " + name);
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        try {
            execute(message);
        }catch (TelegramApiException e){
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void registerUser(Message message){
        if(!userRepository.findById(message.getChatId()).isPresent()){
            var chatId = message.getChatId();
            var chat = message.getChat();

            User user = new User(chatId, chat.getFirstName(), chat.getLastName(), chat.getUserName(), new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("User saved " + user);
        }
    }

}
