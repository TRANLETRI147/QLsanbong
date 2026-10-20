package com.tri.quanlisanbanh.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.tri.quanlisanbanh.exception.NghiepVuException;
import com.tri.quanlisanbanh.model.Booking;
import com.tri.quanlisanbanh.model.BookingStatus;
import com.tri.quanlisanbanh.model.NotificationType;
import com.tri.quanlisanbanh.model.PaymentMethod;
import com.tri.quanlisanbanh.model.PaymentStatus;

class BookingServiceTest {

    private final Fixture f = new Fixture();

    private void khop(long mongDoi, BigDecimal thucTe) {
        assertEquals(0, BigDecimal.valueOf(mongDoi).compareTo(thucTe), "mong " + mongDoi + " nhưng là " + thucTe);
    }

    // ================= TÍNH NĂNG: SÂN 7 KHÓA 3 SÂN 5 =================

    @Test
    void datSan5_thiSan7CuaCumBiKhoa() {
        f.datNgayMai(f.chu, f.a1, "19:00", "20:00");
        NghiepVuException e = assertThrows(NghiepVuException.class, () -> f.datNgayMai(f.chu, f.san7A, "19:00", "20:00"));
        assertTrue(e.getMessage().contains("Sân 7 A"), e.getMessage());
        assertTrue(e.getMessage().contains("Sân 5 A-1"), e.getMessage());
    }

    @Test
    void datSan7_thiCaBaSan5BiKhoa() {
        f.datNgayMai(f.chu, f.san7A, "19:00", "20:00");
        for (var san5 : new com.tri.quanlisanbanh.model.SanBong[] {f.a1, f.a2, f.a3}) {
            NghiepVuException e = assertThrows(NghiepVuException.class, () -> f.datNgayMai(f.chu, san5, "19:00", "20:00"));
            assertTrue(e.getMessage().contains("bị khóa"), e.getMessage());
        }
    }

    @Test
    void haiSan5CungCum_datDuocCungLuc() {
        f.datNgayMai(f.chu, f.a1, "19:00", "20:00");
        f.datNgayMai(f.chu, f.a2, "19:00", "20:00");
        f.datNgayMai(f.chu, f.a3, "19:00", "20:00");
        assertEquals(3, f.bookings.size());
    }

    @Test
    void san7_khongDatDuocKhiMoiMotSan5BanDangDat() {
        f.datNgayMai(f.chu, f.a3, "19:30", "20:30");
        // sân 7 từ 19:00-20:00 chồng lên 19:30-20:30 của sân 5 A-3
        assertThrows(NghiepVuException.class, () -> f.datNgayMai(f.chu, f.san7A, "19:00", "20:00"));
    }

    @Test
    void gioLienKe_khongBiXemLaTrung() {
        f.datNgayMai(f.chu, f.san7A, "18:00", "19:00");
        f.datNgayMai(f.chu, f.a1, "19:00", "20:00");
        f.datNgayMai(f.chu, f.san7A, "20:00", "21:00");
        assertEquals(3, f.bookings.size());
    }

    @Test
    void cumKhacNhau_doiLapNhau() {
        f.datNgayMai(f.chu, f.san7A, "19:00", "20:00");
        f.datNgayMai(f.chu, f.san7B, "19:00", "20:00");
        f.datNgayMai(f.chu, f.b1, "20:00", "21:00");
        assertEquals(3, f.bookings.size());
    }

    @Test
    void ngayKhacNhau_khongTrung() {
        f.datNgayMai(f.chu, f.san7A, "19:00", "20:00");
        f.datHomNay(f.chu, f.san7A, "19:00", "20:00");
        assertEquals(2, f.bookings.size());
    }

    @Test
    void sanLe_vaTrungChinNo() {
        f.datNgayMai(f.chu, f.a1, "19:00", "20:00");
        NghiepVuException e = assertThrows(NghiepVuException.class, () -> f.datNgayMai(f.chu, f.a1, "19:30", "20:30"));
        assertTrue(e.getMessage().contains("đã có người đặt"), e.getMessage());
    }

