package com.tri.quanlisanbanh.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.tri.quanlisanbanh.exception.NghiepVuException;

class TienGioTest {

    @Test
    void dinhDangTien() {
        assertEquals("0đ", Tien.dinhDang(BigDecimal.ZERO));
        assertEquals("400.000đ", Tien.dinhDang(new BigDecimal("400000")));
        assertEquals("1.500.000đ", Tien.dinhDang(new BigDecimal("1500000")));
        assertEquals("0đ", Tien.dinhDang(null));
    }

    @Test
    void docSoTienNguoiDungGo() {
        assertEquals(0, new BigDecimal("400000").compareTo(Tien.parse("400000")));
        assertEquals(0, new BigDecimal("400000").compareTo(Tien.parse("400.000")));
        assertEquals(0, new BigDecimal("400000").compareTo(Tien.parse(" 400,000đ ")));
        assertEquals(0, new BigDecimal("1500000").compareTo(Tien.parse("1 500 000")));
        assertThrows(NghiepVuException.class, () -> Tien.parse(""));
        assertThrows(NghiepVuException.class, () -> Tien.parse("abc"));
        // dấu trừ bị bỏ đi nên không bao giờ ra số âm
        assertEquals(0, new BigDecimal("5").compareTo(Tien.parse("-5")));
    }

    @Test
    void tienSanTheoSoPhut() {
        BigDecimal gia = new BigDecimal("150000");
        assertEquals(0, new BigDecimal("150000").compareTo(Tien.tienSan(gia, 60)));
        assertEquals(0, new BigDecimal("225000").compareTo(Tien.tienSan(gia, 90)));
        assertEquals(0, new BigDecimal("75000").compareTo(Tien.tienSan(gia, 30)));
        assertEquals(0, new BigDecimal("300000").compareTo(Tien.tienSan(gia, 120)));
        // làm tròn đến đồng: 100.000đ/giờ x 20 phút = 33.333,33 => 33.333
        assertEquals(0, new BigDecimal("33333").compareTo(Tien.tienSan(new BigDecimal("100000"), 20)));
    }

    @Test
    void docGioDangChu() {
        LocalDate ngay = LocalDate.of(2026, 10, 20);
        assertEquals(LocalDateTime.of(2026, 10, 20, 19, 30), Gio.parse(ngay, "19:30"));
        assertEquals(LocalDateTime.of(2026, 10, 20, 5, 0), Gio.parse(ngay, "5:00"));
        // 24:00 là nửa đêm của ngày hôm sau
        assertEquals(LocalDateTime.of(2026, 10, 21, 0, 0), Gio.parse(ngay, "24:00"));
        assertThrows(NghiepVuException.class, () -> Gio.parse(ngay, "25:00"));
        assertThrows(NghiepVuException.class, () -> Gio.parse(ngay, "abc"));
        assertThrows(NghiepVuException.class, () -> Gio.parse(null, "19:00"));
    }

    @Test
    void dinhDangGioVaDanhSachMoc() {
        LocalDate ngay = LocalDate.of(2026, 10, 20);
        assertEquals("19:30", Gio.dinhDang(LocalDateTime.of(2026, 10, 20, 19, 30), ngay));
        assertEquals("24:00", Gio.dinhDang(LocalDateTime.of(2026, 10, 21, 0, 0), ngay));
        assertEquals("05:00", Gio.cacMocBatDau(5, 23).get(0));
        assertEquals("22:30", Gio.cacMocBatDau(5, 23).get(Gio.cacMocBatDau(5, 23).size() - 1));
        assertEquals("05:30", Gio.cacMocKetThuc(5, 23).get(0));
        assertEquals("23:00", Gio.cacMocKetThuc(5, 23).get(Gio.cacMocKetThuc(5, 23).size() - 1));
    }
}
