package com.tri.quanlisanbanh.model;

import java.math.BigDecimal;

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
import jakarta.persistence.Table;

/**
 * Một sân có thể đặt. Quy tắc khóa sân 7 / sân 5:
 *  - Sân 5 có thể thuộc về một sân 7 (trường sanLon). Một sân 7 gồm tối đa 3 sân 5.
 *  - Đặt sân 7  => khóa cả 3 sân 5 của nó.
 *  - Đặt 1 sân 5 => khóa sân 7 chứa nó (nhưng 2 sân 5 còn lại vẫn đặt được).
 */
@Entity
@Table(name = "san_bong")
public class SanBong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten", nullable = false, length = 100)
    private String ten;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai", nullable = false, length = 10)
    private LoaiSan loai;

    @Column(name = "gia_moi_gio", nullable = false, precision = 12, scale = 0)
    private BigDecimal giaMoiGio = BigDecimal.ZERO;

    /** Chỉ sân 5 mới có giá trị này: là sân 7 mà sân 5 này là một phần. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "san_lon_id")
    private SanBong sanLon;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTen() {
        return ten;
    }

    public void setTen(String ten) {
        this.ten = ten;
    }

    public LoaiSan getLoai() {
        return loai;
    }

    public void setLoai(LoaiSan loai) {
        this.loai = loai;
    }

    public BigDecimal getGiaMoiGio() {
        return giaMoiGio;
    }

    public void setGiaMoiGio(BigDecimal giaMoiGio) {
        this.giaMoiGio = giaMoiGio;
    }

    public SanBong getSanLon() {
        return sanLon;
    }

    public void setSanLon(SanBong sanLon) {
        this.sanLon = sanLon;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isSan7() {
        return loai == LoaiSan.SAN_7;
    }
}
