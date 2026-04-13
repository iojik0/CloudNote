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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

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

    @FXML private StackPane iconContainer;

    public void initialize() {
        valueChecking();
        PMain.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        loadIcon(iconContainer, 100);
    }

    private void loadIcon(StackPane container, int size) {
        try {
            Image image = new Image(getClass().getResourceAsStream("/icons/CloudNoteIcon.png"));
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(size);
            imageView.setFitHeight(size);
            imageView.setPreserveRatio(true);
            container.getChildren().add(imageView);
        } catch (Exception e) {
            System.out.println("Иконка не найдена!");
        }
    }

    /**
     * Переключает интерфейс на панель авторизации.
     * Скрывает начальную панель и панель регистрации, отображает форму входа.
     */
    @FXML
    private void handleClickSignIn() {
        PFirst.setVisible(false);
        PSignIn.setVisible(true);
        PSignUp.setVisible(false);
    }

    /**
     * Переключает интерфейс на панель регистрации нового пользователя.
     * Скрывает начальную панель и панель входа, отображает форму регистрации.
     */
    @FXML
    private void handleClickSignUp() {
        PFirst.setVisible(false);
        PSignIn.setVisible(false);
        PSignUp.setVisible(true);

    }

    /**
     * Обрабатывает попытку входа пользователя в систему.
     * Проверяет заполненность полей, сверяет логин и хэш пароля с данными в БД.
     * При успешной авторизации очищает форму, скрывает панель входа и загружает основное окно приложения.
     * При ошибке отображает соответствующее сообщение и очищает поля ввода.
     */
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
                    String loginUser = rs.getString("login");
                    TfSignInLogin.clear();
                    TfSignInPass.clear();
                    // переход в новое окно
                    PSignIn.setVisible(false);
                    PSignUp.setVisible(false);
                    LError.setVisible(false);
                    getContent(loginUser);
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

    /**
     * Отменяет процесс авторизации и возвращает пользователя к начальному экрану.
     */
    @FXML
    private void handleClickSignInCancel() {
        PFirst.setVisible(true);
        PSignIn.setVisible(false);
        PSignUp.setVisible(false);
    }

    /**
     * Настраивает валидацию полей регистрационной формы в реальном времени.
     * Проверяет:
     * - Имя пользователя: минимальная длина 4 символа, уникальность в БД
     * - Логин: минимальная длина 5 символов, уникальность в БД
     * - Пароль: минимальная длина 5 символов
     * При ошибках отображает соответствующие сообщения и обновляет флаги валидности.
     */
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

    /**
     * Обрабатывает подтверждение регистрации нового пользователя.
     * Проверяет валидность введенных данных (пароль, имя пользователя, логин).
     * При успешной проверке добавляет пользователя в базу данных и создает
     * приветственную заметку,
     * очищает форму и переходит к основному окну приложения.
     */
    @FXML
    private void handleClickSignUpOk() {
        if (isPasswordValid && isUsernameValid && isLoginValid) {
            String sql = "INSERT INTO users (username, login, password) VALUES (?, ?, ?)";
            String login = TfSignUpLogin.getText().trim();

            try (Connection con = conn.getCon();
                 PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, TfSignUpUsername.getText());
                ps.setString(2, TfSignUpLogin.getText());
                ps.setString(3, PasswordHasher.hashPassword(TfSignUpPass.getText()));

                int rowsAdd = ps.executeUpdate();

                if (rowsAdd > 0) {
                    // Получаем ID нового пользователя
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            int userId = rs.getInt(1);
                            System.out.println("Регистрация прошла успешно. ID: " + userId);

                            // Создаём приветственную заметку
                            createWelcomeNote(con, userId);
                            handleClickSignUpCancel();
                            getContent(login);
                            BpContent.setVisible(true);
                        }
                    }
                }

            } catch (SQLException e) {
                System.out.println("Ошибка, Не удалось зарегистрироваться: " + e.getMessage());
            }
        }
    }
    // создание приветственной заметки
    private void createWelcomeNote(Connection con, int userId) {
        String sql = "INSERT INTO notes (id_user, title, text, ispin) VALUES (?, ?, ?, ?)";
        // добавить в будущем
        //💡 Совет: Попробуйте Markdown в этой заметке!
        //        Напишите **жирный текст** или *курсив* - и увидите результат
        //в окне предпросмотра.
        String welcomeText = """
        ☁️ Ваше личное облачное пространство для заметок.
        
        ✨ Особенности:
        • Тёмная тема
        • Поиск по заметкам
        • Избранное
        • Быстрое редактирование
        • Просмотр заметки в формате markdown
       
       
        Made with ☕ by iojik0 & LanaRaw
        """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, "Добро пожаловать в CloudNote!");
            ps.setString(3, welcomeText);
            ps.setBoolean(4, false);

            ps.executeUpdate();
            System.out.println("Приветственная заметка создана");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Отменяет процесс регистрации и возвращает пользователя к начальному экрану.
     * Очищает все поля ввода и скрывает панель регистрации.
     */
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

    /**
     * Загружает и отображает основное содержимое приложения (окно с заметками).
     * Создает контроллер для ContentView, передает ему идентификатор пользователя
     * и привязывает размеры загруженного контента к родительскому контейнеру.
     */
    public void getContent(String login){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ContentView.fxml"));
            BorderPane loadedPane = loader.load(); // Загружаем как AnchorPane

            ContentViewController contentController = loader.getController();
            contentController.setParentContainer(BpContent);
            int id = getUserId(login);
            contentController.setIdUser(id);
            loadedPane.prefWidthProperty().bind(BpContent.widthProperty());
            loadedPane.prefHeightProperty().bind(BpContent.heightProperty());
            BpContent.setCenter(loadedPane);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // передаем айди юзера в другой контроллер
    public int getUserId(String login) {
        String sql = "SELECT id FROM users WHERE login = ?";

        try (Connection con = conn.getCon();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, login);
            try(ResultSet rs = ps.executeQuery()){
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }
}
