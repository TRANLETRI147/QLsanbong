package com.tri.quanlisanbanh.controller;

import java.security.Principal;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.tri.quanlisanbanh.model.Role;
import com.tri.quanlisanbanh.model.User;
import com.tri.quanlisanbanh.repository.UserRepository;
import com.tri.quanlisanbanh.service.BookingService;

/**
 * Đưa thông tin dùng chung cho thanh menu vào MỌI trang:
 *   me          : người đang đăng nhập (null ở trang đăng nhập)
 *   laChu       : true nếu là chủ sân
 *   soChoDuyet  : số booking chờ duyệt (chỉ chủ sân mới thấy số khác 0)
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final UserRepository userRepository;
    private final BookingService bookingService;

    public GlobalModelAdvice(UserRepository userRepository, BookingService bookingService) {
        this.userRepository = userRepository;
        this.bookingService = bookingService;
    }

    @ModelAttribute
    public void thongTinChung(Principal principal, Model model) {
        User me = principal == null ? null : userRepository.findByUsername(principal.getName()).orElse(null);
        boolean laChu = me != null && me.getRole() == Role.OWNER;
        model.addAttribute("me", me);
        model.addAttribute("laChu", laChu);
        model.addAttribute("soChoDuyet", laChu ? bookingService.soChoDuyet() : 0L);
    }
}
