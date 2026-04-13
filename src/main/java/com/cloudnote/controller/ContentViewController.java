package com.cloudnote.controller;

import com.cloudnote.database.DatabaseConnection;
import com.cloudnote.model.Note;
import com.cloudnote.model.NoteModel;
import com.google.protobuf.NullValue;
import com.mysql.cj.jdbc.ConnectionImpl;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ContentViewController {

    @FXML private TextField searchField;
    @FXML private ListView<NoteModel> listView;
    @FXML private Label statsLabel;
    @FXML private TextField titleField;
    @FXML private TextArea noteContentArea;
    @FXML private BorderPane rootPane;
    @FXML private Button favoriteButton;
    @FXML private Button BtPinned;

    @FXML private VBox editorView;
    @FXML private VBox webViewPanel;
    @FXML private WebView webView;
    @FXML private Button webViewFavoriteButton;

    DatabaseConnection conn = new DatabaseConnection();

    private ObservableList<NoteModel> ListNotesFromUser  = FXCollections.observableArrayList();

    boolean isPinned = false;
    private FilteredList<NoteModel> filteredNotes;
    private NoteModel currentNote;
    private BorderPane parentContainer;
    private int idUser;
    private boolean isEditorMode = true;

    /**
     * Привязывает размеры корневого элемента к размерам родительского контейнера.
     * Используется для адаптации содержимого при изменении размеров окна.
     */
    public void setParentContainer(BorderPane container) {
        this.parentContainer = container;
        if (parentContainer != null) {
            rootPane.prefWidthProperty().bind(parentContainer.widthProperty());
            rootPane.prefHeightProperty().bind(parentContainer.heightProperty());
        }
    }

    @FXML
    public void initialize() {
        resetFavoriteButton();
        getInit();
        filteredNotes = new FilteredList<>(ListNotesFromUser, p -> true);
        listView.setItems(filteredNotes);

        listView.setCellFactory(lv -> new ListCell<NoteModel>() {
            @Override
            protected void updateItem(NoteModel note, boolean empty) {
                super.updateItem(note, empty);
                if (empty || note == null) {
                    setText(null);
                    getStyleClass().removeAll("note-cell", "favorite-cell");
                } else {
                    setText(note.getTitle());
                    getStyleClass().add("note-cell");
                    if (note.isPin()) {
                        getStyleClass().add("favorite-cell");
                    } else {
                        getStyleClass().remove("favorite-cell");
                    }
                }
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                currentNote = newVal;
                titleField.setText(newVal.getTitle());
                noteContentArea.setText(newVal.getText());
                updateFavoriteButton(newVal);
                updateWebViewContent();
            }
        });

        //сюда добавить Listener на автосэйв

        searchField.textProperty().addListener((obs, old, newVal) -> filterNotes());

        updateStats();
        ListNotesFromUser.addListener((javafx.collections.ListChangeListener.Change<? extends NoteModel> c) -> updateStats());

        if (webView != null) {
            updateWebViewContent();
        }
    }

    /**
     * Обработчик кнопки "Избранное" в меню.
     * При нажатии меняет заметки в листе либо на избранные либо на все
     * в зависимости от состояния
     */
    @FXML
    private void handleFavorite() {
        if(isPinned){
            isPinned = false;
            ListNotesFromUser.clear();
            getInit();
            listView.refresh();
            BtPinned.setText("Избранное");
        }
        else{
            isPinned = true;
            ListNotesFromUser.clear();
            getPinnedNotes();
            listView.refresh();
            BtPinned.setText("Все заметки");
        }
    }

    /**
     * Обрабатывает нажатие кнопки избранного (звездочки).
     * Переключает статус текущей выбранной заметки: добавляет её в избранное или удаляет из него.
     * После изменения обновляет внешний вид кнопки, отображает всплывающее уведомление
     * и пересчитывает статистику.
     * Если заметка не выбрана, показывает предупреждение.
     */
    @FXML
    private void handleToggleFavorite() {
        NoteModel selected = listView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            int id = selected.getId();
            String sql = "UPDATE notes SET ispin = ? WHERE id = ?";
            boolean isPin = selected.isPin();
            try(Connection con = conn.getCon();
            PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setBoolean(1, !isPin);
                ps.setInt(2, id);
                int count = ps.executeUpdate();
                if(count > 0) {
                    selected.setPin(!isPin);
                    String status = selected.isPin() ? "добавлена в" : "удалена из";
                    showAlert("Избранное", "Заметка " + status + " ★!");
                    if(isPinned){
                        ListNotesFromUser.clear();
                        getPinnedNotes();
                    }
                    updateStats();
                    listView.refresh();
                    updateFavoriteButton(selected);
                    if (webViewFavoriteButton != null) {
                        if (selected.isPin()) {
                            webViewFavoriteButton.setText("★");
                            webViewFavoriteButton.getStyleClass().add("favorite");
                        } else {
                            webViewFavoriteButton.setText("☆");
                            webViewFavoriteButton.getStyleClass().remove("favorite");
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            showAlert("Внимание", "Выберите заметку!");
        }
    }

    /**
     * Обновляет состояние кнопки избранного в зависимости от статуса заметки.
     */
    private void updateFavoriteButton(NoteModel note) {
        if (note != null && note.isPin()) {
            favoriteButton.setText("★");
            favoriteButton.getStyleClass().add("favorite");

            if (webViewFavoriteButton != null) {
                webViewFavoriteButton.setText("★");
                webViewFavoriteButton.getStyleClass().add("favorite");
            }
        } else {
            favoriteButton.setText("☆");
            favoriteButton.getStyleClass().remove("favorite");

            if (webViewFavoriteButton != null) {
                webViewFavoriteButton.setText("☆");
                webViewFavoriteButton.getStyleClass().remove("favorite");
            }
        }
    }

    /**
     * Сбрасывает состояние кнопки избранного до начального.
     */
    private void resetFavoriteButton() {
        favoriteButton.setText("☆");
        favoriteButton.getStyleClass().remove("favorite");

        if (webViewFavoriteButton != null) {
            webViewFavoriteButton.setText("☆");
            webViewFavoriteButton.getStyleClass().remove("favorite");
        }
    }

    /**
     * Обрабатывает нажатие кнопки "Новая заметка" в меню.
     * создает заметку в бд, но добавляем через экземпляр модели
     * чтобы снизить нагрузку
     */
    @FXML
    private void handleNewNote() {
        currentNote = null;
        titleField.clear();
        noteContentArea.clear();
        listView.getSelectionModel().clearSelection();
        resetFavoriteButton();
        String sql = "INSERT INTO notes (id_user, title, text ,ispin) VALUES (?, ?, ?, ?)";
        try (Connection con = conn.getCon();
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            ps.setInt(1, idUser);
            ps.setString(2, "Новая запись");
            ps.setString(3, "");
            ps.setBoolean(4, false);
            ps.executeUpdate();
            try(ResultSet rs = ps.getGeneratedKeys()){
                if(rs.next()){
                    int id = rs.getInt(1);
                    NoteModel newNote = new NoteModel(
                            id, idUser, "Новая запись",
                            "",
                            LocalDateTime.now(),
                            false);
                    ListNotesFromUser.add(0, newNote);
                    listView.getSelectionModel().select(newNote);
                    titleField.setText(newNote.getTitle());
                    noteContentArea.clear();
                    resetFavoriteButton();
                    updateStats();
                    showAlert("Успех", "Новая заметка создана!");
                }
            }
        } catch (SQLException e) {
            showAlert("Ошибка", "Не удалось создать заметку: " + e.getMessage());
        }
    }


    /**
     * Обрабатывает нажатие кнопки "Удалить заметку" в меню.
     */
    @FXML
    private void handleDeleteNote() {
        NoteModel selected = listView.getSelectionModel().getSelectedItem();

        if (selected != null) {
            int id = selected.getId();
            String sql = "DELETE FROM notes WHERE id = ?";

            try (Connection con = conn.getCon();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, id);
                int rowsAffected = ps.executeUpdate();

                if (rowsAffected > 0) {
                    ListNotesFromUser.remove(selected);
                    //тут был метод очищения редактора
                    showAlert("Успех", "Заметка удалена!");
                    updateStats();
                } else {
                    showAlert("Ошибка", "Заметка не была удалена!");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Ошибка БД", "Не удалось удалить заметку: " + e.getMessage());
            }

        } else {
            showAlert("Внимание", "Выберите заметку для удаления!");
        }
    }

    /**
     * Обрабатывает нажатие кнопки "Настройки" в меню.
     */
    @FXML
    private void handleSettings() {
        showAlert("Настройки", "Тема: Тёмная\nСтиль: CloudNote\nВерсия: 1.0");
    }

    /**
     * Переключает между текстовым редактором и WebView
     */
    @FXML
    private void handleToggleWebView() {
        isEditorMode = !isEditorMode;

        if (isEditorMode) {
            if (editorView != null && webViewPanel != null) {
                editorView.setVisible(true);
                editorView.setManaged(true);
                webViewPanel.setVisible(false);
                webViewPanel.setManaged(false);
            }
            noteContentArea.requestFocus();
        } else {
            updateWebViewContent();

            if (editorView != null && webViewPanel != null) {
                editorView.setVisible(false);
                editorView.setManaged(false);
                webViewPanel.setVisible(true);
                webViewPanel.setManaged(true);
            }
            searchField.getParent().requestFocus();
        }
    }

    /**
     * Обновляет содержимое WebView на основе текущей заметки
     */
    private void updateWebViewContent() {
        if (webView == null) return;

        String title = titleField.getText();
        String content = noteContentArea.getText();

        if (title == null) title = "";
        if (content == null) content = "";

        String htmlContent = String.format("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                
                body {
                    background-color: #0D1117;
                    font-family: -fx-system, 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                    padding: 0;
                    margin: 0;
                    height: 100%%;
                    overflow-x: hidden;
                }
                
                .webview-container {
                    width: 100%%;
                    height: 100%%;
                    background-color: #161B22;
                    border-radius: 8px;
                    border: 1px solid #21262D;
                    overflow: auto;
                    display: flex;
                    flex-direction: column;
                    box-sizing: border-box;
                }
                
                .note-header {
                    background-color: #0D1117;
                    border-bottom: 1px solid #21262D;
                    padding: 20px 24px;
                    flex-shrink: 0;
                }
                
                .note-title {
                    color: #00E5FF;
                    font-size: 18px;
                    font-weight: bold;
                    margin: 0;
                    word-wrap: break-word;
                }
                
                .note-content {
                    padding: 24px;
                    color: #C9D1D9;
                    font-size: 13px;
                    line-height: 1.6;
                    flex: 1;
                    overflow-y: auto;
                }
                
                .note-content p {
                    margin-bottom: 14px;
                    color: #C9D1D9;
                }
                
                .note-content h1 {
                    color: #00E5FF;
                    font-size: 24px;
                    margin: 20px 0 14px 0;
                    border-bottom: 1px solid #21262D;
                    padding-bottom: 6px;
                }
                
                .note-content h2 {
                    color: #00E5FF;
                    font-size: 20px;
                    margin: 18px 0 10px 0;
                }
                
                .note-content h3 {
                    color: #C9D1D9;
                    font-size: 18px;
                    margin: 14px 0 8px 0;
                }
                
                .note-content strong {
                    color: #00E5FF;
                }
                
                .note-content code {
                    background-color: #0D1117;
                    color: #00E5FF;
                    padding: 2px 5px;
                    border-radius: 4px;
                    font-family: 'Courier New', monospace;
                    font-size: 12px;
                    border: 1px solid #21262D;
                }
                
                .note-content pre {
                    background-color: #0D1117;
                    border: 1px solid #21262D;
                    border-radius: 6px;
                    padding: 12px;
                    overflow-x: auto;
                    margin: 12px 0;
                }
                
                .note-content pre code {
                    background-color: transparent;
                    border: none;
                    padding: 0;
                }
                
                .note-content blockquote {
                    border-left: 3px solid #00E5FF;
                    margin: 12px 0;
                    padding-left: 16px;
                    color: #8B949E;
                }
                
                .note-content ul, .note-content ol {
                    margin: 10px 0;
                    padding-left: 24px;
                }
                
                .note-content li {
                    margin: 4px 0;
                }
                
                .empty-note {
                    text-align: center;
                    padding: 40px 20px;
                    color: #8B949E;
                }
                
                ::-webkit-scrollbar {
                    width: 0;
                    height: 0;
                    background: transparent;
                }
                
                ::-webkit-scrollbar-track {
                    background: #0D1117;
                }
                
                ::-webkit-scrollbar-thumb {
                    background: #30363D;
                    border-radius: 4px;
                }
                
                ::-webkit-scrollbar-thumb:hover {
                    background: #3D444D;
                }
            </style>
        </head>
        <body>
            <div class="webview-container">
                <div class="note-header">
                    <div class="note-title">%s</div>
                </div>
                <div class="note-content">
                    %s
                </div>
            </div>
        </body>
        </html>
        """, escapeHtml(title), convertToHtml(content));

        webView.getEngine().loadContent(htmlContent);
    }

    /**
     * Экранирует HTML специальные символы
     */
    private String escapeHtml(String text) {
        if (text == null || text.isEmpty()) return "Без заголовка";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Конвертирует текст с Markdown-подобным синтаксисом в HTML
     */
    private String convertToHtml(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "<div class='empty-note'>✏️ Нет содержимого. Начните писать заметку...</div>";
        }

        String[] lines = text.split("\\n");
        StringBuilder result = new StringBuilder();

        boolean inList = false;
        boolean inBlockquote = false;

        for (String line : lines) {
            if (line.trim().startsWith(">")) {
                if (!inBlockquote) {
                    if (inList) {
                        result.append("</ul>");
                        inList = false;
                    }
                    result.append("<blockquote>");
                    inBlockquote = true;
                }
                String quoteText = line.trim().substring(1).trim();
                quoteText = escapeHtml(quoteText);
                quoteText = applyInlineFormatting(quoteText);
                result.append(quoteText).append("<br/>");
                continue;
            } else if (inBlockquote) {
                result.append("</blockquote>");
                inBlockquote = false;
            }

            // Экранируем HTML для остального текста
            String htmlLine = escapeHtml(line);

            // Заголовки
            if (htmlLine.matches("^#{1,3} .*")) {
                if (inList) {
                    result.append("</ul>");
                    inList = false;
                }
                if (htmlLine.startsWith("### ")) {
                    result.append("<h3>").append(htmlLine.substring(4)).append("</h3>");
                } else if (htmlLine.startsWith("## ")) {
                    result.append("<h2>").append(htmlLine.substring(3)).append("</h2>");
                } else if (htmlLine.startsWith("# ")) {
                    result.append("<h1>").append(htmlLine.substring(2)).append("</h1>");
                }
                continue;
            }

            // Списки
            if (htmlLine.matches("^[-*] .*")) {
                if (!inList) {
                    result.append("<ul>");
                    inList = true;
                }
                String liText = htmlLine.substring(2);
                liText = applyInlineFormatting(liText);
                result.append("<li>").append(liText).append("</li>");
                continue;
            } else if (inList) {
                result.append("</ul>");
                inList = false;
            }

            // Горизонтальные линии
            if (htmlLine.matches("^---+$") || htmlLine.matches("^\\*\\*\\*+$")) {
                result.append("<hr/>");
                continue;
            }

            // Применяем форматирование
            htmlLine = applyInlineFormatting(htmlLine);

            // Обычный текст
            if (!htmlLine.trim().isEmpty()) {
                result.append("<p>").append(htmlLine).append("</p>");
            } else {
                result.append("<br/>");
            }
        }

        // Закрываем открытые теги
        if (inList) result.append("</ul>");
        if (inBlockquote) result.append("</blockquote>");

        return result.toString();
    }

    private String applyInlineFormatting(String text) {
        if (text == null) return "";

        // Жирный текст
        String result = text.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
        result = result.replaceAll("__(.*?)__", "<strong>$1</strong>");

        // Курсив
        result = result.replaceAll("\\*(.*?)\\*", "<em>$1</em>");
        result = result.replaceAll("_(.*?)_", "<em>$1</em>");

        // Зачеркнутый
        result = result.replaceAll("~~(.*?)~~", "<del>$1</del>");

        // Код
        result = result.replaceAll("`(.*?)`", "<code>$1</code>");

        // Ссылки
        result = result.replaceAll("\\[(.*?)\\]\\((.*?)\\)", "<a href='$2'>$1</a>");

        return result;
    }

    /**
     * Фильтрует заметки по текстовому запросу из поля поиска.
     * Поиск выполняется по заголовку и содержимому заметки без учета регистра.
     * Пустой запрос отображает все заметки.
     */
    private void filterNotes() {
        String filter = searchField.getText().toLowerCase();
        filteredNotes.setPredicate(note -> {
            if (filter == null || filter.isEmpty()) return true;
            return note.getTitle().toLowerCase().contains(filter) ||
                    note.getText().toLowerCase().contains(filter);
        });
    }

    /**
     * Обновляет статистику заметок: общее количество и количество избранных.
     * Данные получаются из базы данных.
     */
    private void updateStats() {
        int CountPinned = 0;

        String sql = "SELECT COUNT(*) FROM notes WHERE isPin = TRUE AND id_user = ?";
        try(Connection con = conn.getCon();
            PreparedStatement ps = con.prepareStatement(sql)){
            ps.setInt(1, idUser);
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()){
                    CountPinned = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        int total = ListNotesFromUser.size();
        statsLabel.setText("📊 Всего: " + total + " | ⭐ Избранных: " + CountPinned);
    }

    /**
     * Отображает информационное диалоговое окно с заданным заголовком и текстом.
     * Применяет к окну стили из CSS-файла.
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialogPane.getStyleClass().add("alert-dialog");
        alert.showAndWait();
    }


    /**
     * Загружает все заметки текущего пользователя из базы данных.
     * Полученные заметки добавляются в список ListNotesFromUser.
     */
    private void getInit(){
        String sql = "SELECT * FROM notes WHERE id_user = ?";
        try(Connection con = conn.getCon();
            PreparedStatement ps = con.prepareStatement(sql)){
            ps.setInt(1, idUser);
            try(ResultSet rs = ps.executeQuery()){
                while(rs.next()){
                    NoteModel model = new NoteModel(
                            rs.getInt("id"),
                            rs.getInt("id_user"),
                            rs.getString("title"),
                            rs.getString("text"),
                            rs.getTimestamp("date") != null ? rs.getTimestamp("date").toLocalDateTime() : null,
                            rs.getBoolean("ispin"));

                    ListNotesFromUser.add(model);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void getPinnedNotes(){
        String sql = "SELECT * FROM notes WHERE id_user = ? and isPin = TRUE";
        try(Connection con = conn.getCon();
            PreparedStatement ps = con.prepareStatement(sql)){
            ps.setInt(1, idUser);
            try(ResultSet rs = ps.executeQuery()){
                while(rs.next()){
                    NoteModel model = new NoteModel(
                            rs.getInt("id"),
                            rs.getInt("id_user"),
                            rs.getString("title"),
                            rs.getString("text"),
                            rs.getTimestamp("date") != null ? rs.getTimestamp("date").toLocalDateTime() : null,
                            rs.getBoolean("ispin"));

                    ListNotesFromUser.add(model);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Устанавливает идентификатор пользователя, полученный из окна авторизации.
     * Инициализирует загрузку заметок и обновляет отображение списка.
     */
    public void setIdUser(int idUser){
        this.idUser = idUser;
        System.out.println("id user = " + idUser);
        getInit();

        filteredNotes = new FilteredList<>(ListNotesFromUser, p -> true);
        listView.setItems(filteredNotes);
        updateStats();
    }

}