    @Test
    void datSan_khoaDongGocCuaCum() {
        f.datNgayMai(f.chu, f.a2, "19:00", "20:00");
        // sân 5 thuộc sân 7 A => khóa dòng của sân 7 A (id 1) để các lượt đặt cùng cụm xếp hàng
        assertEquals(1L, f.daKhoa.get(f.daKhoa.size() - 1));
        f.datNgayMai(f.chu, f.san7B, "19:00", "20:00");
        assertEquals(5L, f.daKhoa.get(f.daKhoa.size() - 1));
    }

    // ================= GIỮ CHỖ VÀ NHẢ CHỖ =================

    @Test
    void bookingChoDuyetVanGiuCho() {
        Booking b = f.datNgayMai(f.nv, f.a1, "19:00", "20:00");
        assertEquals(BookingStatus.PENDING_APPROVAL, b.getStatus());
        assertThrows(NghiepVuException.class, () -> f.datNgayMai(f.nv2, f.san7A, "19:00", "20:00"));
    }

    @Test
    void tuChoiHoacHuy_thiNhaSan() {
        Booking b = f.datNgayMai(f.nv, f.a1, "19:00", "20:00");
        f.bookingService.tuChoi(b.getId(), f.chu, "Hết sân");
        assertEquals(BookingStatus.REJECTED, b.getStatus());
        Booking c = f.datNgayMai(f.chu, f.san7A, "19:00", "20:00");   // đặt được rồi
        f.bookingService.huy(c.getId(), f.chu, "Khách bận");
        assertEquals(BookingStatus.CANCELLED, c.getStatus());
        f.datNgayMai(f.chu, f.a2, "19:00", "20:00");                  // sân 5 lại đặt được
    }

    // ================= QUYỀN & DUYỆT =================

    @Test
    void nhanVienTao_choDuyet_vaGuiThongBaoChoChu() {
        Booking b = f.datNgayMai(f.nv, f.a1, "19:00", "20:00");
        assertEquals(BookingStatus.PENDING_APPROVAL, b.getStatus());
        assertEquals(1, f.thongBao.size());
        assertEquals(NotificationType.BOOKING_PENDING, f.thongBao.get(0).getType());
        assertTrue(f.thongBao.get(0).getMessage().contains("NhanVien"));
    }

    @Test
    void chuTao_tuDuyet_khongThongBao() {
        Booking b = f.datNgayMai(f.chu, f.a1, "19:00", "20:00");
        assertEquals(BookingStatus.CONFIRMED, b.getStatus());
        assertEquals(0, f.thongBao.size());
    }

    @Test
    void nhanVienKhongDuyetDuoc_chuDuyetDuoc() {
        Booking b = f.datNgayMai(f.nv, f.a1, "19:00", "20:00");
        assertThrows(NghiepVuException.class, () -> f.bookingService.duyet(b.getId(), f.nv));
        assertEquals(BookingStatus.PENDING_APPROVAL, b.getStatus());
        f.bookingService.duyet(b.getId(), f.chu);
        assertEquals(BookingStatus.CONFIRMED, b.getStatus());
        assertEquals(f.chu, b.getApprovedBy());
    }

    @Test
    void nhanVienKhongHuyKhongDoiGio() {
        Booking b = f.datNgayMai(f.chu, f.a1, "19:00", "20:00");
        assertThrows(NghiepVuException.class, () -> f.bookingService.huy(b.getId(), f.nv, "x"));
        assertThrows(NghiepVuException.class,
                () -> f.bookingService.doiGio(b.getId(), f.nv, f.a1.getId(), Fixture.NGAY_MAI, "20:00", "21:00"));
    }

    @Test
    void duyetTatCa() {
        f.datNgayMai(f.nv, f.a1, "18:00", "19:00");
        f.datNgayMai(f.nv, f.a2, "18:00", "19:00");
        assertEquals(2, f.bookingService.duyetTatCa(f.chu));
        assertEquals(0, f.bookingService.soChoDuyet());
    }

