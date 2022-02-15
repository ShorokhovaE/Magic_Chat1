package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

    private ServerSocket server;
    private Socket socket;
    private final int PORT = 8189;

    private List<ClientHandler> clients; // список пользователей
    private AuthService authService;
    private ChangeNick changeNick;

    private static final Logger logger = Logger.getLogger(Server.class.getName());


    public Server() {

        clients = new CopyOnWriteArrayList<>(); // потокобезопасная реализация List
        authService = new DBAuthService();
        changeNick = new ChangeNickname();

        try {
            server = new ServerSocket(PORT);
            logger.log(Level.INFO, "Сервер подключен!");

            // подключаемся к БД
            DBAuthService.connect();

            while (true){ // цикл на ожидание подключения пользователей
                socket = server.accept();
                logger.log(Level.FINE, "Кто-то из клиентов подключился!");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            logger.log(Level.INFO, "Сервер отключен!");
            try {
                DBAuthService.disconnect(); // отключаемся от БД
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void subscribe(ClientHandler clientHandler){
        clients.add(clientHandler);
        broadcastClientList(); // отправляем после подключения список всех активных пользователей
    }
    public void unsubscribe(ClientHandler clientHandler){
        clients.remove(clientHandler);
        broadcastClientList(); // отправляем после отключения список всех активных пользователей
    }


    public void broadcastMsg(ClientHandler sender, String msg){
        String message = String.format("[ %s ]: %s", sender.getNickname(), msg);
        for (ClientHandler client : clients) {
            client.sendMsg(message);
        }
    }

    public void privatMsg(ClientHandler sender, String recipient, String msg){
        String message = String.format("[ %s ]: %s", sender.getNickname(), msg);
        for (ClientHandler client : clients) {
            if (client.getNickname().equals(recipient) || client.getNickname().equals(sender.getNickname())){
                client.sendMsg(message);
            }
        }
    }


    public boolean isLoginAuthenticated (String login){
        for (ClientHandler client : clients) {
            if(client.getLogin().equals(login)){
                return true;
            }
        }
        return false;
    }

    public void broadcastClientList(){ // отправка списка активных пользователей
        StringBuilder sb = new StringBuilder("/clientlist");

        for (ClientHandler client : clients) {
            sb.append(" ").append(client.getNickname());
        }

        String message = sb.toString();

        for (ClientHandler client : clients) {
            client.sendMsg(message);
        }

    }


    public AuthService getAuthService() {
        return authService;
    }

    public ChangeNick getChangeNick() {
        return changeNick;
    }
}
