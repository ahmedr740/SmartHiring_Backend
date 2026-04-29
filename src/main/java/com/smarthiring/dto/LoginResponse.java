package com.smarthiring.dto;

public class LoginResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String status;
    private String restaurantName;
    private String phone;
    private String location;
    private String token;

    public LoginResponse(
            Long id,
            String name,
            String email,
            String role,
            String status,
            String restaurantName,
            String phone,
            String location,
            String token
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.status = status;
        this.restaurantName = restaurantName;
        this.phone = phone;
        this.location = location;
        this.token = token;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public String getRestaurantName() { return restaurantName; }
    public String getPhone() { return phone; }
    public String getLocation() { return location; }
    public String getToken() { return token; }
}