    // ================= KIỂM TRA GIỜ =================

    @Test
    void kiemTraGio() {
        assertThrows(NghiepVuException.class,
                () -> f.bookingService.tao(f.chu, 2L, "A", null, Fixture.HOM_NAY.minusDays(1), "19:00", "20:00", null));
        assertThrows(NghiepVuException.class, () -> f.datNgayMai(f.chu, f.a1, "20:00", "19:00"));   // ngược
        assertThrows(NghiepVuException.class, () -> f.datNgayMai(f.chu, f.a1, "19:00", "19:00"));   // rỗng
        assertThrows(NghiepVuException.class, () -> f.datNgayMai(f.chu, f.a1, "19:15", "20:15"));   // lệch mốc 30'
        assertThrows(NghiepVuException.class, () -> f.datNgayMai(f.chu, f.a1, "04:00", "05:00"));   // trước giờ mở cửa
        assertThrows(NghiepVuException.class, () -> f.datNgayMai(f.chu, f.a1, "22:00", "23:30"));   // quá giờ đóng cửa
        assertThrows(NghiepVuException.class, () -> f.datNgayMai(f.chu, f.a1, "08:00", "15:00"));   // > 6 giờ
        assertThrows(NghiepVuException.class,
                () -> f.bookingService.tao(f.chu, 2L, "  ", null, Fixture.NGAY_MAI, "19:00", "20:00", null));
        assertThrows(NghiepVuException.class,
                () -> f.bookingService.tao(f.chu, 2L, "A", "abc", Fixture.NGAY_MAI, "19:00", "20:00", null));
        assertEquals(0, f.bookings.size());
    }

    @Test
    void tienSanTinhTheoGiaVaThoiLuong_vaChupLaiGia() {
        Booking b = f.datNgayMai(f.chu, f.a1, "19:00", "20:30");
        khop(225000, b.getCourtFee());                         // 150.000đ x 1,5 giờ
        Booking c = f.datNgayMai(f.chu, f.san7A, "21:00", "22:00");
        khop(300000, c.getCourtFee());
        f.a1.setGiaMoiGio(BigDecimal.valueOf(999999));         // đổi bảng giá
        khop(225000, b.getCourtFee());                         // booking cũ không đổi
    }

    @Test
    void doiGio_boQuaChinNo_vaTinhLaiTien() {
        Booking b = f.datNgayMai(f.chu, f.a1, "19:00", "20:00");
        f.bookingService.doiGio(b.getId(), f.chu, f.a1.getId(), Fixture.NGAY_MAI, "19:30", "21:00");  // chồng chính nó: được
        khop(225000, b.getCourtFee());
        f.datNgayMai(f.chu, f.a2, "18:00", "19:00");
        // đổi sang sân 7 A giờ 18:30-19:30 sẽ đụng booking của A-2 (18:00-19:00) và của A-1 mới
        assertThrows(NghiepVuException.class,
                () -> f.bookingService.doiGio(b.getId(), f.chu, f.san7A.getId(), Fixture.NGAY_MAI, "18:30", "19:30"));
        assertEquals(f.a1, b.getSanBong());
    }

    // ================= TÍNH TIỀN =================

    private Booking dangDa() {
        Booking b = f.datHomNay(f.chu, f.a1, "18:00", "19:00");           // 150.000đ
        f.bookingService.checkIn(b.getId(), f.nv);
        assertEquals(BookingStatus.CHECKED_IN, b.getStatus());
        return b;
    }

