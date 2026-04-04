package com.adarsh.zorvyn.Service;

import com.adarsh.zorvyn.Entity.Role;
import com.adarsh.zorvyn.Entity.Status;
import com.adarsh.zorvyn.Entity.User;
import com.adarsh.zorvyn.Repository.UserDetailsRepository;
import com.adarsh.zorvyn.Request.RegisterUserRequest;
import com.adarsh.zorvyn.Response.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserDetailsRepository userDetailsRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserDetailsRepository userDetailsRepository, PasswordEncoder passwordEncoder) {
        this.userDetailsRepository = userDetailsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse registerUser(RegisterUserRequest request){

        if(userDetailsRepository.findByUsername(request.getUsername()).isPresent()){
            throw new RuntimeException("User already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(Status.ACTIVE);

        User saved = userDetailsRepository.save(user);
        return convertToResponse(saved);
    }

    public List<UserResponse> getAllUsers(){
        return userDetailsRepository.findAll()
                .stream()
                .filter(user -> user.getStatus() != Status.INACTIVE)
                .map(this::convertToResponse)
                .toList();
    }


    public UserResponse updateUserRole(int id, Role role){

        User user = userDetailsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if(user.getStatus() == Status.INACTIVE){
            throw new RuntimeException("Cannot modify inactive user");
        }

        user.setRole(role);

        return convertToResponse(userDetailsRepository.save(user));
    }

    public UserResponse updateUserStatus(int id, Status status){

        User user = userDetailsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(status);

        return convertToResponse(userDetailsRepository.save(user));
    }

    public void softDeleteUser(int id) {
        User user = userDetailsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(Status.INACTIVE);
        userDetailsRepository.save(user);
    }

    public List<UserResponse> getInactiveUsers() {
        return userDetailsRepository.findByStatus(Status.INACTIVE)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    private UserResponse convertToResponse(User user){
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
    }

}