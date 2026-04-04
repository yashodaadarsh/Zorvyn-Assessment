package com.adarsh.zorvyn.Repository;

import com.adarsh.zorvyn.Entity.Status;
import com.adarsh.zorvyn.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDetailsRepository extends JpaRepository<User,Integer> {

    Optional<User> findByUsername(String username);

    List<User> findByStatus(Status status);
}
