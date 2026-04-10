package com.cloudnote.model;

public class Note {
    private String title;
    private String content;
    private boolean isFavorite;

    public Note(String title, String content, boolean isFavorite) {
        this.title = title;
        this.content = content;
        this.isFavorite = isFavorite;
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isFavorite() { return isFavorite; }

    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    @Override
    public String toString() { return title; }
}