package com.tri.quanlisanbanh.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.tri.quanlisanbanh.model.User;
import com.tri.quanlisanbanh.repository.UserRepository;

/** Cho Spring Security biết cách lấy tài khoản từ bảng users. Tài khoản bị khóa (enabled=false) không đăng nhập được. */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String ten = username == null ? "" : username.trim().toLowerCase();
        User u = userRepository.findByUsername(ten)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + ten));
        return org.springframework.security.core.userdetails.User.withUsername(u.getUsername())
                .password(u.getPassword())
                .roles(u.getRole().name())
                .disabled(!u.isEnabled())
                .build();
    }
}
