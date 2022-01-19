package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {

    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private boolean authenticated;
    private String nickname;



    ClientHandler(Server server, Socket socket){
        this.server = server;
        this.socket = socket;

        try {

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

           new Thread(() -> {

                try {

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
                            if(newNick != null){
                                authenticated =true;
                                nickname = newNick;
                                sendMsg("/authok " + nickname);
                                server.subscribe(this);
                                System.out.println("Клиент: " + nickname + " вошел в чат :)");
                                break;
                            } else {
                                sendMsg("Нет такого пользователя! Ты что-то путаешь...");
                            }
                        }
                    }

                    //цикл работы
                    while (authenticated) {
                        String str = in.readUTF();

                        if (str.equals("/end")) {
                            sendMsg("/end");
                            break;
                        } else if(str.startsWith("/w")){
                            String[] message = str.split(" ", 3);
                            server.privatMsg(this, message[1], message[2]);
                        } else {
                            server.broadcastMsg(this, str);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("Клиент "+ nickname + " ушел из чата :(");
                    server.unsubscribe(this);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }).start();

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
}
