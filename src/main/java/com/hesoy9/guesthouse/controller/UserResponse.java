package com.hesoy9.guesthouse.dto;

import com.hesoy9.guesthouse.entity.Role;

public class UserResponse {

    private Long id;
    private String username;
    private Role role;
    private Boolean enabled;

    public UserResponse(Long id, String username, Role role, Boolean enabled) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.enabled = enabled;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public Role getRole() { return role; }
    public Boolean getEnabled() { return enabled; }
}
