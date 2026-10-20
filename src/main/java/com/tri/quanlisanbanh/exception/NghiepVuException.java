package com.tri.quanlisanbanh.exception;

/**
 * Lỗi nghiệp vụ có thông điệp tiếng Việt để hiện thẳng cho người dùng
 * (ví dụ: "Sân đã có người đặt khung giờ này").
 * Là RuntimeException nên @Transactional sẽ tự rollback.
 */
public class NghiepVuException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NghiepVuException(String message) {
        super(message);
    }
}
