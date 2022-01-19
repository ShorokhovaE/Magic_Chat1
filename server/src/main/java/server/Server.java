package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {

    private ServerSocket server;
    private Socket socket;
    private final int PORT = 8189;

    private List<ClientHandler> clients; // список пользователей
    private AuthService authService;

    public Server() {

        clients = new CopyOnWriteArrayList<>(); // потокобезопасная реализация List
        authService = new SimpleAuthService();

        try {
            server = new ServerSocket(PORT);
            System.out.println("Сервер подключен!");

            while (true){ // цикл на ожидание подключения пользователей
                socket = server.accept();
                System.out.println("Клиент подключился!");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Сервер отлючен");
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void subscribe(ClientHandler clientHandler){
        clients.add(clientHandler);
    }
    public void unsubscribe(ClientHandler clientHandler){
        clients.remove(clientHandler);
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
            if (client.getNickname().equals(recipient)){
                client.sendMsg(message);
            }
            if (client.getNickname().equals(sender.getNickname())){
                client.sendMsg(message);
            }
        }



    }

    public AuthService getAuthService() {
        return authService;
    }
}
