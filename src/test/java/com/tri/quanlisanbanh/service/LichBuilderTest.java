package com.tri.quanlisanbanh.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tri.quanlisanbanh.TestData;
import com.tri.quanlisanbanh.model.Booking;
import com.tri.quanlisanbanh.model.BookingStatus;
import com.tri.quanlisanbanh.model.LoaiSan;
import com.tri.quanlisanbanh.model.Role;
import com.tri.quanlisanbanh.model.SanBong;
import com.tri.quanlisanbanh.model.User;
import com.tri.quanlisanbanh.service.LichView.Dong;
import com.tri.quanlisanbanh.service.LichView.Loai;
import com.tri.quanlisanbanh.service.LichView.O;

class LichBuilderTest {

    private static final LocalDate NGAY = LocalDate.of(2026, 10, 20);

    private final User chu = TestData.user(1, "Chu", Role.OWNER);
    private final SanBong san7 = TestData.san(1, "Sân 7 A", LoaiSan.SAN_7, 300000, null);
    private final SanBong a1 = TestData.san(2, "Sân 5 A-1", LoaiSan.SAN_5, 150000, san7);
    private final SanBong a2 = TestData.san(3, "Sân 5 A-2", LoaiSan.SAN_5, 150000, san7);
    private final SanBong a3 = TestData.san(4, "Sân 5 A-3", LoaiSan.SAN_5, 150000, san7);
    private final SanBong b1 = TestData.san(6, "Sân 5 B", LoaiSan.SAN_5, 150000, null);
    private final List<SanBong> tatCa = List.of(a3, san7, a1, b1, a2);   // cố tình xáo trộn

    private Booking dat(long id, SanBong san, int gioTu, int phutTu, int gioDen, int phutDen) {
        return TestData.booking(id, san, chu, NGAY.atTime(gioTu, phutTu), NGAY.atTime(gioDen, phutDen),
                BookingStatus.CONFIRMED, 100000);
    }

    private LichView dung(List<Booking> bookings) {
        return LichBuilder.build(NGAY, tatCa, tatCa, bookings, 5, 23);
    }

    private int cot(LichView v, long sanId) {
        for (int i = 0; i < v.getCacSan().size(); i++) {
            if (v.getCacSan().get(i).getId() == sanId) {
                return i;
            }
        }
        throw new IllegalArgumentException("không có cột " + sanId);
    }

    /** Dòng của một giờ (ví dụ "19:00"). */
    private Dong dong(LichView v, String gio) {
        return v.getCacDong().stream().filter(d -> d.getGio().equals(gio)).findFirst().orElseThrow();
    }

    @Test
    void cotSapXep_san7DauCum_roiToiSan5CuaNo_roiSanLe() {
        LichView v = dung(List.of());
        assertEquals(List.of(1L, 2L, 3L, 4L, 6L), v.getCacSan().stream().map(SanBong::getId).toList());
    }

    @Test
    void soDongTheoGioMoDongCua_moiDong30Phut() {
        LichView v = dung(List.of());
        assertEquals((23 - 5) * 2, v.getCacDong().size());
        assertEquals("05:00", v.getCacDong().get(0).getGio());
        assertEquals("22:30", v.getCacDong().get(v.getCacDong().size() - 1).getGio());
        assertTrue(v.getCacDong().get(0).isGioTron());
        assertTrue(!v.getCacDong().get(1).isGioTron());
    }

