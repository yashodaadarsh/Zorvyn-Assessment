package com.adarsh.zorvyn.Controllers;

import com.adarsh.zorvyn.Request.RegisterUserRequest;
import com.adarsh.zorvyn.Entity.Role;
import com.adarsh.zorvyn.Entity.Status;
import com.adarsh.zorvyn.Response.UserResponse;
import com.adarsh.zorvyn.Service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * UserController manages all user-related operations in the Finance Dashboard.
 *
 * Access Control:
 *   ALL endpoints in this controller are restricted to ADMIN role only.
 *   Analysts and Viewers have no access to user management.
 *
 * Design Decision:
 *   Only an authenticated admin can create new users — there is no public
 *   self-registration. This is intentional because role assignment at creation
 *   determines data access. Uncontrolled registration would be a security risk.
 *
 * Base path: /api/v1/users
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    // Constructor injection — preferred over @Autowired for testability.
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * POST /api/v1/users
     * Access: ADMIN only
     *
     * Creates a new user with the given username, email, password, and role.
     * Password is BCrypt-encoded before being stored.
     * New users are always created with ACTIVE status.
     *
     * @Valid triggers Jakarta Bean Validation on the request body.
     *
     * Returns:
     *   200 OK    - UserResponse (id, username, role)
     *   400       - Validation errors (missing fields, invalid email, etc.)
     *   409       - Username already exists
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UserResponse> registerUser(
            @Valid @RequestBody RegisterUserRequest request
    ){
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerUser(request));
    }

    /**
     * GET /api/v1/users
     * Access: ADMIN only
     *
     * Returns all ACTIVE users in the system.
     * INACTIVE (soft-deleted) users are excluded from this list.
     * Use GET /inactive to view deactivated users.
     *
     * Returns:
     *   200 OK - List of UserResponse objects
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(){
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * GET /api/v1/users/inactive
     * Access: ADMIN only
     *
     * Returns all INACTIVE (soft-deleted) users.
     * Useful for audit purposes and for identifying users eligible for reactivation.
     * An inactive user can be reactivated via PATCH /{id}/status?status=ACTIVE.
     *
     * Returns:
     *   200 OK - List of UserResponse objects
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/inactive")
    public ResponseEntity<List<UserResponse>> getInactiveUsers() {
        return ResponseEntity.ok(userService.getInactiveUsers());
    }

    /**
     * PATCH /api/v1/users/{id}/role?role=ANALYST
     * Access: ADMIN only
     *
     * Updates the role of the specified user.
     * Allowed values: ADMIN, ANALYST, VIEWER
     *
     * Guard: Cannot change the role of an INACTIVE user.
     * Reason: modifying an inactive user's role could lead to confusing
     * state — the user should be reactivated first.
     *
     * Returns:
     *   200 OK  - Updated UserResponse
     *   403     - User is inactive
     *   404     - User not found
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateRole(
            @PathVariable int id,
            @RequestParam Role role
    ){
        return ResponseEntity.ok(userService.updateUserRole(id, role));
    }

    /**
     * PATCH /api/v1/users/{id}/status?status=INACTIVE
     * Access: ADMIN only
     *
     * Directly sets a user's status to ACTIVE or INACTIVE.
     * This is also used to reactivate a soft-deleted user.
     *
     * Allowed values: ACTIVE, INACTIVE
     *
     * Returns:
     *   200 OK  - Updated UserResponse
     *   404     - User not found
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateStatus(
            @PathVariable int id,
            @RequestParam Status status
    ){
        return ResponseEntity.ok(userService.updateUserStatus(id, status));
    }

    /**
     * DELETE /api/v1/users/{id}
     * Access: ADMIN only
     *
     * Soft-deletes a user by setting their status to INACTIVE.
     * The user record is NOT removed from the database — this preserves
     * the audit trail and the createdBy relationship on financial records.
     *
     * An INACTIVE user cannot log in (isEnabled() returns false in the User entity).
     * To restore the user, call PATCH /{id}/status?status=ACTIVE.
     *
     * Returns:
     *   204 No Content - Soft-delete successful
     *   404            - User not found
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeleteUser(@PathVariable int id) {
        userService.softDeleteUser(id);
        return ResponseEntity.noContent().build();
    }

}