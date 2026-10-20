package com.tri.quanlisanbanh.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.tri.quanlisanbanh.TestData;

class BookingTienTest {

    private final User nv = TestData.user(2, "NV", Role.STAFF);
    private final SanBong san5 = TestData.san(2, "Sân 5 A-1", LoaiSan.SAN_5, 150000, null);

    private Booking moi(BookingStatus tt) {
        LocalDateTime tu = LocalDateTime.of(2026, 10, 20, 19, 0);
        return TestData.booking(1, san5, nv, tu, tu.plusHours(1), tt, 150000);
    }

    private BookingItem mon(String ten, int sl, long gia) {
        BookingItem i = new BookingItem();
        i.setName(ten);
        i.setQuantity(sl);
        i.setUnitPrice(BigDecimal.valueOf(gia));
        return i;
    }

    private Payment thu(long soTien) {
        Payment p = new Payment();
        p.setAmount(BigDecimal.valueOf(soTien));
        p.setMethod(PaymentMethod.CASH);
        return p;
    }

    private void khop(long mongDoi, BigDecimal thucTe) {
        assertEquals(0, BigDecimal.valueOf(mongDoi).compareTo(thucTe), "mong " + mongDoi + " nhưng là " + thucTe);
    }

    @Test
    void tongTien_gomTienSanCongDichVuTruGiamGia() {
        Booking b = moi(BookingStatus.CHECKED_IN);
        b.addItem(mon("Nước", 10, 10000));      // 100.000
        b.addItem(mon("Áo bib", 2, 20000));     // 40.000
        khop(140000, b.getTienDichVu());
        khop(290000, b.getTongTien());
        b.setDiscount(BigDecimal.valueOf(40000));
        khop(250000, b.getTongTien());
    }

    @Test
    void trangThaiThanhToan_chuaThu_motPhan_duTien() {
        Booking b = moi(BookingStatus.CHECKED_IN);
        assertEquals(PaymentStatus.UNPAID, b.getTrangThaiThanhToan());
        b.addPayment(thu(50000));
        assertEquals(PaymentStatus.PARTIAL, b.getTrangThaiThanhToan());
        khop(100000, b.getConLai());
        b.addPayment(thu(100000));
        assertEquals(PaymentStatus.PAID, b.getTrangThaiThanhToan());
        khop(0, b.getConLai());
    }

    @Test
    void phieuThuDaHuyKhongTinh() {
        Booking b = moi(BookingStatus.CHECKED_IN);
        Payment p = thu(150000);
        b.addPayment(p);
        assertEquals(PaymentStatus.PAID, b.getTrangThaiThanhToan());
        p.setVoided(true);
        khop(0, b.getDaThu());
        assertEquals(PaymentStatus.UNPAID, b.getTrangThaiThanhToan());
    }

    @Test
    void hoanTat_chiKhiDaCheckInVaDuTien() {
        Booking daDuyet = moi(BookingStatus.CONFIRMED);
        daDuyet.addPayment(thu(150000));
        assertFalse(daDuyet.hoanTatNeuDuTien(), "chưa check-in thì chưa hoàn tất dù đã thu đủ");
        assertEquals(BookingStatus.CONFIRMED, daDuyet.getStatus());

        Booking dangDa = moi(BookingStatus.CHECKED_IN);
        assertFalse(dangDa.hoanTatNeuDuTien(), "chưa đủ tiền");
        dangDa.addPayment(thu(150000));
        assertTrue(dangDa.hoanTatNeuDuTien());
        assertEquals(BookingStatus.COMPLETED, dangDa.getStatus());
    }

    @Test
    void huyPhieuThu_moLaiBookingDaHoanTat() {
        Booking b = moi(BookingStatus.CHECKED_IN);
        Payment p = thu(150000);
        b.addPayment(p);
        assertTrue(b.hoanTatNeuDuTien());
        p.setVoided(true);
        assertTrue(b.moLaiNeuThieuTien());
        assertEquals(BookingStatus.CHECKED_IN, b.getStatus());
    }

    @Test
    void giamGiaLonHonTongThiTongLaKhong() {
        Booking b = moi(BookingStatus.CONFIRMED);
        b.setDiscount(BigDecimal.valueOf(999999));
        khop(0, b.getTongTien());
        assertEquals(PaymentStatus.PAID, b.getTrangThaiThanhToan());
    }

    @Test
    void trangThaiGiuCho() {
        for (BookingStatus s : new BookingStatus[] {BookingStatus.PENDING_APPROVAL, BookingStatus.CONFIRMED,
                BookingStatus.CHECKED_IN, BookingStatus.COMPLETED}) {
            assertTrue(BookingStatus.GIU_CHO.contains(s), s + " phải giữ chỗ");
        }
        for (BookingStatus s : new BookingStatus[] {BookingStatus.REJECTED, BookingStatus.CANCELLED,
                BookingStatus.NO_SHOW}) {
            assertFalse(BookingStatus.GIU_CHO.contains(s), s + " phải nhả chỗ");
        }
    }
}
