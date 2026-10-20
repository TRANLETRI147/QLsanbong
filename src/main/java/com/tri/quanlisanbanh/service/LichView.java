package com.tri.quanlisanbanh.service;

import java.util.List;

import com.tri.quanlisanbanh.model.Booking;
import com.tri.quanlisanbanh.model.SanBong;

/** Dữ liệu đã tính sẵn để vẽ lưới lịch một ngày: mỗi cột là một sân, mỗi dòng là 30 phút. */
public class LichView {

    private final List<SanBong> cacSan;
    private final List<Dong> cacDong;

    public LichView(List<SanBong> cacSan, List<Dong> cacDong) {
        this.cacSan = cacSan;
        this.cacDong = cacDong;
    }

    public List<SanBong> getCacSan() {
        return cacSan;
    }

    public List<Dong> getCacDong() {
        return cacDong;
    }

    public static class Dong {
        private final String gio;
        private final boolean gioTron;
        private final List<O> cacO;

        public Dong(String gio, boolean gioTron, List<O> cacO) {
            this.gio = gio;
            this.gioTron = gioTron;
            this.cacO = cacO;
        }

        public String getGio() {
            return gio;
        }

        /** true nếu là đầu giờ (19:00), false nếu là nửa giờ (19:30). */
        public boolean isGioTron() {
            return gioTron;
        }

        public List<O> getCacO() {
            return cacO;
        }
    }

    public enum Loai {
        /** Chưa ai đặt: bấm vào để đặt. */
        TRONG,
        /** Booking của chính sân này. */
        CUA_SAN,
        /** Bị khóa do sân liên quan (sân 7 <-> sân 5) đang có người đặt. */
        KHOA,
        /** Ô đã được gộp vào ô phía trên (rowspan), không vẽ. */
        BO_QUA
    }

    public static class O {
        private final Loai loai;
        private final Long sanId;
        private final String gio;
        private final int rowspan;
        private final Booking booking;
        private final List<Booking> khoaBoi;

        public O(Loai loai, Long sanId, String gio, int rowspan, Booking booking, List<Booking> khoaBoi) {
            this.loai = loai;
            this.sanId = sanId;
            this.gio = gio;
            this.rowspan = rowspan;
            this.booking = booking;
            this.khoaBoi = khoaBoi;
        }

        public Loai getLoai() {
            return loai;
        }

        public boolean isTrong() {
            return loai == Loai.TRONG;
        }

        public boolean isCuaSan() {
            return loai == Loai.CUA_SAN;
        }

        public boolean isKhoa() {
            return loai == Loai.KHOA;
        }

        public boolean isBoQua() {
            return loai == Loai.BO_QUA;
        }

        public Long getSanId() {
            return sanId;
        }

        /** Giờ bắt đầu của ô, dạng "HH:mm" (dùng để tạo link đặt sân). */
        public String getGio() {
            return gio;
        }

        public int getRowspan() {
            return rowspan;
        }

        public Booking getBooking() {
            return booking;
        }

        public List<Booking> getKhoaBoi() {
            return khoaBoi;
        }
    }
}
