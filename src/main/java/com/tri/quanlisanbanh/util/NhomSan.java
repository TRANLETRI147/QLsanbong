package com.tri.quanlisanbanh.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.tri.quanlisanbanh.model.LoaiSan;
import com.tri.quanlisanbanh.model.SanBong;

/**
 * Quy tắc "1 sân 7 = 3 sân 5" nằm hết ở đây (thuần Java, không phụ thuộc database nên test được).
 *
 * Ví dụ cụm A gồm: Sân 7 A, Sân 5 A-1, Sân 5 A-2, Sân 5 A-3
 *   sanBiAnhHuong(Sân 7 A)   = {Sân 7 A, A-1, A-2, A-3}   (đặt sân 7 là chiếm cả 3 sân 5)
 *   sanBiAnhHuong(Sân 5 A-1) = {A-1, Sân 7 A}              (không đụng tới A-2, A-3)
 *   sanBiAnhHuong(sân lẻ)    = {chính nó}
 * Hai lượt đặt xung đột khi trùng giờ VÀ sân của chúng nằm trong tập sanBiAnhHuong của nhau.
 */
public final class NhomSan {

    /** Sắp xếp để các sân cùng cụm nằm cạnh nhau, sân 7 đứng đầu cụm. */
    public static final Comparator<SanBong> THU_TU = Comparator
            .comparing((SanBong s) -> idGoc(s))
            .thenComparing(s -> s.getLoai() == LoaiSan.SAN_7 ? 0 : 1)
            .thenComparing(SanBong::getId);

    private NhomSan() {
    }

    /** Id của sân "gốc" trong cụm: sân 7 nếu có, ngược lại là chính nó. Dùng để khóa dòng DB khi đặt sân. */
    public static Long idGoc(SanBong san) {
        return san.getSanLon() != null ? san.getSanLon().getId() : san.getId();
    }

    public static Set<Long> sanBiAnhHuong(SanBong san, Collection<SanBong> tatCaSan) {
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(san.getId());
        if (san.getSanLon() != null) {
            ids.add(san.getSanLon().getId());
        } else if (san.getLoai() == LoaiSan.SAN_7) {
            for (SanBong khac : tatCaSan) {
                if (khac.getSanLon() != null && Objects.equals(khac.getSanLon().getId(), san.getId())) {
                    ids.add(khac.getId());
                }
            }
        }
        return ids;
    }
}
