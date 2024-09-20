package com.pro.vayana;

public class User {
    private String id;
    public String name;
    public String email;
    public String phone;
    public boolean isAdmin;
    private boolean blocked;

    // Default constructor required for calls to DataSnapshot.getValue(User.class)
    public User() {
    }

    public User(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.isAdmin = false; // Default to false for regular users
        this.blocked = false; // Default to false for new users
    }

    public User(String name, String email, String phone, boolean isAdmin) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.isAdmin = isAdmin;
        this.blocked = false; // Default to false for new users
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
}