    @Test
    void san7DuocDat_thiBaSan5HienOKhoaDungKhungGio() {
        LichView v = dung(List.of(dat(10, san7, 19, 0, 20, 30)));   // 1,5 giờ = 3 dòng
        O o7 = dong(v, "19:00").getCacO().get(cot(v, 1));
        assertEquals(Loai.CUA_SAN, o7.getLoai());
        assertEquals(3, o7.getRowspan());
        for (long id : new long[] {2, 3, 4}) {
            O o = dong(v, "19:00").getCacO().get(cot(v, id));
            assertEquals(Loai.KHOA, o.getLoai(), "sân " + id + " phải bị khóa");
            assertEquals(3, o.getRowspan());
            assertEquals(10L, o.getKhoaBoi().get(0).getId());
            // hai dòng tiếp theo bị gộp (rowspan)
            assertEquals(Loai.BO_QUA, dong(v, "19:30").getCacO().get(cot(v, id)).getLoai());
            assertEquals(Loai.BO_QUA, dong(v, "20:00").getCacO().get(cot(v, id)).getLoai());
            // hết giờ thì trống lại
            assertEquals(Loai.TRONG, dong(v, "20:30").getCacO().get(cot(v, id)).getLoai());
            assertEquals(Loai.TRONG, dong(v, "18:30").getCacO().get(cot(v, id)).getLoai());
        }
        // cụm khác không bị ảnh hưởng
        assertEquals(Loai.TRONG, dong(v, "19:00").getCacO().get(cot(v, 6)).getLoai());
    }

    @Test
    void san5DuocDat_thiSan7BiKhoa_nhungHaiSan5KiaVanTrong() {
        LichView v = dung(List.of(dat(11, a2, 19, 0, 20, 0)));
        assertEquals(Loai.CUA_SAN, dong(v, "19:00").getCacO().get(cot(v, 3)).getLoai());
        assertEquals(Loai.KHOA, dong(v, "19:00").getCacO().get(cot(v, 1)).getLoai());
        assertEquals(Loai.TRONG, dong(v, "19:00").getCacO().get(cot(v, 2)).getLoai());
        assertEquals(Loai.TRONG, dong(v, "19:00").getCacO().get(cot(v, 4)).getLoai());
    }

    @Test
    void haiSan5DatLechGio_thiCotSan7ChiaThanhCacOKhoaTheoKhung() {
        LichView v = dung(List.of(dat(12, a1, 19, 0, 20, 0), dat(13, a2, 19, 30, 20, 30)));
        int c7 = cot(v, 1);
        O luc1900 = dong(v, "19:00").getCacO().get(c7);
        O luc1930 = dong(v, "19:30").getCacO().get(c7);
        O luc2000 = dong(v, "20:00").getCacO().get(c7);
        assertEquals(1, luc1900.getKhoaBoi().size());
        assertEquals(2, luc1930.getKhoaBoi().size());
        assertEquals(1, luc2000.getKhoaBoi().size());
        assertEquals(1, luc1900.getRowspan());
        assertEquals(1, luc1930.getRowspan());
        assertEquals(1, luc2000.getRowspan());
    }

    @Test
    void bookingGioLienKe_khongGopChung() {
        LichView v = dung(List.of(dat(14, a1, 18, 0, 19, 0), dat(15, a1, 19, 0, 20, 0)));
        O truoc = dong(v, "18:00").getCacO().get(cot(v, 2));
        O sau = dong(v, "19:00").getCacO().get(cot(v, 2));
        assertEquals(Loai.CUA_SAN, truoc.getLoai());
        assertEquals(2, truoc.getRowspan());
        assertEquals(Loai.CUA_SAN, sau.getLoai());
        assertEquals(14L, truoc.getBooking().getId());
        assertEquals(15L, sau.getBooking().getId());
    }

    @Test
    void oTrongCoDuThongTinDeTaoLinkDatSan() {
        O o = dong(dung(List.of()), "19:30").getCacO().get(0);
        assertEquals(Loai.TRONG, o.getLoai());
        assertEquals("19:30", o.getGio());
        assertEquals(1L, o.getSanId());
    }

    @Test
    void bookingSangNgayHomSau_vanVeDuoc() {
        // booking 22:00 -> 00:00 hôm sau (đóng cửa 24h)
        Booking dem = TestData.booking(16, a1, chu, NGAY.atTime(22, 0), NGAY.plusDays(1).atStartOfDay(),
                BookingStatus.CONFIRMED, 100000);
        LichView v = LichBuilder.build(NGAY, tatCa, tatCa, List.of(dem), 5, 24);
        O o = dong(v, "22:00").getCacO().get(cot(v, 2));
        assertEquals(Loai.CUA_SAN, o.getLoai());
        assertEquals(4, o.getRowspan());
        assertEquals("23:30", v.getCacDong().get(v.getCacDong().size() - 1).getGio());
    }
}
