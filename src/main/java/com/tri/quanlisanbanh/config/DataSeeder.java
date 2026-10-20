package com.tri.quanlisanbanh.config;

import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.tri.quanlisanbanh.model.Role;
import com.tri.quanlisanbanh.service.UserService;

/**
 * Chạy mỗi lần khởi động: nếu bảng users đang trống thì tạo tài khoản chủ sân đầu tiên.
 * (Không còn trang đăng ký công khai. Nhân viên do chủ sân tạo trong menu "Nhân viên".)
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String KY_TU = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final UserService userService;
    private final String username;
    private final String matKhau;

    public DataSeeder(UserService userService,
                      @Value("${app.owner.username:owner}") String username,
                      @Value("${app.owner.password:}") String matKhau) {
        this.userService = userService;
        this.username = username;
        this.matKhau = matKhau;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userService.dem() > 0) {
            return;
        }
        String mk = matKhau;
        boolean tuSinh = mk == null || mk.isBlank();
        if (tuSinh) {
            mk = sinhMatKhau(12);
        }
        userService.tao(username, "Chủ sân", mk, Role.OWNER);
        if (tuSinh) {
            log.warn("=====================================================================");
            log.warn(" ĐÃ TẠO TÀI KHOẢN CHỦ SÂN ĐẦU TIÊN");
            log.warn("   Tên đăng nhập : {}", username);
            log.warn("   Mật khẩu tạm  : {}   (chỉ hiện 1 lần, hãy đổi ngay sau khi đăng nhập)", mk);
            log.warn("=====================================================================");
        } else {
            log.info("Đã tạo tài khoản chủ sân '{}' với mật khẩu lấy từ cấu hình (OWNER_PASSWORD).", username);
        }
    }

    private static String sinhMatKhau(int doDai) {
        SecureRandom ngauNhien = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < doDai; i++) {
            sb.append(KY_TU.charAt(ngauNhien.nextInt(KY_TU.length())));
        }
        return sb.toString();
    }
}
