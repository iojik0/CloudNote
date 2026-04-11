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
    DatabaseConnection conn = new DatabaseConnection();

    private ObservableList<NoteModel> ListNotesFromUser  = FXCollections.observableArrayList();

    boolean isPinned = false;
    private FilteredList<NoteModel> filteredNotes;
    private NoteModel currentNote;
    private BorderPane parentContainer;
    private int idUser;

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
            }
        });

        searchField.textProperty().addListener((obs, old, newVal) -> filterNotes());

        updateStats();
        ListNotesFromUser.addListener((javafx.collections.ListChangeListener.Change<? extends NoteModel> c) -> updateStats());
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
        } else {
            favoriteButton.setText("☆");
            favoriteButton.getStyleClass().remove("favorite");
        }
    }

    /**
     * Сбрасывает состояние кнопки избранного до начального.
     */
    private void resetFavoriteButton() {
        favoriteButton.setText("☆");
        favoriteButton.getStyleClass().remove("favorite");
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
                    ListNotesFromUser.add(newNote);
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
                    handleClear();
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
    //убрать как и кнопку
    /**
     * Отменяет изменения в редакторе.
     * Если заметка выбрана - восстанавливает её сохраненные данные.
     * Если заметка не выбрана - очищает поля и снимает выделение.
     */
    @FXML
    private void handleClear() {
        if (currentNote != null && listView.getSelectionModel().getSelectedItem() != null) {
            titleField.setText(currentNote.getTitle());
            noteContentArea.setText(currentNote.getText());
        } else {
            titleField.clear();
            noteContentArea.clear();
            currentNote = null;
            listView.getSelectionModel().clearSelection();
        }
        resetFavoriteButton();
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