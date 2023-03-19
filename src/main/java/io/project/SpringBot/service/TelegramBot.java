package io.project.SpringBot.service;

import io.project.SpringBot.config.BotConfig;
import io.project.SpringBot.model.*;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserListRepository userListRepository;

    @Autowired
    private InviteRepository inviteRepository;

    final BotConfig config;

    public TelegramBot(BotConfig config){
        this.config = config;
    }

    LocalDate today = LocalDate.now();

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
                case "Invite people" -> {
                    Invite invite = new Invite(today, userRepository.findById(chatId).get().getFirstName());
                    inviteRepository.save(invite);
                    sendInvite(chatId);
                }
                case "+" ->{
                    addPeople(chatId);
                }
                case "-" ->{
                    deletePeople(chatId);
                }
                case "Peoples list" ->{
                    peopleList(chatId);
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
        boolean checkUser = true;
        for (Invite invite1 : inviteRepository.findAll()) {
            if (invite1.getDay().equals(today)) {
                addButton2(message);
                checkUser = false;
                break;
            }
        }
        if(checkUser) {
            addButton(message);
        }
        try {
            execute(message);
        }catch (TelegramApiException e){
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void addButton(SendMessage message) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("Invite people");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
    }

    private void addButton2(SendMessage message) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("+");
        row.add("-");
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("Peoples list");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
    }

    private void addPeople(long chatId){
        UserList userList = new UserList(today, userRepository.findById(chatId).get().getFirstName());
        boolean checkUser = true;
        for (UserList user : userListRepository.findAll()) {
            if (user.getDay().equals(today) && user.getName().equals(userList.getName())) {
                sendMessage(chatId, "You invite");
                sendPeopleList();
                checkUser = false;
                break;
            }
        }
        if(checkUser) {
            userListRepository.save(userList);
            sendMessage(chatId, "Cool");
            sendPeopleList();
        }
    }

    private void deletePeople(long chatId){
        UserList userList = new UserList(today, userRepository.findById(chatId).get().getFirstName());
        for (UserList user : userListRepository.findAll()) {
            if (user.getDay().equals(today) && user.getName().equals(userList.getName())) {
                userListRepository.deleteById(user.getId());
                sendMessage(chatId, "Nooooo");
                sendPeopleList();
                break;
            }
        }
        sendMessage(chatId, "You are not registered");
    }

    private void peopleList(long chatId){
        StringBuilder userList = new StringBuilder();
        for (UserList user : userListRepository.findAll()) {
            if (user.getDay().equals(today)) {
                userList.append(", " + user.getName());
            }
        }
        sendMessage(chatId, "" + today + userList);
    }

    private void registerUser(Message message) {
        if (!userRepository.findById(message.getChatId()).isPresent()) {
            var chatId = message.getChatId();
            var chat = message.getChat();

            User user = new User(chatId, chat.getFirstName(), chat.getLastName(), chat.getUserName(), new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("User saved " + user);
        }
    }

    private void sendInvite(long chatId) {
        var users = userRepository.findAll();
        for (User user : users) {
            sendMessage(user.getChatId(),
                    "Hello brother " + user.getFirstName() + ", shall we meet and go for a walk today at 20:00? Send + or -.\n" +
                            "Brother " + userRepository.findById(chatId).get().getFirstName() + " invites.");
        }
    }

    private void sendPeopleList() {
        var users = userRepository.findAll();
        for (User user : users) {
            peopleList(user.getChatId());
        }
    }
}

