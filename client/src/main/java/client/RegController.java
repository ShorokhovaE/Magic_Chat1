package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class RegController {

    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public TextField nicknameField;
    @FXML
    public TextArea regChat;

    private Client controller;

    public void setController(Client controller) {
        this.controller = controller;
    }

    @FXML // при нажатии на кнопку "регистрация" запускается этот метод
    public void clickBtnReg(ActionEvent actionEvent) {
        // из поля loginField берем значение логина и с помощью trim убираем пробелы
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        String nickname = nicknameField.getText().trim();

        //вызываем у клиента метод tryToReg для регистрации и отправляем полученные из полей данные (login, password, nickname)
        controller.tryToReg(login, password, nickname);

    }

    public void regStatus(String result){
        if(result.equals("/reg_ok")){
            regChat.appendText("Регистрация прошла успешно. Теперь ты в чате!\n");
        }else {
            regChat.appendText("Регистрация не выполнена. Логин или никнейм заняты\n");
        }

    }
}
