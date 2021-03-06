import db_classes.User;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import nodes.*;


class ConnectionInServer extends Thread {

    ObjectInputStream  in;
    ObjectOutputStream out;
    Socket clientSocket;
    String global_user_login;
    ChatTables ct;
    ArrayList<ObjectOutputStream> outList;
    ArrayList<String> onlineUsers;

    public ConnectionInServer (Socket clientSocket_, ChatTables ct_, ArrayList<ObjectOutputStream> outList_,
                               ArrayList<String> onlineUsers_ ) {
        onlineUsers = onlineUsers_;
        clientSocket = clientSocket_;
        ct = ct_;
        outList = outList_;
        try {
            in = new ObjectInputStream(clientSocket.getInputStream());
            out = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) { System.out.println("ConnectionInServer constr:"+e.getMessage()); e.printStackTrace(System.out);}
        this.start();

    }

    public void run() { // an echo server
        try {
            outList.add(out);
            MessageObject mo;
            ServerHandler sh;
            while(true) {
                mo = (MessageObject) in.readObject();
                sh = new ServerHandler(mo);
                if (mo.code == -1 || mo.code == 40)
                    break;
                mo = null;
            }
            onlineUsers.remove(global_user_login);
            outList.remove(out);
            out.close();
        } catch (EOFException e){System.out.println("EOF:"+e.getMessage()); e.printStackTrace(System.out);
        } catch (IOException e) {System.out.println("ConnectionInServer run():"+e.getMessage());
        } catch (ClassNotFoundException e) { System.out.println("ClassNotFoundException:"+e.getMessage()); e.printStackTrace();
        } finally {
            onlineUsers.remove(global_user_login);
        }
    }

    public class ServerHandler extends Thread {

        MessageObject cm;

        ServerHandler(MessageObject cm_) {
            cm = cm_;
            this.start();
        }

        public void SendMessage(ObjectOutputStream out_, MessageObject mo_) {
            try {
                out_.writeObject(mo_);
                out_.flush();
            } catch (IOException e) {
                System.out.println("SendMessage: "+ mo_.code +e.getMessage());
                e.printStackTrace(System.out);
            }
        }

        public void run() {
            int code = cm.code;
            switch (code) {
                // клиент просит сервер сделать это:
                case -1:
                    Close_connection(); // клиент завершил работу, просто закрыть соединение
                    break;
                case 21:
                    Check_login_password(cm.info); // log pass correct? нет - повторять бесконечно, до закрытия соединения
                    // да - показать список чатов
                    break;

                case 31:
                    User_sign_up(cm.info); // логин занят - перерегистрация, иначе отобразить пустой список чатов
                    break;
                case 40:
                    Close_connection(); // клиент завершил работу
                    break;
                case 41:
                    Open_conversation(cm.info); //chat; отправить содержимое чата
                    break;
                case 42:
                    Join_conversation(cm.info); //chat; добавить чат в список чатов юзера
                    break;
                case 43:
                    Send_conv_list();
                    break;
                    // отправить юзеру список бесед
                case 61:
                    Create_conversation(cm.info); // добавить в список участников чата создателя
                    break;
                    // если ссылка уникальна, иначе сообщение об ошибке

                case 71:
                    Broadcast_message(cm.info); // сообщение от юзера; записать сообщение в базу
                    break;
                    // для всех участников беседы разослать новое сообщение (71)
                case 74:
                    Get_members(cm.info);
                    break;
                case 73:
                    Leave_chat(cm.info); // удалить чат из списка чатов юзера
                    break;
                    /*
                case 72:
                    Create_task(cm.info, cm.texts[0]); // беседа, описание задачи
                case ??:
                    Show_task(??);

                ///TODO
                */
            }
        }

        public void Close_connection() {
            try {
                // проверить, не занят ли канал другим потоком
                in.close();
            } catch (IOException e) { System.out.println("readline:"+e.getMessage());}
        }

