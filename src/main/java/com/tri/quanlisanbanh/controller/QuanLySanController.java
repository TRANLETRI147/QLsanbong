package com.tri.quanlisanbanh.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tri.quanlisanbanh.model.LoaiSan;
import com.tri.quanlisanbanh.service.SanBongService;
import com.tri.quanlisanbanh.util.Tien;

/** Chủ sân quản lý danh sách sân và cách ghép 3 sân 5 thành 1 sân 7. */
@Controller
public class QuanLySanController extends BaseController {

    private final SanBongService sanBongService;

    public QuanLySanController(SanBongService sanBongService) {
        this.sanBongService = sanBongService;
    }

    @GetMapping("/owner/san")
    public String danhSach(Model model) {
        model.addAttribute("dsSan", sanBongService.tatCa());
        model.addAttribute("cacSan7", sanBongService.cacSan7());
        model.addAttribute("soSan5", LoaiSan.SO_SAN_5_TREN_SAN_7);
        return "owner/san";
    }

    /** Tạo nhanh 1 sân 7 + 3 sân 5 thuộc sân 7 đó. */
    @PostMapping("/owner/san/cum")
    public String taoCum(@RequestParam String tenCum, @RequestParam String gia5, @RequestParam String gia7,
                         RedirectAttributes ra) {
        return chay(ra, "Đã tạo cụm sân \"" + tenCum.trim() + "\" gồm 1 sân 7 và 3 sân 5.", "/owner/san",
                () -> sanBongService.taoCum(tenCum, Tien.parse(gia5), Tien.parse(gia7)));
    }

    @PostMapping("/owner/san")
    public String taoSan(@RequestParam String ten, @RequestParam LoaiSan loai, @RequestParam String gia,
                         @RequestParam(required = false) Long sanLonId, RedirectAttributes ra) {
        return chay(ra, "Đã thêm sân.", "/owner/san",
                () -> sanBongService.tao(ten, loai, Tien.parse(gia), sanLonId));
    }

    @PostMapping("/owner/san/{id}")
    public String capNhat(@PathVariable Long id, @RequestParam String ten, @RequestParam String gia,
                          @RequestParam(defaultValue = "false") boolean active,
                          @RequestParam(required = false) Long sanLonId, RedirectAttributes ra) {
        return chay(ra, "Đã lưu thay đổi.", "/owner/san",
                () -> sanBongService.capNhat(id, ten, Tien.parse(gia), active, sanLonId));
    }
}
