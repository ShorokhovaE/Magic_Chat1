package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.lang.management.PlatformLoggingMXBean;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
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

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private final String ADDRESS = "localhost";
    private final int PORT = 8189;

    private boolean authenticated;
    private String nickname;
    private Stage stage;

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;

        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);

        msgPanel.setVisible(authenticated);
        msgPanel.setManaged(authenticated);

        if(!authenticated){
            nickname = "";
        }

        setTitle(nickname);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) textMassage.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                System.out.println("До свидания!");
                if(socket != null && socket.isClosed()){
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
                                nickname = str.split(" ")[1];
                                setAuthenticated(true);
                                break;
                            }
                        } else {
                            Chat.appendText(str + "\n");
                        }
                    }



                    //цикл работы
                    while (authenticated) {
                        String str = in.readUTF();

                        if (str.equals("/end")) {
                            setAuthenticated(false);
                            break;
                        }

                        Chat.appendText(str + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
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
    }