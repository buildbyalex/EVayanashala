package com.pro.vayana;
public class CardItem {
    public String imageUrl;
    public String title;
    private Long timestamp;

    // Firebase requires a no-argument constructor
    public CardItem() {
    }

    public CardItem(String imageUrl, String title, Long timestamp) {
        this.imageUrl = imageUrl;
        this.title = title;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}