package com.tri.quanlisanbanh.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tri.quanlisanbanh.config.CauHinhLich;
import com.tri.quanlisanbanh.exception.NghiepVuException;
import com.tri.quanlisanbanh.model.Booking;
import com.tri.quanlisanbanh.model.PaymentMethod;
import com.tri.quanlisanbanh.model.User;
import com.tri.quanlisanbanh.service.BookingService;
import com.tri.quanlisanbanh.service.SanBongService;
import com.tri.quanlisanbanh.service.ThanhToanService;
import com.tri.quanlisanbanh.service.UserService;
import com.tri.quanlisanbanh.util.Gio;
import com.tri.quanlisanbanh.util.Tien;

/**
 * Các thao tác trên booking mà CẢ nhân viên và chủ sân đều làm được:
 * tạo booking, xem chi tiết, check-in, ghi chú, thêm dịch vụ, thu tiền, xem hóa đơn.
 */
@Controller
public class BookingController extends BaseController {

    private final BookingService bookingService;
    private final ThanhToanService thanhToanService;
    private final SanBongService sanBongService;
    private final UserService userService;
    private final CauHinhLich cauHinh;

    public BookingController(BookingService bookingService, ThanhToanService thanhToanService,
                             SanBongService sanBongService, UserService userService, CauHinhLich cauHinh) {
        this.bookingService = bookingService;
        this.thanhToanService = thanhToanService;
        this.sanBongService = sanBongService;
        this.userService = userService;
        this.cauHinh = cauHinh;
    }

    // ------------------------------------------------------------------
    // Tạo booking
    // ------------------------------------------------------------------

    @GetMapping("/booking/new")
    public String formTao(@RequestParam(required = false) Long sanId,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
                          @RequestParam(required = false) String gio,
                          Model model) {
        String batDau = gio != null ? gio : "18:00";
        guiDuLieuForm(model, sanId, ngay != null ? ngay : bookingService.homNay(), batDau, macDinhKetThuc(batDau));
        return "booking-form";
    }

    @PostMapping("/booking")
    public String tao(Principal principal,
                      @RequestParam Long sanId,
                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
                      @RequestParam String batDau,
                      @RequestParam String ketThuc,
                      @RequestParam String tenKhach,
                      @RequestParam(required = false) String sdtKhach,
                      @RequestParam(required = false) String ghiChu,
                      Model model, RedirectAttributes ra) {
        try {
            User me = userService.theoTen(principal.getName());
            Booking b = bookingService.tao(me, sanId, tenKhach, sdtKhach, ngay, batDau, ketThuc, ghiChu);
            ra.addFlashAttribute("thongBao", me.isChu()
                    ? "Đã tạo booking #" + b.getId() + " (tự động duyệt)."
                    : "Đã tạo booking #" + b.getId() + ". Đang giữ chỗ, chờ chủ sân duyệt.");
            return "redirect:/lich?ngay=" + ngay;
        } catch (NghiepVuException e) {
            // Hiện lại form với dữ liệu vừa nhập để người dùng sửa
            guiDuLieuForm(model, sanId, ngay, batDau, ketThuc);
            model.addAttribute("tenKhach", tenKhach);
            model.addAttribute("sdtKhach", sdtKhach);
            model.addAttribute("ghiChu", ghiChu);
            model.addAttribute("loi", e.getMessage());
            return "booking-form";
        }
    }

    // ------------------------------------------------------------------
    // Chi tiết và hóa đơn
    // ------------------------------------------------------------------

    @GetMapping("/booking/{id}")
    public String chiTiet(@PathVariable Long id, Model model) {
        Booking b = bookingService.lay(id);
        model.addAttribute("b", b);
        model.addAttribute("ngayB", b.getStartAt().toLocalDate());
        model.addAttribute("batDauB", Gio.dinhDang(b.getStartAt(), b.getStartAt().toLocalDate()));
        model.addAttribute("ketThucB", Gio.dinhDang(b.getEndAt(), b.getStartAt().toLocalDate()));
        model.addAttribute("cacSan", sanBongService.dangHoatDong());
        model.addAttribute("mocBatDau", Gio.cacMocBatDau(cauHinh.gioMoCua(), cauHinh.gioDongCua()));
        model.addAttribute("mocKetThuc", Gio.cacMocKetThuc(cauHinh.gioMoCua(), cauHinh.gioDongCua()));
        model.addAttribute("hinhThuc", PaymentMethod.values());
        return "booking-detail";
    }

