package com.tri.quanlisanbanh.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tri.quanlisanbanh.config.CauHinhLich;
import com.tri.quanlisanbanh.exception.NghiepVuException;
import com.tri.quanlisanbanh.model.Booking;
import com.tri.quanlisanbanh.model.BookingNote;
import com.tri.quanlisanbanh.model.BookingStatus;
import com.tri.quanlisanbanh.model.LoaiSan;
import com.tri.quanlisanbanh.model.NotificationType;
import com.tri.quanlisanbanh.model.Role;
import com.tri.quanlisanbanh.model.SanBong;
import com.tri.quanlisanbanh.model.User;
import com.tri.quanlisanbanh.repository.BookingRepository;
import com.tri.quanlisanbanh.repository.SanBongRepository;
import com.tri.quanlisanbanh.util.Gio;
import com.tri.quanlisanbanh.util.NhomSan;
import com.tri.quanlisanbanh.util.Tien;

/**
 * Vòng đời booking:
 *   Nhân viên tạo -> PENDING_APPROVAL --chủ duyệt--> CONFIRMED --check-in--> CHECKED_IN --thu đủ tiền--> COMPLETED
 *                        |                              |
 *                        +-- chủ từ chối: REJECTED      +-- chủ hủy: CANCELLED / không đến: NO_SHOW
 *   Chủ sân tạo -> CONFIRMED luôn (không cần duyệt).
 * Booking chờ duyệt VẪN GIỮ CHỖ, chỉ REJECTED / CANCELLED / NO_SHOW mới nhả sân.
 */
@Service
public class BookingService {

    private static final int TOI_DA_PHUT = 6 * 60;

