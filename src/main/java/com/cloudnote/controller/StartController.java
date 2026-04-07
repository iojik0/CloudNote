package com.cloudnote.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

public class StartController {
    // Панели (Pane)
    @FXML private Pane PMain;
    @FXML private Pane PFirst;
    @FXML private Pane PSighUp;
    @FXML private Pane PSighIn;

    // Кнопки на главном экране
    @FXML private Button BtSighIn;
    @FXML private Button BtSighUp;

    // Поля для регистрации
    @FXML private TextField TfSighUpLogin;
    @FXML private TextField TfSighUpPass;
    @FXML private Button BtSighUpOk;
    @FXML private Button BtSighUpCancel;

    // Поля для входа
    @FXML private TextField TfSighInLogin;
    @FXML private TextField TfSighInPass;
    @FXML private Button BtSighInOk;
    @FXML private Button BtSighInCancel;

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

    @FXML
    private void handleClickSighInOk() {
        PFirst.setVisible(true);
        PSighIn.setVisible(false);
        PSighUp.setVisible(false);
    }

    @FXML
    private void handleClickSighInCancel() {
        PFirst.setVisible(true);
        PSighIn.setVisible(false);
        PSighUp.setVisible(false);
    }

    @FXML
    private void handleClickSighUpOk() {
        PFirst.setVisible(true);
        PSighIn.setVisible(false);
        PSighUp.setVisible(false);
    }

    @FXML
    private void handleClickSighUpCancel() {
        PFirst.setVisible(true);
        PSighIn.setVisible(false);
        PSighUp.setVisible(false);
    }
}