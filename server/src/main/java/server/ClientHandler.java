package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler {

    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private boolean authenticated;
    private String nickname;
    private String login;

    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());


    ClientHandler(Server server, Socket socket){
        this.server = server;
        this.socket = socket;

        try {

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            ExecutorService service = Executors.newCachedThreadPool();

            service.execute(() -> {
                try {
                    socket.setSoTimeout(120000);
                    //цикл аунтификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.equals("/end")) {
                            sendMsg("/end");
                            break;
                        }
                        if (str.startsWith("/auth")){
                            String[] token = str.split(" ", 3);

                            if(token.length<3){
                                continue;
                            }

                            String newNick = server.getAuthService()
                                    .getNicknameByLoginAndPassword(token[1],token[2]);
                            login = token[1];
                            if(newNick != null){
                                if(!server.isLoginAuthenticated(login)){
                                    authenticated = true;
                                    nickname = newNick;
                                    sendMsg("/authok " + nickname);
                                    server.subscribe(this);
                                    logger.log(Level.FINE, "Клиент " + nickname + " вошел в чат");
                                    break;
                                } else {
                                    sendMsg("Этот пользователь уже в чате. Попробуй другое имя!");
                                }
                            } else {
                                sendMsg("Нет такого пользователя! Ты что-то путаешь...");
                            }
                        }

                        if (str.startsWith("/reg")){ //если полученное сообщение начинается на /reg
                            // делим полученное сообщение на 4 части
                            String[] token = str.split(" ", 4);
                            if(token.length<4){ // если получили меньше 4 значений, то ничего не происходит
                                continue;
                            }
                            //с помощью getAuthService() получаем ссылку на объект интерфейса AuthService и
                            // выполняем метод registration из класса DBAuthService с переданными логином, паролем и никнеймом
                            if(server.getAuthService().registration(token[1], token[2], token[3])){
                                // если метод возвращает true, то отправляем сообщение /reg_ok
                                sendMsg("/reg_ok");
                            } else { // если метод вернул false, то отправляем /reg_no
                                sendMsg("/reg_no");
                            }
                        }
                    }

                    socket.setSoTimeout(0);

                    //цикл работы
                    while (authenticated) {
                        String str = in.readUTF();

                        if(str.startsWith("/")){
                            logger.log(Level.FINE, "Клиент " + nickname + " прислал команду");
                            if (str.equals("/end")) {
                                sendMsg("/end");
                                break;
                            } else if(str.startsWith("/w")){
                                String[] message = str.split(" ", 3);
                                server.privatMsg(this, message[1], message[2]);
                                logger.log(Level.FINE, "Клиент " + nickname + " прислал приватное сообщение");
                            } else if(str.startsWith("/change")){
                                String[] change = str.split(" ", 3);
                                if(change.length<3){
                                    continue;
                                }
                                if(server.getChangeNick().changeNickname(change[1], change[2])){
                                    this.nickname = change[1];
                                    server.unsubscribe(this);
                                    server.subscribe(this);
                                    sendMsg("/change_ok " + change[1]);
                                } else {
                                    sendMsg("/change_no " + change[1]);
                                }
                            }
                        }
                        else {
                            server.broadcastMsg(this, str);
                            logger.log(Level.FINE, "Клиент " + nickname + " прислал сообщение");

                        }
                    }
                } catch (SocketTimeoutException e) {
                    sendMsg("/end");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    logger.log(Level.FINE, "Клиент "+ nickname + " ушел из чата");
                    server.unsubscribe(this);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            });

            service.shutdown();

        } catch (IOException e) {
            e.printStackTrace();
    }
    }

    public void sendMsg(String msg){
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
