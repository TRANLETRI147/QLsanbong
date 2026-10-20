package com.tri.quanlisanbanh.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tri.quanlisanbanh.TestData;
import com.tri.quanlisanbanh.model.LoaiSan;
import com.tri.quanlisanbanh.model.SanBong;

class NhomSanTest {

    private final SanBong san7A = TestData.san(1, "Sân 7 A", LoaiSan.SAN_7, 300000, null);
    private final SanBong a1 = TestData.san(2, "Sân 5 A-1", LoaiSan.SAN_5, 150000, san7A);
    private final SanBong a2 = TestData.san(3, "Sân 5 A-2", LoaiSan.SAN_5, 150000, san7A);
    private final SanBong a3 = TestData.san(4, "Sân 5 A-3", LoaiSan.SAN_5, 150000, san7A);
    private final SanBong le = TestData.san(9, "Sân lẻ", LoaiSan.SAN_5, 100000, null);
    private final List<SanBong> tatCa = List.of(san7A, a1, a2, a3, le);

    @Test
    void san7_khoaCaBaSan5CuaNo() {
        assertEquals(Set.of(1L, 2L, 3L, 4L), NhomSan.sanBiAnhHuong(san7A, tatCa));
    }

    @Test
    void san5_chiKhoaSan7ChuaNo_khongDungToiSan5Khac() {
        assertEquals(Set.of(2L, 1L), NhomSan.sanBiAnhHuong(a1, tatCa));
        assertEquals(Set.of(3L, 1L), NhomSan.sanBiAnhHuong(a2, tatCa));
    }

    @Test
    void sanLe_chiAnhHuongChinhNo() {
        assertEquals(Set.of(9L), NhomSan.sanBiAnhHuong(le, tatCa));
    }

    @Test
    void idGoc_laSan7NeuCo() {
        assertEquals(1L, NhomSan.idGoc(san7A));
        assertEquals(1L, NhomSan.idGoc(a2));
        assertEquals(9L, NhomSan.idGoc(le));
    }

    @Test
    void thuTu_san7DungDauCum() {
        List<SanBong> ds = new ArrayList<>(List.of(a3, le, a1, san7A, a2));
        ds.sort(NhomSan.THU_TU);
        // cụm A (gốc 1) trước, sân lẻ (gốc 9) sau; trong cụm A sân 7 đứng đầu
        assertEquals(List.of(1L, 2L, 3L, 4L, 9L), ds.stream().map(SanBong::getId).toList());
        assertTrue(ds.get(0).isSan7());
    }
}
