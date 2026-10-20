package com.tri.quanlisanbanh.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tri.quanlisanbanh.service.BookingService;
import com.tri.quanlisanbanh.service.LichService;

/** Màn hình chính của nhân viên: lưới lịch các sân trong một ngày. */
@Controller
public class LichController {

    private final LichService lichService;
    private final BookingService bookingService;

    public LichController(LichService lichService, BookingService bookingService) {
        this.lichService = lichService;
        this.bookingService = bookingService;
    }

    @GetMapping("/lich")
    public String lich(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
                       Model model) {
        LocalDate homNay = bookingService.homNay();
        LocalDate d = ngay != null ? ngay : homNay;
        model.addAttribute("ngay", d);
        model.addAttribute("homNay", homNay);
        model.addAttribute("truoc", d.minusDays(1));
        model.addAttribute("sau", d.plusDays(1));
        model.addAttribute("lich", lichService.lichNgay(d));
        model.addAttribute("daNhaCho", bookingService.daNhaCho(d));
        return "lich";
    }
}
