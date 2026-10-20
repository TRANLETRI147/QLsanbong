package com.tri.quanlisanbanh.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tri.quanlisanbanh.exception.NghiepVuException;
import com.tri.quanlisanbanh.model.User;
import com.tri.quanlisanbanh.service.UserService;

/** Đăng nhập (Spring Security tự xử lý POST /login) và đổi mật khẩu. Không có đăng ký công khai. */
@Controller
public class AuthController extends BaseController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String dangNhap() {
        return "login";
    }

    @GetMapping("/doi-mat-khau")
    public String formDoiMatKhau() {
        return "doi-mat-khau";
    }

    @PostMapping("/doi-mat-khau")
    public String doiMatKhau(Principal principal,
                             @RequestParam String matKhauCu,
                             @RequestParam String matKhauMoi,
                             @RequestParam String nhapLai,
                             RedirectAttributes ra) {
        if (!matKhauMoi.equals(nhapLai)) {
            ra.addFlashAttribute("loi", "Hai lần nhập mật khẩu mới không giống nhau.");
            return "redirect:/doi-mat-khau";
        }
        try {
            User u = userService.theoTen(principal.getName());
            userService.doiMatKhau(u, matKhauCu, matKhauMoi);
            ra.addFlashAttribute("thongBao", "Đã đổi mật khẩu.");
            return "redirect:/";
        } catch (NghiepVuException e) {
            ra.addFlashAttribute("loi", e.getMessage());
            return "redirect:/doi-mat-khau";
        }
    }
}
