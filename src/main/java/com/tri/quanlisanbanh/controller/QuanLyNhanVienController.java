package com.tri.quanlisanbanh.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tri.quanlisanbanh.service.UserService;

/** Chủ sân tạo tài khoản nhân viên, khóa/mở khóa, đặt lại mật khẩu. */
@Controller
public class QuanLyNhanVienController extends BaseController {

    private final UserService userService;

    public QuanLyNhanVienController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/owner/nhan-vien")
    public String danhSach(Model model) {
        model.addAttribute("ds", userService.tatCa());
        return "owner/nhan-vien";
    }

    @PostMapping("/owner/nhan-vien")
    public String tao(@RequestParam String username, @RequestParam String hoTen, @RequestParam String matKhau,
                      RedirectAttributes ra) {
        return chay(ra, "Đã tạo tài khoản nhân viên \"" + username.trim().toLowerCase() + "\".", "/owner/nhan-vien",
                () -> userService.taoNhanVien(username, hoTen, matKhau));
    }

    @PostMapping("/owner/nhan-vien/{id}/trang-thai")
    public String datTrangThai(@PathVariable Long id, @RequestParam boolean enabled, Principal principal,
                               RedirectAttributes ra) {
        return chay(ra, enabled ? "Đã mở khóa tài khoản." : "Đã khóa tài khoản.", "/owner/nhan-vien",
                () -> userService.datTrangThai(id, enabled, userService.theoTen(principal.getName())));
    }

    @PostMapping("/owner/nhan-vien/{id}/mat-khau")
    public String datLaiMatKhau(@PathVariable Long id, @RequestParam String matKhauMoi, RedirectAttributes ra) {
        return chay(ra, "Đã đặt lại mật khẩu.", "/owner/nhan-vien",
                () -> userService.datLaiMatKhau(id, matKhauMoi));
    }
}
