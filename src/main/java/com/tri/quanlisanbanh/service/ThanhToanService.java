package com.tri.quanlisanbanh.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tri.quanlisanbanh.exception.NghiepVuException;
import com.tri.quanlisanbanh.model.Booking;
import com.tri.quanlisanbanh.model.BookingItem;
import com.tri.quanlisanbanh.model.BookingStatus;
import com.tri.quanlisanbanh.model.NotificationType;
import com.tri.quanlisanbanh.model.Payment;
import com.tri.quanlisanbanh.model.PaymentMethod;
import com.tri.quanlisanbanh.model.User;
import com.tri.quanlisanbanh.util.Tien;

/**
 * Tính tiền: thêm dịch vụ, thu tiền (nhân viên bấm "Đã thu tiền" => chủ sân nhận thông báo),
 * giảm giá và hủy phiếu thu (chỉ chủ sân).
 */
@Service
public class ThanhToanService {

    private static final BigDecimal TOI_DA_MOT_DONG = new BigDecimal("100000000");

    private final BookingService bookingService;
    private final NotificationService notificationService;
    private final Clock clock;

    public ThanhToanService(BookingService bookingService, NotificationService notificationService, Clock clock) {
        this.bookingService = bookingService;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Transactional
    public void themMon(Long bookingId, User nguoi, String ten, int soLuong, BigDecimal donGia) {
        Booking b = bookingService.layDeCapNhat(bookingId);
        yeuCauDangPhucVu(b);
        String t = ten == null ? "" : ten.trim().replaceAll("\\s+", " ");
        if (t.isEmpty() || t.length() > 100) {
            throw new NghiepVuException("Tên dịch vụ không được để trống (tối đa 100 ký tự).");
        }
        if (soLuong < 1 || soLuong > 99) {
            throw new NghiepVuException("Số lượng phải từ 1 đến 99.");
        }
        if (donGia == null || donGia.signum() < 0 || donGia.compareTo(TOI_DA_MOT_DONG) > 0) {
            throw new NghiepVuException("Đơn giá không hợp lệ.");
        }
        BookingItem item = new BookingItem();
        item.setName(t);
        item.setQuantity(soLuong);
        item.setUnitPrice(donGia);
        item.setAddedBy(nguoi);
        item.setCreatedAt(LocalDateTime.now(clock));
        b.addItem(item);
        bookingService.ghiHeThong(b, nguoi, "Thêm dịch vụ: " + t + " x" + soLuong + " (" + Tien.dinhDang(donGia) + "/cái).");
    }

    @Transactional
    public void xoaMon(Long bookingId, Long itemId, User nguoi) {
        Booking b = bookingService.layDeCapNhat(bookingId);
        yeuCauDangPhucVu(b);
        BookingItem item = b.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy dịch vụ."));
        if (b.getDaThu().compareTo(b.getTongTien().subtract(item.getThanhTien())) > 0) {
            throw new NghiepVuException("Không xóa được: số tiền đã thu vượt quá tổng mới. Nhờ chủ sân hủy phiếu thu trước.");
        }
        b.getItems().remove(item);
        bookingService.ghiHeThong(b, nguoi, "Xóa dịch vụ: " + item.getName() + " x" + item.getQuantity() + ".");
        if (b.hoanTatNeuDuTien()) {
            bookingService.ghiHeThong(b, nguoi, "Đã thu đủ tiền => booking hoàn tất.");
        }
    }

    /** Chỉ chủ sân được giảm giá. Giảm giá không vượt quá (tiền sân + dịch vụ). */
    @Transactional
    public void giamGia(Long bookingId, User chu, BigDecimal soTien) {
        bookingService.yeuCauChu(chu);
        Booking b = bookingService.layDeCapNhat(bookingId);
        if (b.getStatus() == BookingStatus.COMPLETED || !b.isChuaDienRa() && !b.isDangPhucVu()) {
            throw new NghiepVuException("Không giảm giá được cho booking ở trạng thái này.");
        }
        BigDecimal toiDa = b.getCourtFee().add(b.getTienDichVu());
        if (soTien == null || soTien.signum() < 0 || soTien.compareTo(toiDa) > 0) {
            throw new NghiepVuException("Giảm giá phải từ 0 đến " + Tien.dinhDang(toiDa) + ".");
        }
        if (b.getDaThu().compareTo(toiDa.subtract(soTien)) > 0) {
            throw new NghiepVuException("Không giảm được: số tiền đã thu lớn hơn tổng sau giảm.");
        }
        b.setDiscount(soTien);
        bookingService.ghiHeThong(b, chu, "Chủ sân đặt giảm giá: " + Tien.dinhDang(soTien) + ".");
        if (b.hoanTatNeuDuTien()) {
            bookingService.ghiHeThong(b, chu, "Đã thu đủ tiền => booking hoàn tất.");
        }
    }

    /**
     * Nhân viên bấm "Đã thu tiền". Tạo phiếu thu, cập nhật trạng thái, gửi thông báo lên chủ sân.
     */
    @Transactional
    public void thuTien(Long bookingId, User nguoi, BigDecimal soTien, PaymentMethod hinhThuc) {
        Booking b = bookingService.layDeCapNhat(bookingId);
        yeuCauDangPhucVu(b);
        if (hinhThuc == null) {
            throw new NghiepVuException("Chưa chọn hình thức thanh toán.");
        }
        if (soTien == null || soTien.signum() <= 0) {
            throw new NghiepVuException("Số tiền thu phải lớn hơn 0.");
        }
        BigDecimal conLai = b.getConLai();
        if (soTien.compareTo(conLai) > 0) {
            throw new NghiepVuException("Số tiền thu (" + Tien.dinhDang(soTien) + ") vượt quá số còn lại ("
                    + Tien.dinhDang(conLai) + ").");
        }
        Payment p = new Payment();
        p.setAmount(soTien);
        p.setMethod(hinhThuc);
        p.setPaidAt(LocalDateTime.now(clock));
        p.setReceivedBy(nguoi);
        b.addPayment(p);

        boolean xong = b.hoanTatNeuDuTien();
        bookingService.ghiHeThong(b, nguoi, nguoi.getFullName() + " thu " + Tien.dinhDang(soTien)
                + " (" + hinhThuc.getNhan() + ")." + (xong ? " Đã thu đủ => booking hoàn tất." : ""));

        String tinhTrang = b.getConLai().signum() == 0
                ? "Đã tính tiền xong."
                : "Còn lại " + Tien.dinhDang(b.getConLai()) + ".";
        notificationService.guiChoChu(NotificationType.PAYMENT_RECEIVED, b, nguoi,
                nguoi.getFullName() + " đã thu " + Tien.dinhDang(soTien) + " (" + hinhThuc.getNhan() + ") - "
                        + b.getMoTa() + ". " + tinhTrang);
    }

    /** Chỉ chủ sân: hủy phiếu thu nhập nhầm. Nếu booking đã hoàn tất mà thiếu tiền thì mở lại. */
    @Transactional
    public void huyPhieuThu(Long bookingId, Long paymentId, User chu) {
        bookingService.yeuCauChu(chu);
        Booking b = bookingService.layDeCapNhat(bookingId);
        Payment p = b.getPayments().stream().filter(x -> x.getId().equals(paymentId)).findFirst()
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy phiếu thu."));
        if (p.isVoided()) {
            throw new NghiepVuException("Phiếu thu này đã được hủy rồi.");
        }
        p.setVoided(true);
        p.setVoidedBy(chu);
        p.setVoidedAt(LocalDateTime.now(clock));
        boolean moLai = b.moLaiNeuThieuTien();
        bookingService.ghiHeThong(b, chu, "Chủ sân hủy phiếu thu " + Tien.dinhDang(p.getAmount()) + " ("
                + p.getMethod().getNhan() + ")." + (moLai ? " Booking được mở lại vì còn thiếu tiền." : ""));
    }

    private void yeuCauDangPhucVu(Booking b) {
        if (!b.isDangPhucVu()) {
            throw new NghiepVuException("Chỉ thao tác tiền với booking đã duyệt và chưa hoàn tất (hiện: "
                    + b.getStatus().getNhan() + ").");
        }
    }
}
