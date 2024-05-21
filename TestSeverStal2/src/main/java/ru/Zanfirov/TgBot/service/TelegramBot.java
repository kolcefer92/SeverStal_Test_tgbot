package ru.Zanfirov.TgBot.service;

import lombok.Data;
import org.aspectj.weaver.ast.Not;
import org.glassfish.grizzly.http.Note;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.Zanfirov.TgBot.config.BotConfig;
import ru.Zanfirov.TgBot.entity.Notes;
import ru.Zanfirov.TgBot.entity.Users;
import ru.Zanfirov.TgBot.repository.NotesRepo;
import ru.Zanfirov.TgBot.repository.UserRepo;


import java.util.*;


@Component
@Data
public class TelegramBot extends TelegramLongPollingBot {


    final BotConfig config;

    public TelegramBot(BotConfig config) {
        this.config = config;

//Фиксированная кнопка /start внизу окна

        List<BotCommand> listOfStart = new ArrayList<>();
        listOfStart.add(new BotCommand("/start","Нажмите для входа в главное меню"));
        try {
            execute(new SetMyCommands(listOfStart, new BotCommandScopeDefault(), null));
        }
        catch (TelegramApiException e){
            System.out.println(e);
        }
    }

//репозитории для работы БД
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private NotesRepo notesRepo;
//Вспомогательные мапы и пеерменные
    private Map<Long, Integer> textMap = new HashMap<>();
    private Map<String, Long> messageIdMap = new HashMap<>();

    Notes notesGlobal;


    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            long messageId = update.getMessage().getMessageId();


            switch (messageText) {
                case "/start":


                    //  startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    if (userRepo.findById(chatId).isEmpty()) {
                        String welcome = "Добро пожаловать! Здесь можно создать до 10 заметок с фото. Заметки редактируются, удаляются и просматриваются из списка. "+
                                "Заголовок устанавливается автоматически. Реализована логика работы в одном окне.";
                        String welcomePhoto = "AgACAgIAAxkBAAICQWZLSAPAq7SfVmrMGvQZMbCLEv34AAJf2TEbYkxZSq2mSe7qaW5fAQADAgADeQADNQQ";

                        userRepo.save(new Users(chatId, update.getMessage().getChat().getFirstName()));
                        notesRepo.save(new Notes("Инструкция",welcome,welcomePhoto,userRepo.findById(chatId).get()));
                        deleteMessage(chatId,messageId);
                        startMessage(chatId);

                    }else {

                        deleteMessage(chatId,messageId);
                        startMessage(chatId);
                    }

