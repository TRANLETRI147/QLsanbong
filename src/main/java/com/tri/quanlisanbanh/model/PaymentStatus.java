package com.tri.quanlisanbanh.model;

/** Không lưu trong DB: luôn được tính từ tổng tiền và các phiếu thu (Booking#getTrangThaiThanhToan). */
public enum PaymentStatus {
    UNPAID("Chưa thu", "danger"),
    PARTIAL("Thu một phần", "warning"),
    PAID("Đã thu đủ", "success");

    private final String nhan;
    private final String mau;

    PaymentStatus(String nhan, String mau) {
        this.nhan = nhan;
        this.mau = mau;
    }

    public String getNhan() {
        return nhan;
    }

    public String getMau() {
        return mau;
    }
}
