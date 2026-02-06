package com.switflow.swiftFlow.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.switflow.swiftFlow.Entity.User;
import com.switflow.swiftFlow.utility.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private Long id;
    private String username;
    
    private String fullName;
    private String phoneNumber;
    
    private String token;
    
    @JsonProperty("roles")
    private Role role;
    
    public AuthResponse(String token, User user, Role role) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.fullName = user.getFullName();
        this.phoneNumber = user.getPhoneNumber() == null ? "" : user.getPhoneNumber();
        this.token = token;
        this.role = role;
    }
}