                    break;

            }


            if (!textMap.containsKey(chatId)) {
                return;
            }
            int state = textMap.get(chatId);
            /*
            В этом свиче мы записываем заметку и ее заголовок,
            а так же данные при редактировании заметки.


             */


            switch (state) {
                case (1):

                    String input = messageText;
                    if (input == null || input.isEmpty()) {
                        return;
                    }

                    int length = input.length();
                    int endIndex = Math.min(length, 25);

                    int dotIndex = input.indexOf('.');
                    if (dotIndex != -1 && dotIndex < 25) {
                        endIndex = dotIndex;
                    }

                    input = input.substring(0, endIndex) + "...";


                    notesRepo.save(new Notes(input, messageText, null, userRepo.findById(chatId).get()));
                    textMap.clear();
                    deleteMessage(chatId, messageIdMap.get("messageId"));
                    startMessage(chatId);

                    messageIdMap.clear();

                    break;


                case (2):


                    String input2 = messageText;
                    if (input2 == null || input2.isEmpty()) {
                        return;
                    }

                    int length2 = input2.length();
                    int endIndex2 = Math.min(length2, 25);

                    int dotIndex2 = input2.indexOf('.');
                    if (dotIndex2 != -1 && dotIndex2 < 25) {
                        endIndex2 = dotIndex2;
                    }

                    input2 = input2.substring(0, endIndex2) + "...";

                    System.out.println(this.notesGlobal.getId());


                    Notes notesEdit = notesRepo.findById(this.notesGlobal.getId()).get();
                    notesEdit.setHeadLine(input2);
                    notesEdit.setMessage(messageText);
                    notesRepo.save(notesEdit);


                    textMap.clear();

                    messageIdMap.clear();


                    break;

            }

            //удаляем входящие сообщения для чистоты в чате.
            deleteMessage(chatId, messageId);

            //Сохраняем Id фотографии в таблицу к заметке
        } else if (update.hasMessage() && update.getMessage().hasPhoto()) {
            long messageId = update.getMessage().getMessageId();
            long chatId = update.getMessage().getChatId();
            int set = textMap.get(chatId);

            if (!textMap.containsKey(chatId)) {
                return;
            }

            switch (set) {

                case (3):


                    List<PhotoSize> photos = update.getMessage().getPhoto();

                    String f_id = photos.stream().max(Comparator.comparing(PhotoSize::getFileSize))
                            .map(PhotoSize::getFileId)
                            .orElse("");

                    Notes notesEditPhoto = notesRepo.findById(this.notesGlobal.getId()).get();
                    notesEditPhoto.setUrlPicture(f_id);
                    notesRepo.save(notesEditPhoto);

                    deleteMessage(chatId, messageIdMap.get("messageId"));
                    deleteMessage(chatId, messageId);
                    // startMessageEdit(chatId, messageIdMap.get("messageId"));
                    startMessage(chatId);

                    break;
            }

            //Болшой блок для обработки гажатий на кнопки в сообщении

        } else if (update.hasCallbackQuery()) {

            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            //получаем список всех заметок пользователя
            List<Notes> notes = notesRepo.findByUsersChatID(chatId);

            switch (callbackData) {
                case ("CreateNote"):
                    //Создаем заметку

                    sendEditMessage(chatId, messageId, "Введите текст заметки");
                    textMap.put(chatId, 1);


                    //сохраняем данные о сообщении для его последующего редактирования
                    messageIdMap.put("messageId", messageId);
                    break;


                case ("ShowNotes"):

// отображаем список всех заметок. Передаем в метод саписок всех заметок записаных на этот chatId
                    allNotesMessage(notes, chatId, messageId);


                    break;

                case ("addPhoto2"):

                    //Добавляем фото к заметке, с помощью textMap попадаем в свитч с сохранением фото


                    sendEditMessage(chatId, messageId, "Прикрепите фото");

                    textMap.put(chatId, 3);
                    messageIdMap.put("messageId", messageId);


                    break;


                case ("delPhoto2"):

                    //Удаляем фото из заметки, удаляем текущее сообщение, выводим новое с списком заметок


                    Notes notesEditPhoto = notesRepo.findById(this.notesGlobal.getId()).get();
                    notesEditPhoto.setUrlPicture(null);
                    notesRepo.save(notesEditPhoto);


                    deleteMessage(chatId, messageId);


                    allNotesMessageNew(notes, chatId);


                    break;


                case ("back2"):

                    deleteMessage(chatId, messageId);


                    allNotesMessageNew(notes, chatId);

                    break;

                case ("enterTextNote"):

                    deleteMessage(chatId, messageId);


                    allNotesMessageNew(notes, chatId);

                    break;

                case ("edit2"):

                    //Изменение заметки, с помощью textMap попадаем в соответсвующий блок после получения сообщения от пользователя.

                    messageIdMap.put("messageId", messageId);
                    textMap.put(chatId, 2);
                    deleteMessage(chatId, messageId);

                    NewTextNotes(chatId);


                    break;

                case ("delete2"):

                    //Удаляем заметку

                    notesRepo.deleteById(this.notesGlobal.getId());
                    deleteMessage(chatId, messageId);
                    startMessage(chatId);
                    //allNotesMessageNew(notes, chatId);


                    break;

                //отображение заметки и фото, если прикреплено
                case ("0zametka"):


                    this.notesGlobal = notes.get(0);


                    showNote(chatId, messageId, this.notesGlobal);


                    break;

                case ("1zametka"):


                    this.notesGlobal = notes.get(1);


                    showNote(chatId, messageId, this.notesGlobal);


                    break;
                case ("2zametka"):

                    this.notesGlobal = notes.get(2);


                    showNote(chatId, messageId, this.notesGlobal);


                    break;


                case ("3zametka"):

                    this.notesGlobal = notes.get(3);


                    showNote(chatId, messageId, this.notesGlobal);

                    break;
                case ("4zametka"):

                    this.notesGlobal = notes.get(4);


                    showNote(chatId, messageId, this.notesGlobal);

                    break;
                case ("5zametka"):

                    this.notesGlobal = notes.get(5);


                    showNote(chatId, messageId, this.notesGlobal);

                    break;
                case ("6zametka"):

                    this.notesGlobal = notes.get(6);


                    showNote(chatId, messageId, this.notesGlobal);

                    break;
                case ("7zametka"):

                    this.notesGlobal = notes.get(7);

                    showNote(chatId, messageId, this.notesGlobal);

                    break;
                case ("8zametka"):

                    this.notesGlobal = notes.get(8);

                    showNote(chatId, messageId, this.notesGlobal);

                    break;
                case ("9zametka"):

                    this.notesGlobal = notes.get(9);


                    showNote(chatId, messageId, this.notesGlobal);

                    break;

                case ("back1"):

                    startMessageEdit(chatId, messageId);

                    break;

                case ("clear1"):

                    //Удаляет все заметки пользователя

                    for (Notes notes2 : notes) {
                        notesRepo.delete(notes2);
                    }


                    notes.clear();

                    startMessageEdit(chatId, messageId);


                    break;

                case ("new_notes"):


                    sendEditMessage(chatId, messageId, "Введите текст заметки");
                    textMap.put(chatId, 1);

                    messageIdMap.put("messageId", messageId);

                    break;
            }


        }


    }


    public void allNotesMessage(List<Notes> list, long chatId, long messageId) {

        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText("Список всех заметок");
        message.setMessageId((int) messageId);


        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        int size = list.size();
        if (list.size() > 10) {
            size = 10;

        }

        for (int i = 0; i < size; i++) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            var button = new InlineKeyboardButton();
            button.setText(list.get(i).getHeadLine());
            button.setCallbackData(i + "zametka");
            rowInline.add(button);
            rowsInline.add(rowInline);

        }


        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();

        var back1 = new InlineKeyboardButton();
        back1.setText("Назад");
        back1.setCallbackData("back1");
        rowInline1.add(back1);

        var clear1 = new InlineKeyboardButton();
        clear1.setText("Очистить");
        clear1.setCallbackData("clear1");
        rowInline1.add(clear1);

        var new_notes = new InlineKeyboardButton();
        new_notes.setText("Новая заметка");
        new_notes.setCallbackData("new_notes");
        rowInline1.add(new_notes);


        rowsInline.add(rowInline1);


        keyboardMarkup.setKeyboard(rowsInline);

        message.setReplyMarkup(keyboardMarkup);

        executeEditMessage(message);


    }


    public void allNotesMessageNew(List<Notes> list, long chatId) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Список всех заметок");


        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

