package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class Client implements Initializable {
    @FXML
    public TextField textMassage;
    @FXML
    public TextArea Chat;
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public HBox authPanel;
    @FXML
    public HBox msgPanel;
    @FXML
    public ListView<String> clientList;
    @FXML
    public Button changeNickname;
    @FXML
    public HBox changeNicknamePanel;
    @FXML
    public TextField newNicknameField;


    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private final String ADDRESS = "localhost";
    private final int PORT = 8189;

    private boolean authenticated;
    private String nickname;
    private Stage stage;
    private Stage regStage;
    private RegController regController;
    private FileWriter fileHistory;

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;

        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);

        msgPanel.setVisible(authenticated);
        msgPanel.setManaged(authenticated);
        clientList.setVisible(authenticated);
        clientList.setManaged(authenticated);
        changeNickname.setVisible(authenticated);
        changeNickname.setManaged(authenticated);

        if(!authenticated){
            nickname = "";
        }

        setTitle(nickname);
        Chat.clear();

        if(authenticated){
            showLast100Message(loginField.getText().trim());
        }
    }



    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) textMassage.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                System.out.println("До свидания!");
                if(socket != null && !socket.isClosed()){
                    try {
                        out.writeUTF("/end");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
        setAuthenticated(false);


    }

    public void connect(){
        try {
            socket = new Socket(ADDRESS, PORT);

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    //цикл аунтификации
                    while (true) {
                        String str = in.readUTF();

                        if(str.startsWith("/")){
                            if (str.equals("/end")) {
                                break;
                            }
                            if(str.startsWith("/authok")){
                                // поделили полученное служебное сообщение на части и второй элемент записали в никнейм
                                nickname = str.split(" ")[1];
                                createHistory(loginField.getText().trim());
                                setAuthenticated(true); // аунтификация успешна
                                break;
                            }
                            if (str.startsWith("/reg")) {
                                // отправляем полученное служебное сообщение в метод regStatus
                            regController.regStatus(str);
                            }
                        } else {
                            Chat.appendText(str + "\n");
                        }
                    }



                    //цикл работы
                    while (authenticated) { // цикл работает пока аунтификация успешна
                        String str = in.readUTF();

                        if(str.startsWith("/")){
                            if (str.equals("/end")) {
                                setAuthenticated(false);
                                break;
                            }
                            if(str.startsWith("/clientlist")){ // обновляем список пользователей онлайн в clientList
                                String[] token = str.split(" ");
                                Platform.runLater(() -> {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < token.length; i++) {
                                        clientList.getItems().add(token[i]);
                                    }
                                });
                            }
                            if(str.startsWith("/change")){
                                changeNicknameStatus(str);
                            }
                        }
                        else {
                            Chat.appendText(str + "\n");
                            addToHistory(str);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        socket.close();
                        closeFileWriter();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void sendMassageBtn(ActionEvent actionEvent) {

        if (textMassage.getText().length()>0){
            try {
                out.writeUTF(textMassage.getText());
                textMassage.clear();
                textMassage.requestFocus();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    public void clickBtnAuth(ActionEvent actionEvent) {
        if(socket == null || socket.isClosed()){
            connect();
        }

        try {
            String msg = String.format("/auth %s %s",
                    loginField.getText().trim(), passwordField.getText().trim());
                out.writeUTF(msg);
                passwordField.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void setTitle(String nickname){
            String title;
            if(nickname.equals("")){
                title = "Magic Chat";
            } else {
                title = String.format("Magic Chat - %s", nickname);
            }
            Platform.runLater(() -> {
                stage.setTitle(title);
            });
        }

    // метод для выбора из списка пользователей и подготовки шаблона для приватного сообщения
    public void clickClientList(MouseEvent mouseEvent) {
        String receiver = clientList.getSelectionModel().getSelectedItem();
        textMassage.setText("/w " + receiver + " ");
    }

    @FXML
    // нажатие на кнопку "Регистрация" вызывает метод createRegWindow(), чтобы создать окно регистрации
    public void clickBtnReg(ActionEvent actionEvent) {
        if(regStage == null){ // если окно еще не открыто, то создать его. В противном случае просто показать окно
            createRegWindow();
        }
        regStage.show();

    }

    private void createRegWindow(){ // метод создания окна регистрации

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("reg.fxml"));
            Parent root = fxmlLoader.load();
            regStage = new Stage();
            regStage.setTitle("Magic chat registration");
            regStage.setScene(new Scene(root, 700,400));

            regStage.initModality(Modality.APPLICATION_MODAL); // выводит экран на первый план и там держит
            regStage.initStyle(StageStyle.UTILITY); // вид окошка без сворачивания

            regController = fxmlLoader.getController(); // ссылка на экземпляр регконтроллера, который будет создаг
            regController.setController(this); // дали ссылку на наш Client

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // метод для регистрации новых пользователей
    public void tryToReg(String login, String password, String nickname){

        if(socket == null || socket.isClosed()){ // проверяем, что клиент еще не подключен к сокету
            connect();
        }
        // в строку msg передаем служебное сообщение /reg с данными для регистрации (login, password, nickname)
        String msg = String.format("/reg %s %s %s", login, password, nickname);
        //затем отправляем это служебное сообщение в ClientHandler в цикл аунтификации и очищаем поле passwordField
        try {
            out.writeUTF(msg);
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToChangeNickname(String newNickname){
        String msg = String.format("/change %s %s", newNickname, nickname);

        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void clickBtnOpenChangeNickname(ActionEvent actionEvent) {
        changeNicknamePanel.setVisible(true);
        changeNicknamePanel.setManaged(true);
        changeNickname.setVisible(false);
        changeNickname.setManaged(false);
    }


    public void clickBtnChangeNickname(ActionEvent actionEvent) {

        String newNickname = newNicknameField.getText().trim();

        changeNicknamePanel.setVisible(false);
        changeNicknamePanel.setManaged(false);
        changeNickname.setVisible(true);
        changeNickname.setManaged(true);

        tryToChangeNickname(newNickname);

    }


    public void clickBtnCancelChNickname(ActionEvent actionEvent) {
        changeNicknamePanel.setVisible(false);
        changeNicknamePanel.setManaged(false);
        changeNickname.setVisible(true);
        changeNickname.setManaged(true);
    }

    public void changeNicknameStatus(String result){
        String[] str = result.split(" ", 2);
        if(str[0].equals("/change_ok")){
            setTitle(str[1]);
            Chat.appendText("Ник успешно изменен на " + str[1] +"\n");
        }else {
            Chat.appendText("Неуспешно. Ник " + str[1] + " уже занят\n");
        }
    }

    public void showLast100Message(String login){

        Platform.runLater(() -> {
            Chat.clear();
            List<String> history;
        try {
            history = Files.readAllLines(Paths.get(String.format("client/history/history_%s.txt", login)));
            System.out.println(history.size());
            if (history.size() > 100) {
                for (int i = (history.size() - 100); i < history.size(); i++) {
                    Chat.appendText(history.get(i));
                    Chat.appendText("\n");
                }
            } else {
                for (int i = 0; i < history.size(); i++) {
                    Chat.appendText(history.get(i));
                    Chat.appendText("\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        });
    }

    public void addToHistory(String msq){
        try {
            fileHistory.write(msq);
            fileHistory.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createHistory(String login){
        try {
            fileHistory = new FileWriter(String.format("client/history/history_%s.txt", login), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void closeFileWriter(){
        try {
            if(fileHistory != null){
                fileHistory.close();
                System.out.println("Закрыли FileFilter");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}