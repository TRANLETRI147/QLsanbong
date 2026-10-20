package com.tri.quanlisanbanh.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tri.quanlisanbanh.model.Booking;
import com.tri.quanlisanbanh.model.BookingStatus;
import com.tri.quanlisanbanh.model.Payment;
import com.tri.quanlisanbanh.model.PaymentStatus;
import com.tri.quanlisanbanh.repository.BookingRepository;
import com.tri.quanlisanbanh.repository.PaymentRepository;

@Service
public class DashboardService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;

    public DashboardService(PaymentRepository paymentRepository, BookingRepository bookingRepository, Clock clock) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardView tongQuan() {
        LocalDate homNay = LocalDate.now(clock);
        LocalDateTime dauHomNay = homNay.atStartOfDay();
        LocalDateTime cuoiHomNay = homNay.plusDays(1).atStartOfDay();

        DashboardView v = new DashboardView();
        v.setDoanhThuHomNay(tong(dauHomNay, cuoiHomNay));
        v.setDoanhThu7Ngay(tong(homNay.minusDays(6).atStartOfDay(), cuoiHomNay));
        v.setDoanhThuThang(tong(homNay.withDayOfMonth(1).atStartOfDay(), cuoiHomNay));
        v.setSoChoDuyet(bookingRepository.countByStatus(BookingStatus.PENDING_APPROVAL));
        v.setSoBookingHomNay(bookingRepository.countByStartAtGreaterThanEqualAndStartAtLessThanAndStatusIn(
                dauHomNay, cuoiHomNay, BookingStatus.GIU_CHO));

        List<Payment> thu = paymentRepository.danhSachThu(dauHomNay, cuoiHomNay);
        v.setThuHomNay(thu);
        Map<String, BigDecimal> theoNhanVien = new LinkedHashMap<>();
        for (Payment p : thu) {
            theoNhanVien.merge(p.getReceivedBy().getFullName(), p.getAmount(), BigDecimal::add);
        }
        v.setThuTheoNhanVien(theoNhanVien);

        // Booking hôm nay đã qua giờ kết thúc nhưng chưa thu đủ tiền
        LocalDateTime bayGio = LocalDateTime.now(clock);
        List<Booking> chuaThu = new ArrayList<>();
        for (Booking b : bookingRepository.timTrongKhoang(dauHomNay, cuoiHomNay, BookingStatus.GIU_CHO)) {
            boolean dangPhucVu = b.isDangPhucVu();
            if (dangPhucVu && b.getEndAt().isBefore(bayGio) && b.getTrangThaiThanhToan() != PaymentStatus.PAID) {
                chuaThu.add(b);
            }
        }
        v.setChuaThuDu(chuaThu);

        // Biểu đồ 7 ngày gần nhất
        DateTimeFormatter dinhDang = DateTimeFormatter.ofPattern("dd/MM");
        List<String> nhan = new ArrayList<>();
        List<Long> giaTri = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate ngay = homNay.minusDays(i);
            nhan.add(ngay.format(dinhDang));
            giaTri.add(tong(ngay.atStartOfDay(), ngay.plusDays(1).atStartOfDay()).longValue());
        }
        v.setNhan7Ngay(nhan);
        v.setGiaTri7Ngay(giaTri);
        return v;
    }

    private BigDecimal tong(LocalDateTime tu, LocalDateTime den) {
        BigDecimal t = paymentRepository.tongThu(tu, den);
        return t == null ? BigDecimal.ZERO : t;
    }
}
