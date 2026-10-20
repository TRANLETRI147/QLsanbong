package com.tri.quanlisanbanh.model;

/**
 * Chỉ có 2 vai trò. Mỗi người dùng giữ đúng 1 vai trò (cột users.role).
 * File này thay cho Role.java cũ (trước đây là entity/bảng riêng).
 */
public enum Role {
    OWNER("Chủ sân"),
    STAFF("Nhân viên");

    private final String nhan;

    Role(String nhan) {
        this.nhan = nhan;
    }

    public String getNhan() {
        return nhan;
    }
}
