package com.cloudnote.controller;

import com.cloudnote.model.Note;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;

public class ContentViewController {

    @FXML private TextField searchField;
    @FXML private ListView<Note> listView;
    @FXML private Label statsLabel;
    @FXML private TextField titleField;
    @FXML private TextArea noteContentArea;
    @FXML
    private BorderPane rootPane;

    private ObservableList<Note> notesList = FXCollections.observableArrayList();
    private FilteredList<Note> filteredNotes;
    private Note currentNote;
    private BorderPane parentContainer;

    public void setParentContainer(BorderPane container) {
        this.parentContainer = container;
        if (parentContainer != null) {
            rootPane.prefWidthProperty().bind(parentContainer.widthProperty());
            rootPane.prefHeightProperty().bind(parentContainer.heightProperty());
        }
    }

    @FXML
    public void initialize() {
        loadSampleNotes();
        filteredNotes = new FilteredList<>(notesList, p -> true);
        listView.setItems(filteredNotes);

        listView.setCellFactory(lv -> new ListCell<Note>() {
            @Override
            protected void updateItem(Note note, boolean empty) {
                super.updateItem(note, empty);
                if (empty || note == null) {
                    setText(null);
                    getStyleClass().removeAll("note-cell", "favorite-cell");
                } else {
                    setText(note.getTitle());
                    getStyleClass().add("note-cell");
                    if (note.isFavorite()) {
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
                noteContentArea.setText(newVal.getContent());
            }
        });

        searchField.textProperty().addListener((obs, old, newVal) -> filterNotes());

        updateStats();
        notesList.addListener((javafx.collections.ListChangeListener.Change<? extends Note> c) -> updateStats());
    }

    @FXML
    private void handleFavorite() {
        Note selected = listView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.setFavorite(!selected.isFavorite());
            listView.refresh();
            String status = selected.isFavorite() ? "добавлена в" : "удалена из";
            showAlert("Избранное", "Заметка " + status + " избранного!");
            updateStats();
        } else {
            showAlert("Внимание", "Выберите заметку!");
        }
    }

    @FXML
    private void handleNewNote() {
        currentNote = null;
        titleField.clear();
        noteContentArea.clear();
        listView.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleDeleteNote() {
        Note selected = listView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            notesList.remove(selected);
            handleClear();
            showAlert("Успех", "Заметка удалена!");
            updateStats();
        } else {
            showAlert("Внимание", "Выберите заметку для удаления!");
        }
    }

    @FXML
    private void handleSettings() {
        showAlert("Настройки", "Тема: Тёмная\nСтиль: CloudNote\nВерсия: 1.0");
    }

    @FXML
    private void handleClear() {
        if (currentNote != null && listView.getSelectionModel().getSelectedItem() != null) {
            titleField.setText(currentNote.getTitle());
            noteContentArea.setText(currentNote.getContent());
        } else {
            titleField.clear();
            noteContentArea.clear();
            currentNote = null;
            listView.getSelectionModel().clearSelection();
        }
    }

    @FXML
    private void handleSave() {
        String title = titleField.getText().trim();
        String content = noteContentArea.getText();

        if (title.isEmpty()) {
            showAlert("Ошибка", "Заголовок не может быть пустым!");
            return;
        }

        if (currentNote != null) {
            currentNote.setTitle(title);
            currentNote.setContent(content);
            listView.refresh();
            showAlert("Успех", "Заметка сохранена!");
        } else {
            Note newNote = new Note(title, content, false);
            notesList.add(newNote);
            listView.getSelectionModel().select(newNote);
            showAlert("Успех", "Новая заметка создана!");
        }
        updateStats();
        filterNotes();
    }

    private void filterNotes() {
        String filter = searchField.getText().toLowerCase();
        filteredNotes.setPredicate(note -> {
            if (filter == null || filter.isEmpty()) return true;
            return note.getTitle().toLowerCase().contains(filter) ||
                    note.getContent().toLowerCase().contains(filter);
        });
    }

    private void updateStats() {
        int total = notesList.size();
        int favorites = (int) notesList.stream().filter(Note::isFavorite).count();
        statsLabel.setText("📊 Всего: " + total + " | ⭐ Избранных: " + favorites);
    }

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

    private void loadSampleNotes() {
        notesList.addAll(
                new Note("Добро пожаловать в CloudNote!", "☁️ Ваше личное облачное пространство для заметок.\n\n✨ Особенности:\n• Тёмная тема\n• Поиск по заметкам\n• Избранное\n• Быстрое редактирование", false),
                new Note("Идеи для проекта", "🎯 Добавьте сюда свои мысли и идеи...\n\n1. Реализовать синхронизацию\n2. Добавить теги\n3. Экспорт в PDF", true),
                new Note("Список дел", "✅ Сегодня:\n• Создать заметку\n• Сохранить изменения\n• Организовать в избранное\n• Наслаждаться порядком!", false),
                new Note("Вдохновение", "\"Облачные заметки всегда под рукой!\"\n\nЦитата дня: \"Лучшая заметка - та, которую вы написали\"", true),
                new Note("Рецепты", "☕ Идеальный кофе:\n1. Свежемолотые зёрна\n2. Вода 90-96°C\n3. Наслаждение\n\n🍰 Чизкейк:\n• Сливочный сыр\n• Печенье\n• Любовь к готовке", false)
        );
    }
}