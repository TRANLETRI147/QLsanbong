package com.tri.quanlisanbanh;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tri.quanlisanbanh.model.Booking;
import com.tri.quanlisanbanh.model.BookingStatus;
import com.tri.quanlisanbanh.model.LoaiSan;
import com.tri.quanlisanbanh.model.Role;
import com.tri.quanlisanbanh.model.SanBong;
import com.tri.quanlisanbanh.model.User;

/** Dữ liệu mẫu dùng chung cho các test (không cần database). */
public final class TestData {

    private TestData() {
    }

    public static User user(long id, String ten, Role vaiTro) {
        User u = new User();
        u.setId(id);
        u.setUsername(ten.toLowerCase());
        u.setFullName(ten);
        u.setRole(vaiTro);
        u.setPassword("x");
        return u;
    }

    public static SanBong san(long id, String ten, LoaiSan loai, long giaMoiGio, SanBong sanLon) {
        SanBong s = new SanBong();
        s.setId(id);
        s.setTen(ten);
        s.setLoai(loai);
        s.setGiaMoiGio(BigDecimal.valueOf(giaMoiGio));
        s.setSanLon(sanLon);
        s.setActive(true);
        return s;
    }

    public static Booking booking(long id, SanBong san, User nguoiTao, LocalDateTime tu, LocalDateTime den,
                                  BookingStatus trangThai, long tienSan) {
        Booking b = new Booking();
        b.setId(id);
        b.setSanBong(san);
        b.setCustomerName("Khach " + id);
        b.setStartAt(tu);
        b.setEndAt(den);
        b.setStatus(trangThai);
        b.setCourtFee(BigDecimal.valueOf(tienSan));
        b.setCreatedBy(nguoiTao);
        b.setCreatedAt(tu.minusDays(1));
        return b;
    }
}
