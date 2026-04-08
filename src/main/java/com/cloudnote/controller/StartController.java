package com.cloudnote.controller;

import com.cloudnote.database.DatabaseConnection;
import com.cloudnote.model.UserModel;
import com.cloudnote.utils.PasswordHasher;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ResourceBundle;

public class StartController {
    DatabaseConnection conn = new DatabaseConnection();


    ObservableList<UserModel> listUser = FXCollections.observableArrayList();

    boolean isUsernameValid = false;
    boolean isPasswordValid = false;
    boolean isLoginValid = false;

    // Панели (Pane)
    @FXML private Pane PMain;
    @FXML private Pane PFirst;
    @FXML private Pane PSighUp;
    @FXML private Pane PSighIn;

    // Кнопки на главном экране
    @FXML private Button BtSighIn;
    @FXML private Button BtSighUp;

    // Поля для регистрации
    @FXML private TextField TfSighUpUsername;
    @FXML private TextField TfSighUpLogin;
    @FXML private TextField TfSighUpPass;
    @FXML private Button BtSighUpOk;
    @FXML private Button BtSighUpCancel;
    @FXML private Label LUsernameStatus;
    @FXML private Label LLoginStatus;
    @FXML private Label LPasswordStatus;
    // Поля для входа
    @FXML private Label LError;
    @FXML private TextField TfSighInLogin;
    @FXML private TextField TfSighInPass;
    @FXML private Button BtSighInOk;
    @FXML private Button BtSighInCancel;


    public void initialize() {
        valueChecking();
    }

    // Методы-обработчики событий
    @FXML
    private void handleClickSighIn() {
        PFirst.setVisible(false);
        PSighIn.setVisible(true);
        PSighUp.setVisible(false);
    }

    @FXML
    private void handleClickSighUp() {
        PFirst.setVisible(false);
        PSighIn.setVisible(false);
        PSighUp.setVisible(true);

    }




    // обработка кнопки для входа
    @FXML
    private void handleClickSighInOk() {
        String login = TfSighInLogin.getText().trim();
        String pass = TfSighInPass.getText().trim();

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
                    TfSighInLogin.clear();
                    TfSighInPass.clear();
                    // переход в новое окно
                    PFirst.setVisible(true);
                    PSighIn.setVisible(false);
                    PSighUp.setVisible(false);
                    LError.setVisible(false);
                }
                else{
                    LError.setText("неправильный логин или пароль");
                    LError.setVisible(true);
                    TfSighInLogin.clear();
                    TfSighInPass.clear();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void handleClickSighInCancel() {
        PFirst.setVisible(true);
        PSighIn.setVisible(false);
        PSighUp.setVisible(false);
    }

    private void valueChecking() {
        //проверка юзернейма
        TfSighUpUsername.textProperty().addListener((observable, oldValue, newValue) -> {
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
        TfSighUpLogin.textProperty().addListener((observable, oldValue, newValue) -> {
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
        TfSighUpPass.textProperty().addListener((observable, oldValue, newValue) -> {
            String pass = TfSighUpPass.getText().trim();
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
    private void handleClickSighUpOk() {
        if(isPasswordValid && isUsernameValid && isLoginValid){
            String sql = "INSERT INTO users (username, login, password) VALUES (?, ?, ?)";
            try (Connection con = conn.getCon();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, TfSighUpUsername.getText());
                ps.setString(2, TfSighUpLogin.getText());
                ps.setString(3, PasswordHasher.hashPassword(TfSighUpPass.getText()));

                int rowsAdd = ps.executeUpdate();
                if(rowsAdd > 0){
                    System.out.println("Регистрация прошла успешно");
                    handleClickSighUpCancel(); // очистить поля после регистрации
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleClickSighUpCancel() {
        PFirst.setVisible(true);
        PSighIn.setVisible(false);
        PSighUp.setVisible(false);
        TfSighUpUsername.clear();
        TfSighUpLogin.clear();
        TfSighUpPass.clear();
        PSighUp.setVisible(false);
    }
}