    @GetMapping("/booking/{id}/hoa-don")
    public String hoaDon(@PathVariable Long id, Model model) {
        model.addAttribute("b", bookingService.lay(id));
        model.addAttribute("bayGio", bookingService.bayGio());
        return "hoa-don";
    }

    // ------------------------------------------------------------------
    // Thao tác của nhân viên
    // ------------------------------------------------------------------

    @PostMapping("/booking/{id}/check-in")
    public String checkIn(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        return chay(ra, "Đã check-in.", "/booking/" + id,
                () -> bookingService.checkIn(id, userService.theoTen(principal.getName())));
    }

    @PostMapping("/booking/{id}/ghi-chu")
    public String ghiChu(@PathVariable Long id, Principal principal,
                         @RequestParam String noiDung,
                         @RequestParam(defaultValue = "false") boolean baoChu,
                         RedirectAttributes ra) {
        return chay(ra, "Đã thêm ghi chú.", "/booking/" + id,
                () -> bookingService.themGhiChu(id, userService.theoTen(principal.getName()), noiDung, baoChu));
    }

    @PostMapping("/booking/{id}/mon")
    public String themMon(@PathVariable Long id, Principal principal,
                          @RequestParam String ten,
                          @RequestParam(defaultValue = "1") int soLuong,
                          @RequestParam String donGia,
                          RedirectAttributes ra) {
        return chay(ra, "Đã thêm dịch vụ.", "/booking/" + id,
                () -> thanhToanService.themMon(id, userService.theoTen(principal.getName()), ten, soLuong,
                        Tien.parse(donGia)));
    }

    @PostMapping("/booking/{id}/mon/{itemId}/xoa")
    public String xoaMon(@PathVariable Long id, @PathVariable Long itemId, Principal principal,
                         RedirectAttributes ra) {
        return chay(ra, "Đã xóa dịch vụ.", "/booking/" + id,
                () -> thanhToanService.xoaMon(id, itemId, userService.theoTen(principal.getName())));
    }

    /** Nhân viên bấm "Đã thu tiền": ghi phiếu thu và gửi thông báo lên chủ sân. */
    @PostMapping("/booking/{id}/thu-tien")
    public String thuTien(@PathVariable Long id, Principal principal,
                          @RequestParam String soTien,
                          @RequestParam PaymentMethod hinhThuc,
                          RedirectAttributes ra) {
        return chay(ra, "Đã ghi nhận thu tiền và báo lên chủ sân.", "/booking/" + id,
                () -> thanhToanService.thuTien(id, userService.theoTen(principal.getName()), Tien.parse(soTien),
                        hinhThuc));
    }

    // ------------------------------------------------------------------

    private void guiDuLieuForm(Model model, Long sanId, LocalDate ngay, String batDau, String ketThuc) {
        model.addAttribute("cacSan", sanBongService.dangHoatDong());
        model.addAttribute("mocBatDau", Gio.cacMocBatDau(cauHinh.gioMoCua(), cauHinh.gioDongCua()));
        model.addAttribute("mocKetThuc", Gio.cacMocKetThuc(cauHinh.gioMoCua(), cauHinh.gioDongCua()));
        model.addAttribute("sanId", sanId);
        model.addAttribute("ngay", ngay);
        model.addAttribute("batDau", batDau);
        model.addAttribute("ketThuc", ketThuc);
    }

    /** Mặc định đá 1 tiếng kể từ giờ bắt đầu (nhưng không quá giờ đóng cửa). */
    private String macDinhKetThuc(String batDau) {
        try {
            String[] p = batDau.split(":");
            int phut = Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]) + 60;
            phut = Math.min(phut, cauHinh.gioDongCua() * 60);
            return String.format("%02d:%02d", phut / 60, phut % 60);
        } catch (RuntimeException e) {
            return "19:00";
        }
    }
}