    @Test
    void luongThuTien_motPhanRoiDu_hoanTatVaThongBaoChu() {
        Booking b = dangDa();
        f.thanhToan.themMon(b.getId(), f.nv, "Nước suối", 10, BigDecimal.valueOf(10000));   // +100.000 => 250.000
        khop(250000, b.getTongTien());

        f.thanhToan.thuTien(b.getId(), f.nv, BigDecimal.valueOf(100000), PaymentMethod.CASH);
        assertEquals(PaymentStatus.PARTIAL, b.getTrangThaiThanhToan());
        assertEquals(BookingStatus.CHECKED_IN, b.getStatus());
        khop(150000, b.getConLai());

        f.thanhToan.thuTien(b.getId(), f.nv, BigDecimal.valueOf(150000), PaymentMethod.BANK_TRANSFER);
        assertEquals(PaymentStatus.PAID, b.getTrangThaiThanhToan());
        assertEquals(BookingStatus.COMPLETED, b.getStatus());

        long soThongBaoThu = f.thongBao.stream().filter(n -> n.getType() == NotificationType.PAYMENT_RECEIVED).count();
        assertEquals(2, soThongBaoThu);
        assertTrue(f.thongBao.get(f.thongBao.size() - 1).getMessage().contains("Đã tính tiền xong"));
        assertTrue(f.thongBao.get(f.thongBao.size() - 1).getMessage().contains("150.000đ"));
    }

    @Test
    void khongThuVuotSoConLai_vaKhongThuSau_khiDaHoanTat() {
        Booking b = dangDa();
        assertThrows(NghiepVuException.class,
                () -> f.thanhToan.thuTien(b.getId(), f.nv, BigDecimal.valueOf(150001), PaymentMethod.CASH));
        assertThrows(NghiepVuException.class,
                () -> f.thanhToan.thuTien(b.getId(), f.nv, BigDecimal.ZERO, PaymentMethod.CASH));
        f.thanhToan.thuTien(b.getId(), f.nv, BigDecimal.valueOf(150000), PaymentMethod.CASH);
        assertEquals(BookingStatus.COMPLETED, b.getStatus());
        assertThrows(NghiepVuException.class,
                () -> f.thanhToan.thuTien(b.getId(), f.nv, BigDecimal.valueOf(1000), PaymentMethod.CASH));
        assertThrows(NghiepVuException.class,
                () -> f.thanhToan.themMon(b.getId(), f.nv, "Nước", 1, BigDecimal.valueOf(10000)));
    }

    @Test
    void chiChuMoiGiamGia() {
        Booking b = dangDa();
        assertThrows(NghiepVuException.class, () -> f.thanhToan.giamGia(b.getId(), f.nv, BigDecimal.valueOf(50000)));
        f.thanhToan.giamGia(b.getId(), f.chu, BigDecimal.valueOf(50000));
        khop(100000, b.getTongTien());
        assertThrows(NghiepVuException.class, () -> f.thanhToan.giamGia(b.getId(), f.chu, BigDecimal.valueOf(999999)));
    }

    @Test
    void giamGiaDuTien_thiHoanTatLuon() {
        Booking b = dangDa();
        f.thanhToan.thuTien(b.getId(), f.nv, BigDecimal.valueOf(100000), PaymentMethod.CASH);
        f.thanhToan.giamGia(b.getId(), f.chu, BigDecimal.valueOf(50000));   // tổng còn 100.000, đã thu 100.000
        assertEquals(BookingStatus.COMPLETED, b.getStatus());
    }

    @Test
    void chuHuyPhieuThu_thiBookingMoLai_vaNhanVienKhongHuyDuoc() {
        Booking b = dangDa();
        f.thanhToan.thuTien(b.getId(), f.nv, BigDecimal.valueOf(150000), PaymentMethod.CASH);
        assertEquals(BookingStatus.COMPLETED, b.getStatus());
        b.getPayments().get(0).setId(7L);
        assertThrows(NghiepVuException.class, () -> f.thanhToan.huyPhieuThu(b.getId(), 7L, f.nv));
        f.thanhToan.huyPhieuThu(b.getId(), 7L, f.chu);
        assertEquals(BookingStatus.CHECKED_IN, b.getStatus());
        assertEquals(PaymentStatus.UNPAID, b.getTrangThaiThanhToan());
        assertThrows(NghiepVuException.class, () -> f.thanhToan.huyPhieuThu(b.getId(), 7L, f.chu));   // hủy lần 2
    }

