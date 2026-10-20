package com.tri.quanlisanbanh.model;

public enum LoaiSan {
    SAN_5("Sân 5"),
    SAN_7("Sân 7");

    /** Một sân 7 được ghép từ đúng bấy nhiêu sân 5. */
    public static final int SO_SAN_5_TREN_SAN_7 = 3;

    private final String nhan;

    LoaiSan(String nhan) {
        this.nhan = nhan;
    }

    public String getNhan() {
        return nhan;
    }
}
