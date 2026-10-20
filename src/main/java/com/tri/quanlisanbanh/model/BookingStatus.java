package com.tri.quanlisanbanh.model;

import java.util.List;

public enum BookingStatus {
    PENDING_APPROVAL("Chờ duyệt", "warning"),
    CONFIRMED("Đã duyệt", "primary"),
    CHECKED_IN("Đang đá", "success"),
    COMPLETED("Hoàn tất", "secondary"),
    REJECTED("Từ chối", "danger"),
    CANCELLED("Đã hủy", "dark"),
    NO_SHOW("Không đến", "dark");

    /**
     * Các trạng thái đang "giữ chỗ" (chiếm sân). Booking chờ duyệt vẫn giữ chỗ để
     * hai nhân viên không thể cùng đặt một khung giờ. Từ chối / hủy / không đến thì nhả sân.
     */
    public static final List<BookingStatus> GIU_CHO =
            List.of(PENDING_APPROVAL, CONFIRMED, CHECKED_IN, COMPLETED);

    public static final List<BookingStatus> NHA_CHO = List.of(REJECTED, CANCELLED, NO_SHOW);

    private final String nhan;
    private final String mau;

    BookingStatus(String nhan, String mau) {
        this.nhan = nhan;
        this.mau = mau;
    }

    public String getNhan() {
        return nhan;
    }

    /** Tên màu Bootstrap (warning, primary, success...). */
    public String getMau() {
        return mau;
    }
}
