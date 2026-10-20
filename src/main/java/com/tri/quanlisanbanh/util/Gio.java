package com.tri.quanlisanbanh.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.tri.quanlisanbanh.exception.NghiepVuException;

/** Xử lý giờ dạng chữ "HH:mm" (có hỗ trợ "24:00" = nửa đêm hôm sau). */
public final class Gio {

    public static final int BUOC_PHUT = 30;

    private Gio() {
    }

    public static LocalDateTime parse(LocalDate ngay, String chuoi) {
        if (ngay == null) {
            throw new NghiepVuException("Chưa chọn ngày.");
        }
        if (chuoi == null || !chuoi.matches("\\d{1,2}:\\d{2}")) {
            throw new NghiepVuException("Giờ không hợp lệ: " + chuoi);
        }
        String[] phan = chuoi.split(":");
        int gio = Integer.parseInt(phan[0]);
        int phut = Integer.parseInt(phan[1]);
        if (gio == 24 && phut == 0) {
            return ngay.plusDays(1).atStartOfDay();
        }
        if (gio > 23 || phut > 59) {
            throw new NghiepVuException("Giờ không hợp lệ: " + chuoi);
        }
        return ngay.atTime(gio, phut);
    }

    /** Ngược lại với parse: nửa đêm của ngày hôm sau được hiện là "24:00". */
    public static String dinhDang(LocalDateTime thoiDiem, LocalDate ngayGoc) {
        if (thoiDiem.toLocalDate().isAfter(ngayGoc) && thoiDiem.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)) {
            return "24:00";
        }
        return String.format("%02d:%02d", thoiDiem.getHour(), thoiDiem.getMinute());
    }

    public static List<String> cacMocBatDau(int gioMo, int gioDong) {
        List<String> ds = new ArrayList<>();
        for (int p = gioMo * 60; p < gioDong * 60; p += BUOC_PHUT) {
            ds.add(dinhDangPhut(p));
        }
        return ds;
    }

    public static List<String> cacMocKetThuc(int gioMo, int gioDong) {
        List<String> ds = new ArrayList<>();
        for (int p = gioMo * 60 + BUOC_PHUT; p <= gioDong * 60; p += BUOC_PHUT) {
            ds.add(dinhDangPhut(p));
        }
        return ds;
    }

    private static String dinhDangPhut(int tongPhut) {
        return String.format("%02d:%02d", tongPhut / 60, tongPhut % 60);
    }
}
