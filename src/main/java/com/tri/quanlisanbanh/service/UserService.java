package com.tri.quanlisanbanh.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tri.quanlisanbanh.exception.NghiepVuException;
import com.tri.quanlisanbanh.model.Role;
import com.tri.quanlisanbanh.model.User;
import com.tri.quanlisanbanh.repository.UserRepository;

@Service
public class UserService {

    private static final int MAT_KHAU_TOI_THIEU = 6;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public User theoTen(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy tài khoản " + username));
    }

    @Transactional(readOnly = true)
    public List<User> tatCa() {
        return userRepository.findAllByOrderByRoleAscFullNameAsc();
    }

    @Transactional
    public User taoNhanVien(String username, String hoTen, String matKhau) {
        return tao(username, hoTen, matKhau, Role.STAFF);
    }

    /** Dùng cho DataSeeder khi tạo chủ sân đầu tiên. */
    @Transactional
    public User tao(String username, String hoTen, String matKhau, Role vaiTro) {
        String ten = username == null ? "" : username.trim().toLowerCase();
        if (!ten.matches("[a-z0-9._]{3,30}")) {
            throw new NghiepVuException("Tên đăng nhập chỉ gồm chữ thường không dấu, số, dấu chấm hoặc gạch dưới (3-30 ký tự).");
        }
        if (hoTen == null || hoTen.isBlank() || hoTen.trim().length() > 100) {
            throw new NghiepVuException("Họ tên không được để trống (tối đa 100 ký tự).");
        }
        kiemTraMatKhau(matKhau);
        if (userRepository.existsByUsername(ten)) {
            throw new NghiepVuException("Tên đăng nhập \"" + ten + "\" đã tồn tại.");
        }
        User u = new User();
        u.setUsername(ten);
        u.setFullName(hoTen.trim());
        u.setPassword(passwordEncoder.encode(matKhau));
        u.setRole(vaiTro);
        u.setEnabled(true);
        u.setCreatedAt(LocalDateTime.now(clock));
        return userRepository.save(u);
    }

    @Transactional
    public void datLaiMatKhau(Long id, String matKhauMoi) {
        kiemTraMatKhau(matKhauMoi);
        User u = userRepository.findById(id).orElseThrow(() -> new NghiepVuException("Không tìm thấy tài khoản."));
        u.setPassword(passwordEncoder.encode(matKhauMoi));
    }

    /** Khóa / mở khóa. Chủ sân không tự khóa chính mình được. */
    @Transactional
    public void datTrangThai(Long id, boolean enabled, User nguoiThucHien) {
        User u = userRepository.findById(id).orElseThrow(() -> new NghiepVuException("Không tìm thấy tài khoản."));
        if (u.getId().equals(nguoiThucHien.getId())) {
            throw new NghiepVuException("Bạn không thể tự khóa tài khoản của chính mình.");
        }
        u.setEnabled(enabled);
    }

    @Transactional
    public void doiMatKhau(User nguoiDung, String matKhauCu, String matKhauMoi) {
        User u = userRepository.findById(nguoiDung.getId())
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy tài khoản."));
        if (matKhauCu == null || !passwordEncoder.matches(matKhauCu, u.getPassword())) {
            throw new NghiepVuException("Mật khẩu hiện tại không đúng.");
        }
        kiemTraMatKhau(matKhauMoi);
        u.setPassword(passwordEncoder.encode(matKhauMoi));
    }

    public long dem() {
        return userRepository.count();
    }

    private void kiemTraMatKhau(String matKhau) {
        if (matKhau == null || matKhau.length() < MAT_KHAU_TOI_THIEU) {
            throw new NghiepVuException("Mật khẩu phải có ít nhất " + MAT_KHAU_TOI_THIEU + " ký tự.");
        }
    }
}
