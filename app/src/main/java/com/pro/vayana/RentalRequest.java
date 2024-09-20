package com.pro.vayana;

public class RentalRequest {
    private String requestId;
    private String userId;
    private String userName;  // Add this field
    private String bookTitle;
    private String requestDate;
    private String status;

    // Update constructor and add getter/setter for userName
    public RentalRequest(String requestId, String userId, String userName, String bookTitle, String requestDate, String status) {
        this.requestId = requestId;
        this.userId = userId;
        this.userName = userName;
        this.bookTitle = bookTitle;
        this.requestDate = requestDate;
        this.status = status;
    }

    // Getter and setter for userName
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    // Other getters and setters...
}