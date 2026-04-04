package com.adarsh.zorvyn.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserResponse is the outbound DTO returned from user management endpoints.
 *
 * It deliberately excludes the password and status fields —
 * only safe, non-sensitive user information is sent to the client.
 *
 * Fields returned: id, username, role
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private int id;
    private String username;
    private String role;
}