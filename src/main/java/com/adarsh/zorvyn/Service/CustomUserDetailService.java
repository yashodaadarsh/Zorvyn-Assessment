package com.adarsh.zorvyn.Service;

import com.adarsh.zorvyn.Entity.User;
import com.adarsh.zorvyn.Repository.UserDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailService implements UserDetailsService {
    @Autowired
    private UserDetailsRepository userDetailsRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = userDetailsRepository.findByUsername(username).orElseThrow(()->new UsernameNotFoundException("User Not Found"));
        return user;
    }

    @Autowired
    private PasswordEncoder encoder;

    public User save( User user ){
        user.setPassword( encoder.encode(user.getPassword()) );
        return userDetailsRepository.save(user);
    }
}
