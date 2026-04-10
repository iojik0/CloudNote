package com.cloudnote.controller;

import com.cloudnote.database.DatabaseConnection;
import com.cloudnote.model.UserModel;
import com.cloudnote.utils.PasswordHasher;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.sql.*;

public class StartController {
    DatabaseConnection conn = new DatabaseConnection();


    ObservableList<UserModel> listUser = FXCollections.observableArrayList();

    boolean isUsernameValid = false;
    boolean isPasswordValid = false;
    boolean isLoginValid = false;

    // Панели (Pane)
    @FXML private Pane PMain;
    @FXML private Pane PFirst;
    @FXML private Pane PSignUp;
    @FXML private Pane PSignIn;
    @FXML private BorderPane BpContent;

    // Кнопки на главном экране
    @FXML private Button BtSignIn;
    @FXML private Button BtSignUp;

    // Поля для регистрации
    @FXML private TextField TfSignUpUsername;
    @FXML private TextField TfSignUpLogin;
    @FXML private TextField TfSignUpPass;
    @FXML private Button BtSignUpOk;
    @FXML private Button BtSignUpCancel;
    @FXML private Label LUsernameStatus;
    @FXML private Label LLoginStatus;
    @FXML private Label LPasswordStatus;
    // Поля для входа
    @FXML private Label LError;
    @FXML private TextField TfSignInLogin;
    @FXML private TextField TfSignInPass;
    @FXML private Button BtSignInOk;
    @FXML private Button BtSignInCancel;


    public void initialize() {
        valueChecking();
        PMain.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
    }

    // Методы-обработчики событий
    @FXML
    private void handleClickSignIn() {
        PFirst.setVisible(false);
        PSignIn.setVisible(true);
        PSignUp.setVisible(false);
    }

    @FXML
    private void handleClickSignUp() {
        PFirst.setVisible(false);
        PSignIn.setVisible(false);
        PSignUp.setVisible(true);

    }




    // обработка кнопки для входа
    @FXML
    private void handleClickSignInOk() {
        String login = TfSignInLogin.getText().trim();
        String pass = TfSignInPass.getText().trim();

        // проверяем на пустоту ввода
        if(login.isEmpty() || pass.isEmpty()) {
            LError.setText("заполните все поля");
            LError.setVisible(true);
            return;
        }
        // записываем пароль в виде хэша
        String sql = "SELECT * FROM users WHERE login = ? AND password = ?";
        String hashPass = PasswordHasher.hashPassword(pass);
        try (Connection con = conn.getCon();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, login);
            ps.setString(2, hashPass);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()){
                    String username = rs.getString("username");
                    TfSignInLogin.clear();
                    TfSignInPass.clear();
                    // переход в новое окно
                    PSignIn.setVisible(false);
                    PSignUp.setVisible(false);
                    LError.setVisible(false);
                    getContent();
                    BpContent.setVisible(true);

                }
                else{
                    LError.setText("неправильный логин или пароль");
                    LError.setVisible(true);
                    TfSignInLogin.clear();
                    TfSignInPass.clear();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void handleClickSignInCancel() {
        PFirst.setVisible(true);
        PSignIn.setVisible(false);
        PSignUp.setVisible(false);
    }

    private void valueChecking() {
        //проверка юзернейма
        TfSignUpUsername.textProperty().addListener((observable, oldValue, newValue) -> {
            String username = newValue.trim();

            if (username.length() < 4) {
                LUsernameStatus.setText("имя должно состоять минимум из 4 символов");
                LUsernameStatus.setVisible(true);
                isUsernameValid = false;
                return;
            }
            String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
            try (Connection con = conn.getCon();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, username);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        if (count > 0) {
                            LUsernameStatus.setText("имя занято");
                            LUsernameStatus.setVisible(true);
                            isUsernameValid = false;
                        } else {
                            LUsernameStatus.setVisible(false);
                            isUsernameValid = true;
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        // проверка логина
        TfSignUpLogin.textProperty().addListener((observable, oldValue, newValue) -> {
            String login = newValue.trim();

            if (login.length() < 5) {
                LLoginStatus.setText("логин должен состоять минимум из 5 символов");
                LLoginStatus.setVisible(true);
                isLoginValid = false;
                return;
            }
            String sql = "SELECT COUNT(*) FROM users WHERE login = ?";
            try (Connection con = conn.getCon();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, login);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        if (count > 0) {
                            LLoginStatus.setText("логин занят");
                            LLoginStatus.setVisible(true);
                            isLoginValid = false;
                        } else {
                            LLoginStatus.setVisible(false);
                            isLoginValid = true;
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        // проверка пароля
        TfSignUpPass.textProperty().addListener((observable, oldValue, newValue) -> {
            String pass = TfSignUpPass.getText().trim();
            if (newValue.length() < 5) {
                LPasswordStatus.setText("пароль должен состоять минимум из 5 символов");
                LPasswordStatus.setVisible(true);
                isPasswordValid = false;
            } else {
                LPasswordStatus.setVisible(false);
                isPasswordValid = true;
            }
        });
    }



    @FXML
    private void handleClickSignUpOk() {
        if(isPasswordValid && isUsernameValid && isLoginValid){
            String sql = "INSERT INTO users (username, login, password) VALUES (?, ?, ?)";
            try (Connection con = conn.getCon();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, TfSignUpUsername.getText());
                ps.setString(2, TfSignUpLogin.getText());
                ps.setString(3, PasswordHasher.hashPassword(TfSignUpPass.getText()));

                int rowsAdd = ps.executeUpdate();
                if(rowsAdd > 0){
                    System.out.println("Регистрация прошла успешно");
                    handleClickSignUpCancel(); // очистить поля после регистрации
                    //переход в основное окно
                    getContent();
                    BpContent.setVisible(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleClickSignUpCancel() {
        PFirst.setVisible(true);
        PSignIn.setVisible(false);
        PSignUp.setVisible(false);
        TfSignUpUsername.clear();
        TfSignUpLogin.clear();
        TfSignUpPass.clear();
        PSignUp.setVisible(false);
    }
    public void getContent(){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ContentView.fxml"));
            AnchorPane loadedPane = loader.load(); // Загружаем как AnchorPane

            // 2. Очищаем старый контент (опционально)
            BpContent.getChildren().clear();

            // 3. Добавляем загруженный Pane в текущий
            BpContent.getChildren().add(loadedPane);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
