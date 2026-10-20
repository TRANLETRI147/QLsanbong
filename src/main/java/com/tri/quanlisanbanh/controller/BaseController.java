package com.tri.quanlisanbanh.controller;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tri.quanlisanbanh.exception.NghiepVuException;

/**
 * Chạy một thao tác rồi chuyển hướng về trang cũ, kèm thông báo thành công (xanh) hoặc lỗi nghiệp vụ (đỏ).
 */
public abstract class BaseController {

    protected String chay(RedirectAttributes ra, String thongBaoThanhCong, String chuyenDen, Runnable viec) {
        try {
            viec.run();
            if (thongBaoThanhCong != null) {
                ra.addFlashAttribute("thongBao", thongBaoThanhCong);
            }
        } catch (NghiepVuException e) {
            ra.addFlashAttribute("loi", e.getMessage());
        }
        return "redirect:" + chuyenDen;
    }
}
