package com.tri.quanlisanbanh.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Trang gốc "/" : chủ sân vào Tổng quan, nhân viên vào Lịch sân. */
@Controller
public class HomeController {

    @GetMapping("/")
    public String trangChu(Authentication auth) {
        boolean laChu = auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_OWNER"::equals);
        return laChu ? "redirect:/owner/dashboard" : "redirect:/lich";
    }
}
