package com.tri.quanlisanbanh.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import com.tri.quanlisanbanh.exception.NghiepVuException;

public final class Tien {

    private Tien() {
    }

    /** 1500000 -> "1.500.000đ" */
    public static String dinhDang(BigDecimal so) {
        BigDecimal v = so == null ? BigDecimal.ZERO : so;
        DecimalFormatSymbols kyHieu = new DecimalFormatSymbols(Locale.ROOT);
        kyHieu.setGroupingSeparator('.');
        DecimalFormat dinhDang = new DecimalFormat("#,##0", kyHieu);
        return dinhDang.format(v.setScale(0, RoundingMode.HALF_UP)) + "đ";
    }

    /**
     * Đọc số tiền người dùng gõ. Chấp nhận "400000", "400.000", "400,000", "400 000đ".
     * Chỉ giữ lại chữ số nên không bao giờ ra số âm.
     */
    public static BigDecimal parse(String chuoi) {
        String chiSo = chuoi == null ? "" : chuoi.replaceAll("[^0-9]", "");
        if (chiSo.isEmpty()) {
            throw new NghiepVuException("Số tiền không hợp lệ.");
        }
        if (chiSo.length() > 10) {
            throw new NghiepVuException("Số tiền quá lớn.");
        }
        return new BigDecimal(chiSo);
    }

    /** Tiền sân = đơn giá giờ x số phút / 60, làm tròn đến đồng. */
    public static BigDecimal tienSan(BigDecimal giaMoiGio, long soPhut) {
        return giaMoiGio.multiply(BigDecimal.valueOf(soPhut))
                .divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP);
    }
}
