package com.tri.quanlisanbanh.model;

public enum NotificationType {
    BOOKING_PENDING("Chờ duyệt", "warning"),
    PAYMENT_RECEIVED("Đã thu tiền", "success"),
    NOTE_ADDED("Ghi chú", "info");

    private final String nhan;
    private final String mau;

    NotificationType(String nhan, String mau) {
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
