package com.tri.quanlisanbanh.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.tri.quanlisanbanh.model.Booking;
import com.tri.quanlisanbanh.model.SanBong;
import com.tri.quanlisanbanh.service.LichView.Dong;
import com.tri.quanlisanbanh.service.LichView.Loai;
import com.tri.quanlisanbanh.service.LichView.O;
import com.tri.quanlisanbanh.util.Gio;
import com.tri.quanlisanbanh.util.NhomSan;

/**
 * Dựng lưới lịch (thuần Java, không đụng database nên test được).
 * Với mỗi sân, một khung 30 phút bị "chiếm" bởi mọi booking đặt trên các sân thuộc
 * NhomSan.sanBiAnhHuong(sân đó). Vì vậy khi sân 7 được đặt, 3 cột sân 5 hiện ô "khóa"
 * trong đúng khung giờ đó, và ngược lại.
 */
public final class LichBuilder {

    private LichBuilder() {
    }

    /**
     * @param cotSan   các sân sẽ hiện thành cột (thường là sân đang hoạt động)
     * @param tatCaSan mọi sân (để biết sân 5 nào thuộc sân 7 nào)
     * @param bookings các booking đang giữ chỗ có giờ giao với ngày này
     */
    public static LichView build(LocalDate ngay, Collection<SanBong> cotSan, Collection<SanBong> tatCaSan,
                                 List<Booking> bookings, int gioMo, int gioDong) {
        List<SanBong> cot = new ArrayList<>(cotSan);
        cot.sort(NhomSan.THU_TU);

        int buoc = Gio.BUOC_PHUT;
        int soDong = (gioDong - gioMo) * 60 / buoc;
        LocalDateTime dau = ngay.atTime(gioMo, 0);

        // oTheoCot.get(c)[r] = ô ở cột c, dòng r
        List<O[]> oTheoCot = new ArrayList<>();
        for (SanBong san : cot) {
            Set<Long> anhHuong = NhomSan.sanBiAnhHuong(san, tatCaSan);
            List<Booking> lienQuan = new ArrayList<>();
            for (Booking b : bookings) {
                if (anhHuong.contains(b.getSanBong().getId())) {
                    lienQuan.add(b);
                }
            }
            lienQuan.sort(Comparator.comparing(Booking::getId));

            List<List<Booking>> chiem = new ArrayList<>();
            for (int r = 0; r < soDong; r++) {
                LocalDateTime tu = dau.plusMinutes((long) r * buoc);
                LocalDateTime den = tu.plusMinutes(buoc);
                List<Booking> trongKhung = new ArrayList<>();
                for (Booking b : lienQuan) {
                    if (b.getStartAt().isBefore(den) && b.getEndAt().isAfter(tu)) {
                        trongKhung.add(b);
                    }
                }
                chiem.add(trongKhung);
            }

            O[] cacO = new O[soDong];
            int r = 0;
            while (r < soDong) {
                List<Booking> hienTai = chiem.get(r);
                String gio = Gio.dinhDang(dau.plusMinutes((long) r * buoc), ngay);
                if (hienTai.isEmpty()) {
                    cacO[r] = new O(Loai.TRONG, san.getId(), gio, 1, null, List.of());
                    r++;
                    continue;
                }
                int j = r + 1;
                while (j < soDong && giongNhau(chiem.get(j), hienTai)) {
                    j++;
                }
                boolean cuaSan = hienTai.size() == 1 && hienTai.get(0).getSanBong().getId().equals(san.getId());
                cacO[r] = cuaSan
                        ? new O(Loai.CUA_SAN, san.getId(), gio, j - r, hienTai.get(0), List.of())
                        : new O(Loai.KHOA, san.getId(), gio, j - r, null, hienTai);
                for (int k = r + 1; k < j; k++) {
                    cacO[k] = new O(Loai.BO_QUA, san.getId(), gio, 1, null, List.of());
                }
                r = j;
            }
            oTheoCot.add(cacO);
        }

        List<Dong> dong = new ArrayList<>();
        for (int r = 0; r < soDong; r++) {
            LocalDateTime tu = dau.plusMinutes((long) r * buoc);
            List<O> cacO = new ArrayList<>();
            for (O[] cacOCuaCot : oTheoCot) {
                cacO.add(cacOCuaCot[r]);
            }
            dong.add(new Dong(Gio.dinhDang(tu, ngay), tu.getMinute() == 0, cacO));
        }
        return new LichView(cot, dong);
    }

    private static boolean giongNhau(List<Booking> a, List<Booking> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).getId().equals(b.get(i).getId())) {
                return false;
            }
        }
        return true;
    }
}
