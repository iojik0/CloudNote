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
    PreparedStatement ps;
    Statement st;
    ResultSet rs;
    Connection con;

    ObservableList<UserModel> listUser = FXCollections.observableArrayList();

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

    // Поля для входа
    @FXML private Label LError;
    @FXML private TextField TfSighInLogin;
    @FXML private TextField TfSighInPass;
    @FXML private Button BtSighInOk;
    @FXML private Button BtSighInCancel;


    public void initialize(URL url, ResourceBundle rb) {


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
        String hashPass = PasswordHasher.hashPassword(pass);
        try {
            String sql = "SELECT * FROM users WHERE login = ? AND password = ?";
            con = conn.getCon();
            ps = con.prepareStatement(sql);
            ps.setString(1, login);
            ps.setString(2, hashPass);
            rs = ps.executeQuery();
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

    @FXML
    private void handleClickSighUpOk() {

    }

    @FXML
    private void handleClickSighUpCancel() {
        PFirst.setVisible(true);
        PSighIn.setVisible(false);
        PSighUp.setVisible(false);
    }
}