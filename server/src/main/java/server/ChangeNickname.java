package server;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ChangeNickname implements ChangeNick {


    public boolean searchNickname(String newNickname) {

        try {
            ResultSet rs = DBAuthService.getStm().executeQuery("SELECT nickname FROM clients;");
            while (rs.next()) {
                if (rs.getString("nickname").equals(newNickname)) {
                    return false;
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    //метод для изменения никнейма в БД
    public boolean changeNickname(String newNickname, String nickname) {
        try {
            PreparedStatement psChangeNickname =
                    DBAuthService.getConnection().prepareStatement("UPDATE clients SET nickname = ? WHERE nickname = ? ;");

            if (searchNickname(newNickname)) {
                psChangeNickname.setString(1, newNickname);
                psChangeNickname.setString(2, nickname);
                psChangeNickname.executeUpdate();
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}