        public void Check_login_password(MessageNode info) {
            String user_login = info.text1;
            String user_password = info.text2;


            MessageObject mo = new MessageObject();
            if (onlineUsers.contains(user_login)){
                mo.code = 21;
                mo.info.text1 = "User already online";
                SendMessage(out, mo);
                return;
            }
            if (ct.isUserExist(user_login) ){
                if (ct.isPasswordCorrect(user_login, user_password)){
                    mo.code = 41;
                    global_user_login = user_login;
                    mo.info.text1 = user_login;
                    onlineUsers.add(global_user_login);
                }
                else {
                    mo.code = 21;
                    mo.info.text1 = "Incorrect password";
                }
            }
            else {
                mo.code = 21;
                mo.info.text1 = "Login not found. Create new account";
            }
            SendMessage(out, mo);
            // сохранить логин юзера или id для дальнейшего использования
        }

        public void User_sign_up(MessageNode info) {
            String user_login = info.text1;
            String user_password = info.text2;

            MessageObject mo = new MessageObject();
            if (ct.isUserExist(user_login)) {
                mo.code = 100;
                mo.info.text1 = "Login already used";
            }
            else {
                ct.addUser(user_login, user_password);
                mo.code = 21; // ввести лог пасс
                mo.info.text1 = user_login;
            }
            SendMessage(out, mo);
        }

        public void Open_conversation(MessageNode chat_info) {
            String conv_title = chat_info.text1;
            String conv_link = chat_info.text2;

            MessageObject mo = new MessageObject();
            // получить из базы последние N сообщений ( N=20) в виде массива MessageArray из MessageNode порядок (прямой, обратный?)
            mo.texts = ct.getMessages(conv_link);
            mo.code = 51;
            mo.info.text1 = conv_title;
            mo.info.text2 = conv_link;
            SendMessage(out, mo);
        }

        public void Send_conv_list() {
            MessageObject mo = new MessageObject();
            mo.code = 41;
            mo.texts = ct.getConversations(global_user_login);
            SendMessage(out, mo);
            if (mo.texts != null)
                System.out.println("SendMessage: "+ mo.code + mo.texts[0].text1);
            else
                System.out.println("SendMessage: "+ mo.code);
        }

        public void Join_conversation(MessageNode chat_info) {
            MessageObject mo = new MessageObject();
            if (!ct.isConversationExist(chat_info.text1)){
                mo.code = 100;
                mo.info.text1 = "No conversation found";
            }
            else {
                if (ct.isUserInConversation(global_user_login, chat_info.text1)) {
                    mo.code = 100;
                    mo.info.text1 = "You alreday in this conversation";
                } else {
                    ct.joinConversation(global_user_login, chat_info.text1);
                    return;
                }
            }
            SendMessage(out, mo);
        }

        public void Create_conversation(MessageNode chat_info) {
            MessageObject mo = new MessageObject();
            if (!ct.isConversationExist(chat_info.text1)){
                ct.addConversation(chat_info.text1, chat_info.text2);
                ct.joinConversation(global_user_login, chat_info.text2);
                mo.code = 41;
                mo.texts = ct.getConversations(global_user_login);
            }
            else{
                mo.code = 100;
                mo.info.text1 = "Conversation with your link is already exist";
            }
            SendMessage(out, mo);
        }

        public void Get_members(MessageNode chat_info) {
            MessageObject mo = new MessageObject();
            mo.code = 72;
            mo.texts = ct.getMembers(chat_info.text1);
            SendMessage(out, mo);
        }

        public void Broadcast_message(MessageNode chat_info) {
            String conv_link = chat_info.text1;
            String text = chat_info.text2;
            String sender = global_user_login;
            ct.addMessageToConversation(sender, conv_link, text);

            MessageObject mo = new MessageObject();
            mo.code = 71;
            mo.info.text2 = conv_link;
            mo.texts = new MessageNode[1];
            mo.texts[0] = new MessageNode();
            mo.texts[0].text1 = sender;
            mo.texts[0].text2 = text;
            for (ObjectOutputStream oos: outList){
                SendMessage(oos, mo);
            }
            // для всех открытых соединений разослать mo new SendMessageObject(mo)
        }

        public void Leave_chat(MessageNode chat_info) {
            String conv_link = chat_info.text1;
            ct.leaveConversation(global_user_login, conv_link);
        }

    }
}
