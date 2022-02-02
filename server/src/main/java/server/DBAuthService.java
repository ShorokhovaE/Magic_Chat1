package server;

import java.sql.*;

public class DBAuthService implements AuthService {

    private static Connection connection;   // чтобы подключиться
    private static Statement stm;           // чтобы делать запросы

    public static Statement getStm() {
        return stm;
    }

    public static Connection getConnection() {
        return connection;
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        try {
            ResultSet rs = stm.executeQuery("SELECT * FROM clients;");
            while (rs.next()) {
                if (rs.getString("login").equals(login) && rs.getString("password").equals(password)) {
                    return rs.getString("nickname");
                }
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean registration(String login, String password, String nickname) {

        try {
            ResultSet rs = stm.executeQuery("SELECT * FROM clients;");
            while (rs.next()) {
                if (rs.getString("login").equals(login) || rs.getString("nickname").equals(nickname)) {
                    return false;
                }
            }
            addInDB(login, password, nickname);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    //метод для добавления учетки в БД
    public void addInDB(String login, String password, String nickname) throws SQLException {
        PreparedStatement psInsert =
                   connection.prepareStatement("INSERT INTO clients (login, password, nickname) VALUES ( ? , ? , ? );");
           psInsert.setString(1, login);
           psInsert.setString(2, password);
           psInsert.setString(3, nickname);
           psInsert.executeUpdate();
    }


    public static void connect() throws Exception {

        Class.forName("org.sqlite.JDBC");   //используется для загрузки драйвера
        connection = DriverManager.getConnection("jdbc:sqlite:main.db");    //подключение драйвера
        stm = connection.createStatement(); //после получения connection, можем получить stm, чтобы делать запросы
        System.out.println("БД подключена");

    }

    public static void disconnect(){
        try {
            stm.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }




    }


}