    private final BookingRepository bookingRepository;
    private final SanBongRepository sanBongRepository;
    private final NotificationService notificationService;
    private final CauHinhLich cauHinh;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepository, SanBongRepository sanBongRepository,
                          NotificationService notificationService, CauHinhLich cauHinh, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.sanBongRepository = sanBongRepository;
        this.notificationService = notificationService;
        this.cauHinh = cauHinh;
        this.clock = clock;
    }

    public LocalDate homNay() {
        return LocalDate.now(clock);
    }

    public LocalDateTime bayGio() {
        return LocalDateTime.now(clock);
    }

    // ------------------------------------------------------------------
    // Đọc
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Booking lay(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new NghiepVuException("Không tìm thấy booking #" + id));
    }

    /** Lấy booking và khóa dòng để sửa tiền / trạng thái an toàn khi nhiều người thao tác cùng lúc. */
    @Transactional
    public Booking layDeCapNhat(Long id) {
        return bookingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy booking #" + id));
    }

    @Transactional(readOnly = true)
    public List<Booking> choDuyet() {
        return bookingRepository.timTheoTrangThai(BookingStatus.PENDING_APPROVAL);
    }

    @Transactional(readOnly = true)
    public long soChoDuyet() {
        return bookingRepository.countByStatus(BookingStatus.PENDING_APPROVAL);
    }

    /** Booking đã bị từ chối / hủy / không đến trong ngày (để nhân viên biết mà báo lại cho khách). */
    @Transactional(readOnly = true)
    public List<Booking> daNhaCho(LocalDate ngay) {
        return bookingRepository.timTrongKhoang(ngay.atStartOfDay(), ngay.plusDays(1).atStartOfDay(),
                BookingStatus.NHA_CHO);
    }

    // ------------------------------------------------------------------
    // Tạo booking
    // ------------------------------------------------------------------

    @Transactional
    public Booking tao(User nguoiTao, Long sanId, String tenKhach, String sdtKhach, LocalDate ngay,
                       String batDau, String ketThuc, String ghiChu) {
        String ten = chuanHoaTenKhach(tenKhach);
        String sdt = chuanHoaSdt(sdtKhach);
        SanBong san = khoaVaLaySan(sanId);
        if (!san.isActive()) {
            throw new NghiepVuException(san.getTen() + " đang ngừng hoạt động.");
        }
        LocalDateTime start = Gio.parse(ngay, batDau);
        LocalDateTime end = Gio.parse(ngay, ketThuc);
        kiemTraKhungGio(ngay, start, end);
        kiemTraTrung(san, start, end, null);

        LocalDateTime bayGio = bayGio();
        boolean laChu = nguoiTao.getRole() == Role.OWNER;

        Booking b = new Booking();
        b.setSanBong(san);
        b.setCustomerName(ten);
        b.setCustomerPhone(sdt);
        b.setStartAt(start);
        b.setEndAt(end);
        b.setCourtFee(Tien.tienSan(san.getGiaMoiGio(), Duration.between(start, end).toMinutes()));
        b.setDiscount(BigDecimal.ZERO);
        b.setCreatedBy(nguoiTao);
        b.setCreatedAt(bayGio);
        if (laChu) {
            b.setStatus(BookingStatus.CONFIRMED);
            b.setApprovedBy(nguoiTao);
            b.setApprovedAt(bayGio);
        } else {
            b.setStatus(BookingStatus.PENDING_APPROVAL);
        }
        bookingRepository.save(b);

        if (ghiChu != null && !ghiChu.isBlank()) {
            ghiChuMoi(b, nguoiTao, ghiChu.trim(), false);
        }
        ghiHeThong(b, nguoiTao, laChu
                ? "Chủ sân tạo booking (tự động duyệt)."
                : "Nhân viên tạo booking, chờ chủ sân duyệt.");

        if (!laChu) {
            notificationService.guiChoChu(NotificationType.BOOKING_PENDING, b, nguoiTao,
                    nguoiTao.getFullName() + " tạo booking chờ duyệt: " + b.getMoTa());
        }
        return b;
    }

    // ------------------------------------------------------------------
    // Chủ sân: duyệt / từ chối / hủy / không đến / đổi giờ
    // ------------------------------------------------------------------

    @Transactional
    public void duyet(Long id, User chu) {
        yeuCauChu(chu);
        Booking b = layDeCapNhat(id);
        if (!b.isChoDuyet()) {
            throw new NghiepVuException("Booking này không ở trạng thái chờ duyệt.");
        }
        duyetBooking(b, chu);
    }

    @Transactional
    public int duyetTatCa(User chu) {
        yeuCauChu(chu);
        int dem = 0;
        for (Booking b : bookingRepository.timTheoTrangThai(BookingStatus.PENDING_APPROVAL)) {
            duyetBooking(b, chu);
            dem++;
        }
        return dem;
    }

    @Transactional
    public void tuChoi(Long id, User chu, String lyDo) {
        yeuCauChu(chu);
        Booking b = layDeCapNhat(id);
        if (!b.isChoDuyet()) {
            throw new NghiepVuException("Chỉ từ chối được booking đang chờ duyệt.");
        }
        b.setStatus(BookingStatus.REJECTED);
        b.setReason(chuanHoaLyDo(lyDo));
        ghiHeThong(b, chu, "Chủ sân từ chối booking" + hauToLyDo(b) + ". Sân đã được nhả.");
    }

    @Transactional
    public void huy(Long id, User chu, String lyDo) {
        yeuCauChu(chu);
        Booking b = layDeCapNhat(id);
        if (!b.isChuaDienRa()) {
            throw new NghiepVuException("Chỉ hủy được booking đang chờ duyệt hoặc đã duyệt (chưa check-in).");
        }
        if (b.getDaThu().signum() > 0) {
            throw new NghiepVuException("Booking đã thu " + Tien.dinhDang(b.getDaThu())
                    + ". Hãy hủy phiếu thu (hoàn tiền cho khách) trước khi hủy booking.");
        }
        b.setStatus(BookingStatus.CANCELLED);
        b.setReason(chuanHoaLyDo(lyDo));
        ghiHeThong(b, chu, "Chủ sân hủy booking" + hauToLyDo(b) + ". Sân đã được nhả.");
    }

    @Transactional
    public void khongDen(Long id, User chu) {
        yeuCauChu(chu);
        Booking b = layDeCapNhat(id);
        if (b.getStatus() != BookingStatus.CONFIRMED) {
            throw new NghiepVuException("Chỉ đánh dấu \"không đến\" cho booking đã duyệt và chưa check-in.");
        }
        if (b.getDaThu().signum() > 0) {
            throw new NghiepVuException("Booking đã thu " + Tien.dinhDang(b.getDaThu())
                    + ". Hãy xử lý phiếu thu (cọc) trước.");
        }
        b.setStatus(BookingStatus.NO_SHOW);
        ghiHeThong(b, chu, "Khách không đến. Sân đã được nhả.");
    }

    /** Đổi sân / ngày / giờ (chỉ chủ sân). Tiền sân được tính lại theo giá hiện hành của sân mới. */
    @Transactional
    public void doiGio(Long id, User chu, Long sanId, LocalDate ngay, String batDau, String ketThuc) {
        yeuCauChu(chu);
        Booking b = layDeCapNhat(id);
        if (!b.isChuaDienRa()) {
            throw new NghiepVuException("Chỉ đổi giờ được booking đang chờ duyệt hoặc đã duyệt (chưa check-in).");
        }
        SanBong san = khoaVaLaySan(sanId);
        if (!san.isActive()) {
            throw new NghiepVuException(san.getTen() + " đang ngừng hoạt động.");
        }
        LocalDateTime start = Gio.parse(ngay, batDau);
        LocalDateTime end = Gio.parse(ngay, ketThuc);
        kiemTraKhungGio(ngay, start, end);
        kiemTraTrung(san, start, end, b.getId());

        String cu = b.getSanBong().getTen() + " " + b.getNgayText() + " " + b.getGioText();
        BigDecimal phiCu = b.getCourtFee();
        b.setSanBong(san);
        b.setStartAt(start);
        b.setEndAt(end);
        b.setCourtFee(Tien.tienSan(san.getGiaMoiGio(), Duration.between(start, end).toMinutes()));
        String moi = san.getTen() + " " + b.getNgayText() + " " + b.getGioText();
        String phi = phiCu.compareTo(b.getCourtFee()) == 0 ? ""
                : " Tiền sân: " + Tien.dinhDang(phiCu) + " -> " + Tien.dinhDang(b.getCourtFee()) + ".";
        ghiHeThong(b, chu, "Chủ sân đổi lịch: " + cu + " -> " + moi + "." + phi);
    }

    // ------------------------------------------------------------------
    // Nhân viên (và chủ): check-in, ghi chú
    // ------------------------------------------------------------------

    @Transactional
    public void checkIn(Long id, User nguoi) {
        Booking b = layDeCapNhat(id);
        if (b.getStatus() != BookingStatus.CONFIRMED) {
            throw new NghiepVuException("Chỉ check-in được booking đã duyệt và chưa check-in.");
        }
        if (!b.getStartAt().toLocalDate().equals(homNay())) {
            throw new NghiepVuException("Chỉ check-in trong đúng ngày đặt sân (" + b.getNgayText() + ").");
        }
        b.setStatus(BookingStatus.CHECKED_IN);
        b.setCheckedInBy(nguoi);
        b.setCheckedInAt(bayGio());
        ghiHeThong(b, nguoi, nguoi.getFullName() + " check-in cho khách vào sân.");
        if (b.hoanTatNeuDuTien()) {
            ghiHeThong(b, nguoi, "Đã thu đủ tiền => booking hoàn tất.");
        }
    }

    @Transactional
    public void themGhiChu(Long id, User nguoi, String noiDung, boolean baoChu) {
        if (noiDung == null || noiDung.isBlank()) {
            throw new NghiepVuException("Nội dung ghi chú không được để trống.");
        }
        String nd = noiDung.trim();
        if (nd.length() > 1000) {
            throw new NghiepVuException("Ghi chú tối đa 1000 ký tự.");
        }
        Booking b = layDeCapNhat(id);
        ghiChuMoi(b, nguoi, nd, false);
        if (baoChu && nguoi.getRole() != Role.OWNER) {
            notificationService.guiChoChu(NotificationType.NOTE_ADDED, b, nguoi,
                    nguoi.getFullName() + " ghi chú (" + b.getMoTa() + "): " + nd);
        }
    }

    // ------------------------------------------------------------------
    // Trợ giúp dùng chung (ThanhToanService cũng gọi)
    // ------------------------------------------------------------------

    /** Dòng nhật ký tự động hiện trong lịch sử ghi chú của booking. */
    public void ghiHeThong(Booking b, User nguoi, String noiDung) {
        ghiChuMoi(b, nguoi, noiDung, true);
    }

    public void yeuCauChu(User nguoi) {
        if (nguoi == null || nguoi.getRole() != Role.OWNER) {
            throw new NghiepVuException("Chỉ chủ sân mới được thực hiện thao tác này.");
        }
    }

    // ------------------------------------------------------------------
    // Nội bộ
    // ------------------------------------------------------------------

    private void duyetBooking(Booking b, User chu) {
        b.setStatus(BookingStatus.CONFIRMED);
        b.setApprovedBy(chu);
        b.setApprovedAt(bayGio());
        ghiHeThong(b, chu, "Chủ sân duyệt booking.");
    }

    private void ghiChuMoi(Booking b, User nguoi, String noiDung, boolean heThong) {
        BookingNote n = new BookingNote();
        n.setAuthor(nguoi);
        n.setContent(noiDung.length() > 1000 ? noiDung.substring(0, 1000) : noiDung);
        n.setHeThong(heThong);
        n.setCreatedAt(bayGio());
        b.addNote(n);
    }

    /**
     * Khóa dòng sân "gốc" của cụm rồi mới kiểm tra trùng giờ. Sân 7 và 3 sân 5 của nó dùng chung
     * một dòng khóa, nên 2 người đặt cùng lúc (người đặt sân 7, người đặt sân 5) phải xếp hàng,
     * người sau luôn thấy booking của người trước.
     */
    private SanBong khoaVaLaySan(Long sanId) {
        if (sanId == null) {
            throw new NghiepVuException("Chưa chọn sân.");
        }
        SanBong san = sanBongRepository.findById(sanId).orElseThrow(() -> new NghiepVuException("Không tìm thấy sân."));
        sanBongRepository.findByIdForUpdate(NhomSan.idGoc(san));
        return san;
    }

    private void kiemTraKhungGio(LocalDate ngay, LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new NghiepVuException("Giờ kết thúc phải sau giờ bắt đầu.");
        }
        if (ngay.isBefore(homNay())) {
            throw new NghiepVuException("Không thể đặt sân cho ngày đã qua.");
        }
        long phut = Duration.between(start, end).toMinutes();
        if (start.getMinute() % Gio.BUOC_PHUT != 0 || phut % Gio.BUOC_PHUT != 0) {
            throw new NghiepVuException("Giờ đặt sân phải tròn mốc " + Gio.BUOC_PHUT + " phút.");
        }
        if (phut > TOI_DA_PHUT) {
            throw new NghiepVuException("Mỗi lượt đặt tối đa " + (TOI_DA_PHUT / 60) + " giờ.");
        }
        LocalDateTime moCua = ngay.atTime(cauHinh.gioMoCua(), 0);
        LocalDateTime dongCua = ngay.atStartOfDay().plusHours(cauHinh.gioDongCua());
        if (start.isBefore(moCua) || end.isAfter(dongCua)) {
            throw new NghiepVuException("Sân hoạt động từ " + cauHinh.gioMoCua() + "h đến " + cauHinh.gioDongCua() + "h.");
        }
    }

    /** Cốt lõi của tính năng "sân 7 khóa 3 sân 5": xem NhomSan.sanBiAnhHuong. */
    private void kiemTraTrung(SanBong san, LocalDateTime start, LocalDateTime end, Long boQuaId) {
        Set<Long> ids = NhomSan.sanBiAnhHuong(san, sanBongRepository.findAll());
        List<Booking> trung = bookingRepository.timXungDot(ids, BookingStatus.GIU_CHO, start, end,
                boQuaId == null ? -1L : boQuaId);
        if (trung.isEmpty()) {
            return;
        }
        Booking khac = trung.get(0);
        String chiTiet = khac.getCustomerName() + ", " + khac.getGioText();
        if (khac.getSanBong().getId().equals(san.getId())) {
            throw new NghiepVuException(san.getTen() + " đã có người đặt khung giờ này (" + chiTiet + ").");
        }
        if (san.getLoai() == LoaiSan.SAN_7) {
            throw new NghiepVuException("Không đặt được " + san.getTen() + " vì " + khac.getSanBong().getTen()
                    + " đang có người đặt (" + chiTiet + "). Sân 7 cần trống cả " + LoaiSan.SO_SAN_5_TREN_SAN_7
                    + " sân 5.");
        }
        throw new NghiepVuException(san.getTen() + " đang bị khóa vì " + khac.getSanBong().getTen()
                + " đã được đặt (" + chiTiet + ").");
    }

    private String chuanHoaTenKhach(String ten) {
        String t = ten == null ? "" : ten.trim().replaceAll("\\s+", " ");
        if (t.isEmpty()) {
            throw new NghiepVuException("Tên khách không được để trống.");
        }
        if (t.length() > 100) {
            throw new NghiepVuException("Tên khách tối đa 100 ký tự.");
        }
        return t;
    }

    private String chuanHoaSdt(String sdt) {
        if (sdt == null || sdt.isBlank()) {
            return null;
        }
        String s = sdt.trim();
        if (!s.matches("[0-9 +.\\-]{8,20}")) {
            throw new NghiepVuException("Số điện thoại không hợp lệ.");
        }
        return s;
    }

    private String chuanHoaLyDo(String lyDo) {
        if (lyDo == null || lyDo.isBlank()) {
            return null;
        }
        String l = lyDo.trim();
        return l.length() > 255 ? l.substring(0, 255) : l;
    }

    private String hauToLyDo(Booking b) {
        return b.getReason() == null ? "" : " (lý do: " + b.getReason() + ")";
    }
}
