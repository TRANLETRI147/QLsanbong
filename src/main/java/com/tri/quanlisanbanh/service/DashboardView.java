package com.tri.quanlisanbanh.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.tri.quanlisanbanh.model.Booking;
import com.tri.quanlisanbanh.model.Payment;

/** Số liệu cho trang tổng quan của chủ sân. */
public class DashboardView {

    private BigDecimal doanhThuHomNay = BigDecimal.ZERO;
    private BigDecimal doanhThu7Ngay = BigDecimal.ZERO;
    private BigDecimal doanhThuThang = BigDecimal.ZERO;
    private long soBookingHomNay;
    private long soChoDuyet;
    private List<Booking> chuaThuDu = List.of();
    private List<Payment> thuHomNay = List.of();
    private Map<String, BigDecimal> thuTheoNhanVien = Map.of();
    private List<String> nhan7Ngay = List.of();
    private List<Long> giaTri7Ngay = List.of();

    public BigDecimal getDoanhThuHomNay() {
        return doanhThuHomNay;
    }

    public void setDoanhThuHomNay(BigDecimal doanhThuHomNay) {
        this.doanhThuHomNay = doanhThuHomNay;
    }

    public BigDecimal getDoanhThu7Ngay() {
        return doanhThu7Ngay;
    }

    public void setDoanhThu7Ngay(BigDecimal doanhThu7Ngay) {
        this.doanhThu7Ngay = doanhThu7Ngay;
    }

    public BigDecimal getDoanhThuThang() {
        return doanhThuThang;
    }

    public void setDoanhThuThang(BigDecimal doanhThuThang) {
        this.doanhThuThang = doanhThuThang;
    }

    public long getSoBookingHomNay() {
        return soBookingHomNay;
    }

    public void setSoBookingHomNay(long soBookingHomNay) {
        this.soBookingHomNay = soBookingHomNay;
    }

    public long getSoChoDuyet() {
        return soChoDuyet;
    }

    public void setSoChoDuyet(long soChoDuyet) {
        this.soChoDuyet = soChoDuyet;
    }

    public List<Booking> getChuaThuDu() {
        return chuaThuDu;
    }

    public void setChuaThuDu(List<Booking> chuaThuDu) {
        this.chuaThuDu = chuaThuDu;
    }

    public List<Payment> getThuHomNay() {
        return thuHomNay;
    }

    public void setThuHomNay(List<Payment> thuHomNay) {
        this.thuHomNay = thuHomNay;
    }

    public Map<String, BigDecimal> getThuTheoNhanVien() {
        return thuTheoNhanVien;
    }

    public void setThuTheoNhanVien(Map<String, BigDecimal> thuTheoNhanVien) {
        this.thuTheoNhanVien = thuTheoNhanVien;
    }

    public List<String> getNhan7Ngay() {
        return nhan7Ngay;
    }

    public void setNhan7Ngay(List<String> nhan7Ngay) {
        this.nhan7Ngay = nhan7Ngay;
    }

    public List<Long> getGiaTri7Ngay() {
        return giaTri7Ngay;
    }

    public void setGiaTri7Ngay(List<Long> giaTri7Ngay) {
        this.giaTri7Ngay = giaTri7Ngay;
    }
}
