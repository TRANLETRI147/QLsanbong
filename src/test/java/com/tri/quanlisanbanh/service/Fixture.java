package com.tri.quanlisanbanh.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.tri.quanlisanbanh.TestData;
import com.tri.quanlisanbanh.config.CauHinhLich;
import com.tri.quanlisanbanh.model.Booking;
import com.tri.quanlisanbanh.model.BookingStatus;
import com.tri.quanlisanbanh.model.LoaiSan;
import com.tri.quanlisanbanh.model.Notification;
import com.tri.quanlisanbanh.model.Role;
import com.tri.quanlisanbanh.model.SanBong;
import com.tri.quanlisanbanh.model.User;
import com.tri.quanlisanbanh.repository.BookingRepository;
import com.tri.quanlisanbanh.repository.NotificationRepository;
import com.tri.quanlisanbanh.repository.SanBongRepository;

/**
 * Môi trường giả để test service mà không cần database: các repository là "bản giả" lưu trong bộ nhớ.
 * Hàm timXungDot ở đây viết lại đúng ý nghĩa của câu JPQL trong BookingRepository.
 * Bây giờ giả lập là 19/10/2026 10:00 giờ Việt Nam.
 */
class Fixture {

    static final LocalDate HOM_NAY = LocalDate.of(2026, 10, 19);
    static final LocalDate NGAY_MAI = HOM_NAY.plusDays(1);

    final User chu = TestData.user(1, "Chu", Role.OWNER);
    final User nv = TestData.user(2, "NhanVien", Role.STAFF);
    final User nv2 = TestData.user(3, "NhanVien2", Role.STAFF);

    // Cụm A: sân 7 (id 1) + 3 sân 5 (id 2,3,4). Cụm B: sân 7 (id 5) + 3 sân 5 (id 6,7,8)
    final SanBong san7A = TestData.san(1, "Sân 7 A", LoaiSan.SAN_7, 300000, null);
    final SanBong a1 = TestData.san(2, "Sân 5 A-1", LoaiSan.SAN_5, 150000, san7A);
    final SanBong a2 = TestData.san(3, "Sân 5 A-2", LoaiSan.SAN_5, 150000, san7A);
    final SanBong a3 = TestData.san(4, "Sân 5 A-3", LoaiSan.SAN_5, 150000, san7A);
    final SanBong san7B = TestData.san(5, "Sân 7 B", LoaiSan.SAN_7, 300000, null);
    final SanBong b1 = TestData.san(6, "Sân 5 B-1", LoaiSan.SAN_5, 150000, san7B);

    final Map<Long, SanBong> sans = new LinkedHashMap<>();
    final List<Booking> bookings = new ArrayList<>();
    final List<Notification> thongBao = new ArrayList<>();
    final List<Long> daKhoa = new ArrayList<>();
    private long idKeTiep = 100;

    final Clock clock = Clock.fixed(Instant.parse("2026-10-19T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    final BookingService bookingService;
    final ThanhToanService thanhToan;

    Fixture() {
        for (SanBong s : List.of(san7A, a1, a2, a3, san7B, b1)) {
            sans.put(s.getId(), s);
        }
        BookingRepository bookingRepo = proxy(BookingRepository.class, (p, m, args) -> switch (m.getName()) {
            case "findById", "findByIdForUpdate" -> Optional.ofNullable(timBooking((Long) args[0]));
            case "save" -> luu((Booking) args[0]);
            case "timXungDot" -> timXungDot((Collection<Long>) args[0], (Collection<BookingStatus>) args[1],
                    (LocalDateTime) args[2], (LocalDateTime) args[3], (Long) args[4]);
            case "timTheoTrangThai" -> bookings.stream().filter(b -> b.getStatus() == args[0]).toList();
            case "countByStatus" -> bookings.stream().filter(b -> b.getStatus() == args[0]).count();
            case "timTrongKhoang" -> bookings.stream()
                    .filter(b -> b.getStartAt().isBefore((LocalDateTime) args[1])
                            && b.getEndAt().isAfter((LocalDateTime) args[0])
                            && ((Collection<BookingStatus>) args[2]).contains(b.getStatus()))
                    .toList();
            default -> throw new UnsupportedOperationException("BookingRepository." + m.getName());
        });
        SanBongRepository sanRepo = proxy(SanBongRepository.class, (p, m, args) -> switch (m.getName()) {
            case "findAll" -> new ArrayList<>(sans.values());
            case "findById" -> Optional.ofNullable(sans.get((Long) args[0]));
            case "findByIdForUpdate" -> {
                daKhoa.add((Long) args[0]);
                yield Optional.ofNullable(sans.get((Long) args[0]));
            }
            default -> throw new UnsupportedOperationException("SanBongRepository." + m.getName());
        });
        NotificationRepository thongBaoRepo = proxy(NotificationRepository.class, (p, m, args) -> {
            if (m.getName().equals("save")) {
                thongBao.add((Notification) args[0]);
                return args[0];
            }
            throw new UnsupportedOperationException("NotificationRepository." + m.getName());
        });
        NotificationService notificationService = new NotificationService(thongBaoRepo, clock);
        bookingService = new BookingService(bookingRepo, sanRepo, notificationService, new CauHinhLich(5, 23), clock);
        thanhToan = new ThanhToanService(bookingService, notificationService, clock);
    }

    /** Nhân viên tạo booking cho ngày mai. Trả về booking (hoặc ném NghiepVuException nếu bị từ chối). */
    Booking datNgayMai(User nguoi, SanBong san, String tu, String den) {
        return bookingService.tao(nguoi, san.getId(), "Khách " + san.getTen(), "0900000000", NGAY_MAI, tu, den, null);
    }

    Booking datHomNay(User nguoi, SanBong san, String tu, String den) {
        return bookingService.tao(nguoi, san.getId(), "Khách hôm nay", null, HOM_NAY, tu, den, null);
    }

    // ------------------------------------------------------------------

    private Booking timBooking(Long id) {
        return bookings.stream().filter(b -> b.getId() != null && b.getId().equals(id)).findFirst().orElse(null);
    }

    private Booking luu(Booking b) {
        if (b.getId() == null) {
            b.setId(idKeTiep++);
        }
        if (!bookings.contains(b)) {
            bookings.add(b);
        }
        return b;
    }

    private List<Booking> timXungDot(Collection<Long> sanIds, Collection<BookingStatus> trangThai, LocalDateTime tu,
                                     LocalDateTime den, Long boQuaId) {
        return bookings.stream()
                .filter(b -> sanIds.contains(b.getSanBong().getId()))
                .filter(b -> trangThai.contains(b.getStatus()))
                .filter(b -> b.getStartAt().isBefore(den) && b.getEndAt().isAfter(tu))
                .filter(b -> !b.getId().equals(boQuaId))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> kieu, Handler handler) {
        InvocationHandler ih = (p, m, args) -> handler.xuLy(p, m, args);
        return (T) Proxy.newProxyInstance(kieu.getClassLoader(), new Class<?>[] {kieu}, ih);
    }

    private interface Handler {
        Object xuLy(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable;
    }
}