    @Test
    void daThuTien_thiKhongHuyBookingDuoc_vaChuaDuyetThiKhongThuDuoc() {
        Booking pending = f.datNgayMai(f.nv, f.a1, "19:00", "20:00");
        assertThrows(NghiepVuException.class,
                () -> f.thanhToan.thuTien(pending.getId(), f.nv, BigDecimal.valueOf(1000), PaymentMethod.CASH));

        Booking daDuyet = f.datNgayMai(f.chu, f.a2, "19:00", "20:00");
        f.thanhToan.thuTien(daDuyet.getId(), f.nv, BigDecimal.valueOf(50000), PaymentMethod.CASH);   // tiền cọc
        assertThrows(NghiepVuException.class, () -> f.bookingService.huy(daDuyet.getId(), f.chu, "x"));
        assertEquals(BookingStatus.CONFIRMED, daDuyet.getStatus());
    }

    @Test
    void xoaMon() {
        Booking b = dangDa();
        f.thanhToan.themMon(b.getId(), f.nv, "Nước", 2, BigDecimal.valueOf(10000));
        b.getItems().get(0).setId(3L);
        khop(170000, b.getTongTien());
        f.thanhToan.xoaMon(b.getId(), 3L, f.nv);
        khop(150000, b.getTongTien());
        assertTrue(b.getItems().isEmpty());
    }

    @Test
    void checkIn_chiDungNgay_vaChiKhiDaDuyet() {
        Booking mai = f.datNgayMai(f.chu, f.a1, "19:00", "20:00");
        assertThrows(NghiepVuException.class, () -> f.bookingService.checkIn(mai.getId(), f.nv));   // chưa tới ngày
        Booking pending = f.datHomNay(f.nv, f.a2, "19:00", "20:00");
        assertThrows(NghiepVuException.class, () -> f.bookingService.checkIn(pending.getId(), f.nv)); // chưa duyệt
    }

    @Test
    void checkIn_khiDaThuDuCoc_thiHoanTatLuon() {
        Booking b = f.datHomNay(f.chu, f.a1, "18:00", "19:00");
        f.thanhToan.thuTien(b.getId(), f.nv, BigDecimal.valueOf(150000), PaymentMethod.BANK_TRANSFER);
        assertEquals(BookingStatus.CONFIRMED, b.getStatus());   // chưa check-in thì chưa hoàn tất
        f.bookingService.checkIn(b.getId(), f.nv);
        assertEquals(BookingStatus.COMPLETED, b.getStatus());
    }

    // ================= GHI CHÚ =================

    @Test
    void ghiChu_nhanVienBaoChu_thiCoThongBao() {
        Booking b = f.datNgayMai(f.chu, f.a1, "19:00", "20:00");
        f.bookingService.themGhiChu(b.getId(), f.nv, "Khách xin dời 30 phút", false);
        assertEquals(0, f.thongBao.size());
        f.bookingService.themGhiChu(b.getId(), f.nv, "Khách xin dời 30 phút", true);
        assertEquals(1, f.thongBao.size());
        assertEquals(NotificationType.NOTE_ADDED, f.thongBao.get(0).getType());
        assertTrue(b.getNotes().stream().anyMatch(n -> !n.isHeThong() && n.getContent().contains("dời 30 phút")));
        assertThrows(NghiepVuException.class, () -> f.bookingService.themGhiChu(b.getId(), f.nv, "   ", false));
    }

    @Test
    void nhatKyHeThongDuocGhi() {
        Booking b = f.datNgayMai(f.nv, f.a1, "19:00", "20:00");
        f.bookingService.duyet(b.getId(), f.chu);
        assertTrue(b.getNotes().stream().anyMatch(n -> n.isHeThong() && n.getContent().contains("duyệt")));
        assertFalse(b.getNotes().isEmpty());
    }
}
