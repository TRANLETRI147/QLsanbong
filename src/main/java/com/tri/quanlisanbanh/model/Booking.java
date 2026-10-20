package com.tri.quanlisanbanh.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Một lượt đặt sân. Tiền được tính như sau:
 *   Tổng cần thu = tiền sân (đã chụp lại lúc đặt) + dịch vụ thêm - giảm giá (chỉ chủ sân đặt)
 *   Đã thu       = tổng các phiếu thu chưa bị hủy
 *   Còn lại      = Tổng cần thu - Đã thu
 */
@Entity
@Table(name = "booking")
public class Booking {

    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter NGAY_NGAN = DateTimeFormatter.ofPattern("dd/MM");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "san_bong_id", nullable = false)
    private SanBong sanBong;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status;

    /** Tiền sân được chụp lại lúc đặt: sau này đổi bảng giá cũng không làm đổi booking cũ. */
    @Column(name = "court_fee", nullable = false, precision = 12, scale = 0)
    private BigDecimal courtFee = BigDecimal.ZERO;

    @Column(name = "discount", nullable = false, precision = 12, scale = 0)
    private BigDecimal discount = BigDecimal.ZERO;

    /** Lý do từ chối / hủy. */
    @Column(name = "reason", length = 255)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_in_by_id")
    private User checkedInBy;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<BookingItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id DESC")
    private List<BookingNote> notes = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Payment> payments = new ArrayList<>();

    // ------------------------------------------------------------------
    // Tính tiền
    // ------------------------------------------------------------------

    public BigDecimal getTienDichVu() {
        BigDecimal tong = BigDecimal.ZERO;
        for (BookingItem item : items) {
            tong = tong.add(item.getThanhTien());
        }
        return tong;
    }

    public BigDecimal getTongTien() {
        BigDecimal tong = courtFee.add(getTienDichVu()).subtract(discount);
        return tong.signum() < 0 ? BigDecimal.ZERO : tong;
    }

    public BigDecimal getDaThu() {
        BigDecimal tong = BigDecimal.ZERO;
        for (Payment p : payments) {
            if (!p.isVoided()) {
                tong = tong.add(p.getAmount());
            }
        }
        return tong;
    }

    public BigDecimal getConLai() {
        BigDecimal conLai = getTongTien().subtract(getDaThu());
        return conLai.signum() < 0 ? BigDecimal.ZERO : conLai;
    }

    public PaymentStatus getTrangThaiThanhToan() {
        if (getConLai().signum() == 0) {
            return PaymentStatus.PAID;
        }
        return getDaThu().signum() == 0 ? PaymentStatus.UNPAID : PaymentStatus.PARTIAL;
    }

    /**
     * Nếu khách đã đến (CHECKED_IN) và đã thu đủ tiền thì chuyển sang COMPLETED.
     * @return true nếu trạng thái vừa được đổi
     */
    public boolean hoanTatNeuDuTien() {
        if (status == BookingStatus.CHECKED_IN && getTrangThaiThanhToan() == PaymentStatus.PAID) {
            status = BookingStatus.COMPLETED;
            return true;
        }
        return false;
    }

    /**
     * Khi chủ sân hủy một phiếu thu làm booking đã hoàn tất lại thiếu tiền thì mở lại.
     * @return true nếu trạng thái vừa được đổi
     */
    public boolean moLaiNeuThieuTien() {
        if (status == BookingStatus.COMPLETED && getTrangThaiThanhToan() != PaymentStatus.PAID) {
            status = BookingStatus.CHECKED_IN;
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Trợ giúp hiển thị / kiểm tra
    // ------------------------------------------------------------------

    public long getSoPhut() {
        return Duration.between(startAt, endAt).toMinutes();
    }

    public String getGioText() {
        return startAt.format(GIO) + "–" + endAt.format(GIO);
    }

    public String getNgayText() {
        return startAt.format(NGAY);
    }

    /** Ví dụ: "Nguyễn A · Sân 5A · 18/10 19:00–20:00" (dùng trong thông báo và thông điệp lỗi). */
    public String getMoTa() {
        return customerName + " · " + sanBong.getTen() + " · " + startAt.format(NGAY_NGAN) + " " + getGioText();
    }

    public boolean isChoDuyet() {
        return status == BookingStatus.PENDING_APPROVAL;
    }

    /** Còn được thêm dịch vụ và thu tiền (đã duyệt, chưa hoàn tất). */
    public boolean isDangPhucVu() {
        return status == BookingStatus.CONFIRMED || status == BookingStatus.CHECKED_IN;
    }

    /** Còn được đổi giờ / hủy. */
    public boolean isChuaDienRa() {
        return status == BookingStatus.PENDING_APPROVAL || status == BookingStatus.CONFIRMED;
    }

    public boolean isDangGiuCho() {
        return BookingStatus.GIU_CHO.contains(status);
    }

    // Thêm con (giữ hai phía của quan hệ luôn khớp nhau)

    public void addItem(BookingItem item) {
        item.setBooking(this);
        items.add(item);
    }

    public void addNote(BookingNote note) {
        note.setBooking(this);
        notes.add(note);
    }

    public void addPayment(Payment payment) {
        payment.setBooking(this);
        payments.add(payment);
    }

    // ------------------------------------------------------------------
    // Getter / setter
    // ------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SanBong getSanBong() {
        return sanBong;
    }

    public void setSanBong(SanBong sanBong) {
        this.sanBong = sanBong;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public BigDecimal getCourtFee() {
        return courtFee;
    }

    public void setCourtFee(BigDecimal courtFee) {
        this.courtFee = courtFee;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public User getCheckedInBy() {
        return checkedInBy;
    }

    public void setCheckedInBy(User checkedInBy) {
        this.checkedInBy = checkedInBy;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(LocalDateTime checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public List<BookingItem> getItems() {
        return items;
    }

    public List<BookingNote> getNotes() {
        return notes;
    }

    public List<Payment> getPayments() {
        return payments;
    }
}