//ограничиваем выдачу 10 заметками

        int size = list.size();
        if (list.size() > 10) {
            size = 10;

        }

        for (int i = 0; i < size; i++) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            var button = new InlineKeyboardButton();
            button.setText(list.get(i).getHeadLine());
            button.setCallbackData(i + "zametka");
            rowInline.add(button);
            rowsInline.add(rowInline);

        }


        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();

        var back1 = new InlineKeyboardButton();
        back1.setText("Назад");
        back1.setCallbackData("back1");
        rowInline1.add(back1);

        var clear1 = new InlineKeyboardButton();
        clear1.setText("Очистить");
        clear1.setCallbackData("clear1");
        rowInline1.add(clear1);

        var new_notes = new InlineKeyboardButton();
        new_notes.setText("Новая заметка");
        new_notes.setCallbackData("new_notes");
        rowInline1.add(new_notes);


        rowsInline.add(rowInline1);


        keyboardMarkup.setKeyboard(rowsInline);

        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);


    }


    public void startMessage(long chatid) {


        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatid));
        message.setText("Выберите действие");

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();


        var CreateNote = new InlineKeyboardButton();
        CreateNote.setText("Создать заметку");
        CreateNote.setCallbackData("CreateNote");
        rowInline1.add(CreateNote);

        var ShowNotes = new InlineKeyboardButton();
        ShowNotes.setText("Показать все заметки");
        ShowNotes.setCallbackData("ShowNotes");
        rowInline2.add(ShowNotes);


        rowsInline.add(rowInline1);
        rowsInline.add(rowInline2);


        keyboardMarkup.setKeyboard(rowsInline);

        message.setReplyMarkup(keyboardMarkup);


        executeMessage(message);

    }


    public void NewTextNotes(long chatid) {


        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatid));
        message.setText("Введите текст заметки и нажмите ОК");

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();


        var enterTextNote = new InlineKeyboardButton();
        enterTextNote.setText("ОК");
        enterTextNote.setCallbackData("enterTextNote");
        rowInline1.add(enterTextNote);


        rowsInline.add(rowInline1);


        keyboardMarkup.setKeyboard(rowsInline);

        message.setReplyMarkup(keyboardMarkup);


        executeMessage(message);

    }


    public void startMessageEdit(long chatId, long messageId) {

        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите действие");
        message.setMessageId((int) messageId);


        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();


        var CreateNote = new InlineKeyboardButton();
        CreateNote.setText("Создать заметку");
        CreateNote.setCallbackData("CreateNote");
        rowInline1.add(CreateNote);

        var ShowNotes = new InlineKeyboardButton();
        ShowNotes.setText("Показать все заметки");
        ShowNotes.setCallbackData("ShowNotes");
        rowInline2.add(ShowNotes);


        rowsInline.add(rowInline1);
        rowsInline.add(rowInline2);


        keyboardMarkup.setKeyboard(rowsInline);

        message.setReplyMarkup(keyboardMarkup);


        executeEditMessage(message);

    }

    private void showNote(long chatId, long messageId, Notes notes) {

        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(notes.getMessage());
        message.setMessageId((int) messageId);


        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();


        var back2 = new InlineKeyboardButton();
        back2.setText("Назад");
        back2.setCallbackData("back2");
        rowInline2.add(back2);

        var edit2 = new InlineKeyboardButton();
        edit2.setText("Изменить");
        edit2.setCallbackData("edit2");
        rowInline2.add(edit2);


        var delete2 = new InlineKeyboardButton();
        delete2.setText("Удалить");
        delete2.setCallbackData("delete2");
        rowInline1.add(delete2);

        if (notes.getUrlPicture() == null) {

            var addPhoto2 = new InlineKeyboardButton();
            addPhoto2.setText("Добавить фото");
            addPhoto2.setCallbackData("addPhoto2");
            rowInline1.add(addPhoto2);
        } else {

            var delPhoto2 = new InlineKeyboardButton();
            delPhoto2.setText("Удалить фото");
            delPhoto2.setCallbackData("delPhoto2");
            rowInline1.add(delPhoto2);

        }

        rowsInline.add(rowInline1);
        rowsInline.add(rowInline2);


        keyboardMarkup.setKeyboard(rowsInline);

        message.setReplyMarkup(keyboardMarkup);


        if (notes.getUrlPicture() != null) {

            SendPhoto msg = SendPhoto
                    .builder()
                    .chatId(String.valueOf(chatId))
                    .photo(new InputFile(notes.getUrlPicture()))
                    .replyMarkup(keyboardMarkup)
                    .caption(notes.getMessage())
                    .build();

            try {
                deleteMessage(chatId, messageId);
                execute(msg);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

        } else {

            executeEditMessage(message);
        }

    }


//    private void startCommandReceived(long chatId, String name) {
//
//        String answer = "Hi, " + name + ", nice to meet you";
//        sendMessage(chatId, answer);
//    }

    public void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {

        }
    }

    public void sendEditMessage(long chatId, long messageId, String text) {

        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);

        executeEditMessage(message);


    }


    public void executeEditMessage(EditMessageText message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {

        }
    }


    public void deleteMessage(long chatId, long messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(String.valueOf(chatId));
        deleteMessage.setMessageId((int) messageId);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String getBotUsername() {
        return config.getBotName();
    }


    @Override
    public String getBotToken() {
        return config.getToken();
    }

}

