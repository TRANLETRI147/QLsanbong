package com.tri.quanlisanbanh.controller;

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

import com.tri.quanlisanbanh.model.User;
import com.tri.quanlisanbanh.service.BookingService;
import com.tri.quanlisanbanh.service.DashboardService;
import com.tri.quanlisanbanh.service.NotificationService;
import com.tri.quanlisanbanh.service.ThanhToanService;
import com.tri.quanlisanbanh.service.UserService;
import com.tri.quanlisanbanh.util.Tien;

/** Mọi thứ dưới /owner/** chỉ chủ sân vào được (SecurityConfig), và service cũng kiểm tra lại vai trò. */
@Controller
public class OwnerController extends BaseController {

    private final DashboardService dashboardService;
    private final BookingService bookingService;
    private final ThanhToanService thanhToanService;
    private final NotificationService notificationService;
    private final UserService userService;

    public OwnerController(DashboardService dashboardService, BookingService bookingService,
                           ThanhToanService thanhToanService, NotificationService notificationService,
                           UserService userService) {
        this.dashboardService = dashboardService;
        this.bookingService = bookingService;
        this.thanhToanService = thanhToanService;
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping("/owner/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("tq", dashboardService.tongQuan());
        return "owner/dashboard";
    }

    // ------------------------------------------------------------------
    // Duyệt booking
    // ------------------------------------------------------------------

    @GetMapping("/owner/cho-duyet")
    public String choDuyet(Model model) {
        model.addAttribute("ds", bookingService.choDuyet());
        return "owner/cho-duyet";
    }

    @PostMapping("/owner/booking/{id}/duyet")
    public String duyet(@PathVariable Long id, Principal principal,
                        @RequestParam(defaultValue = "/owner/cho-duyet") String quayLai, RedirectAttributes ra) {
        return chay(ra, "Đã duyệt booking #" + id + ".", diaChiAnToan(quayLai, id),
                () -> bookingService.duyet(id, nguoiDung(principal)));
    }

    @PostMapping("/owner/cho-duyet/duyet-het")
    public String duyetHet(Principal principal, RedirectAttributes ra) {
        return chay(ra, "Đã duyệt tất cả booking đang chờ.", "/owner/cho-duyet",
                () -> bookingService.duyetTatCa(nguoiDung(principal)));
    }

    @PostMapping("/owner/booking/{id}/tu-choi")
    public String tuChoi(@PathVariable Long id, Principal principal,
                         @RequestParam(required = false) String lyDo,
                         @RequestParam(defaultValue = "/owner/cho-duyet") String quayLai, RedirectAttributes ra) {
        return chay(ra, "Đã từ chối booking #" + id + ". Sân đã được nhả.", diaChiAnToan(quayLai, id),
                () -> bookingService.tuChoi(id, nguoiDung(principal), lyDo));
    }

    @PostMapping("/owner/booking/{id}/huy")
    public String huy(@PathVariable Long id, Principal principal, @RequestParam(required = false) String lyDo,
                      RedirectAttributes ra) {
        return chay(ra, "Đã hủy booking #" + id + ". Sân đã được nhả.", "/booking/" + id,
                () -> bookingService.huy(id, nguoiDung(principal), lyDo));
    }

    @PostMapping("/owner/booking/{id}/khong-den")
    public String khongDen(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        return chay(ra, "Đã đánh dấu khách không đến. Sân đã được nhả.", "/booking/" + id,
                () -> bookingService.khongDen(id, nguoiDung(principal)));
    }

    @PostMapping("/owner/booking/{id}/doi-gio")
    public String doiGio(@PathVariable Long id, Principal principal,
                         @RequestParam Long sanId,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
                         @RequestParam String batDau,
                         @RequestParam String ketThuc,
                         RedirectAttributes ra) {
        return chay(ra, "Đã đổi lịch booking #" + id + ".", "/booking/" + id,
                () -> bookingService.doiGio(id, nguoiDung(principal), sanId, ngay, batDau, ketThuc));
    }

    // ------------------------------------------------------------------
    // Tiền: giảm giá, hủy phiếu thu
    // ------------------------------------------------------------------

    @PostMapping("/owner/booking/{id}/giam-gia")
    public String giamGia(@PathVariable Long id, Principal principal, @RequestParam String soTien,
                          RedirectAttributes ra) {
        return chay(ra, "Đã cập nhật giảm giá.", "/booking/" + id,
                () -> thanhToanService.giamGia(id, nguoiDung(principal), Tien.parse(soTien)));
    }

    @PostMapping("/owner/booking/{id}/phieu-thu/{paymentId}/huy")
    public String huyPhieuThu(@PathVariable Long id, @PathVariable Long paymentId, Principal principal,
                              RedirectAttributes ra) {
        return chay(ra, "Đã hủy phiếu thu.", "/booking/" + id,
                () -> thanhToanService.huyPhieuThu(id, paymentId, nguoiDung(principal)));
    }

    // ------------------------------------------------------------------
    // Thông báo (chuông)
    // ------------------------------------------------------------------

    @GetMapping("/owner/thong-bao")
    public String thongBao(Model model) {
        model.addAttribute("ds", notificationService.moiNhat());
        return "owner/thong-bao";
    }

    @PostMapping("/owner/thong-bao/doc-het")
    public String docHet(RedirectAttributes ra) {
        notificationService.danhDauDaDocHet();
        ra.addFlashAttribute("thongBao", "Đã đánh dấu đã đọc tất cả.");
        return "redirect:/owner/thong-bao";
    }

    /** Mảnh HTML nhỏ của quả chuông, trình duyệt tự gọi lại mỗi 20 giây (HTMX). */
    @GetMapping("/owner/thong-bao/badge")
    public String badge(Model model) {
        model.addAttribute("chuaDoc", notificationService.soChuaDoc());
        return "fragments :: badge";
    }

    // ------------------------------------------------------------------

    private User nguoiDung(Principal principal) {
        return userService.theoTen(principal.getName());
    }

    /** Chỉ cho quay về các trang nội bộ đã biết (tránh bị lợi dụng chuyển hướng sang trang lạ). */
    private String diaChiAnToan(String quayLai, Long id) {
        return "/booking/".concat(String.valueOf(id)).equals(quayLai) ? quayLai : "/owner/cho-duyet";
    }
}
