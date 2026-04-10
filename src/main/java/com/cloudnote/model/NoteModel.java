package com.cloudnote.model;

import java.time.LocalDateTime;

public class NoteModel {
    private int id;
    private int id_user;
    private String title;
    private String text;
    private LocalDateTime date;
    private boolean isPin;

    public NoteModel() {
    }

    public NoteModel(int id, int id_user, String title, String text, LocalDateTime date, boolean isPin) {
        this.id = id;
        this.id_user = id_user;
        this.title = title;
        this.text = text;
        this.date = date;
        this.isPin = isPin;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId_user() {
        return id_user;
    }

    public void setId_user(int id_user) {
        this.id_user = id_user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public boolean isPin() {
        return isPin;
    }

    public void setPin(boolean pin) {
        isPin = pin;
    }
}