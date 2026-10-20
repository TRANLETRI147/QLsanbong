package com.tri.quanlisanbanh.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tri.quanlisanbanh.exception.NghiepVuException;
import com.tri.quanlisanbanh.model.BookingStatus;
import com.tri.quanlisanbanh.model.LoaiSan;
import com.tri.quanlisanbanh.model.SanBong;
import com.tri.quanlisanbanh.repository.BookingRepository;
import com.tri.quanlisanbanh.repository.SanBongRepository;
import com.tri.quanlisanbanh.util.NhomSan;

@Service
public class SanBongService {

    private final SanBongRepository sanBongRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;

    public SanBongService(SanBongRepository sanBongRepository, BookingRepository bookingRepository, Clock clock) {
        this.sanBongRepository = sanBongRepository;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    /** Mọi sân (cả sân đã ngừng) theo thứ tự cụm: sân 7 rồi tới các sân 5 của nó. */
    @Transactional(readOnly = true)
    public List<SanBong> tatCa() {
        List<SanBong> ds = new ArrayList<>(sanBongRepository.findAll());
        ds.sort(NhomSan.THU_TU);
        return ds;
    }

    @Transactional(readOnly = true)
    public List<SanBong> dangHoatDong() {
        List<SanBong> ds = new ArrayList<>();
        for (SanBong s : tatCa()) {
            if (s.isActive()) {
                ds.add(s);
            }
        }
        return ds;
    }

    @Transactional(readOnly = true)
    public List<SanBong> cacSan7() {
        List<SanBong> ds = new ArrayList<>();
        for (SanBong s : tatCa()) {
            if (s.isActive() && s.isSan7()) {
                ds.add(s);
            }
        }
        return ds;
    }

    @Transactional(readOnly = true)
    public SanBong lay(Long id) {
        return sanBongRepository.findById(id).orElseThrow(() -> new NghiepVuException("Không tìm thấy sân."));
    }

    /** Các sân bị ảnh hưởng khi đặt sân này (chính nó + sân 7 chứa nó, hoặc 3 sân 5 của nó). */
    @Transactional(readOnly = true)
    public Set<Long> sanBiAnhHuong(SanBong san) {
        return NhomSan.sanBiAnhHuong(san, sanBongRepository.findAll());
    }

    /** Tạo nhanh một cụm: 1 sân 7 + 3 sân 5 thuộc sân 7 đó. */
    @Transactional
    public void taoCum(String tenCum, BigDecimal gia5, BigDecimal gia7) {
        String ten = chuanHoaTen(tenCum);
        SanBong san7 = tao("Sân 7 " + ten, LoaiSan.SAN_7, gia7, null);
        for (int i = 1; i <= LoaiSan.SO_SAN_5_TREN_SAN_7; i++) {
            tao("Sân 5 " + ten + "-" + i, LoaiSan.SAN_5, gia5, san7.getId());
        }
    }

    @Transactional
    public SanBong tao(String ten, LoaiSan loai, BigDecimal gia, Long sanLonId) {
        String tenChuan = chuanHoaTen(ten);
        if (loai == null) {
            throw new NghiepVuException("Chưa chọn loại sân.");
        }
        if (sanBongRepository.existsByTen(tenChuan)) {
            throw new NghiepVuException("Đã có sân tên \"" + tenChuan + "\".");
        }
        SanBong san = new SanBong();
        san.setTen(tenChuan);
        san.setLoai(loai);
        san.setGiaMoiGio(gia);
        san.setSanLon(timSanLon(loai, sanLonId, null));
        san.setActive(true);
        return sanBongRepository.save(san);
    }

    @Transactional
    public void capNhat(Long id, String ten, BigDecimal gia, boolean active, Long sanLonId) {
        SanBong san = lay(id);
        String tenChuan = chuanHoaTen(ten);
        if (sanBongRepository.existsByTenAndIdNot(tenChuan, id)) {
            throw new NghiepVuException("Đã có sân khác tên \"" + tenChuan + "\".");
        }
        SanBong sanLonMoi = timSanLon(san.getLoai(), sanLonId, san);
        Long idCu = san.getSanLon() == null ? null : san.getSanLon().getId();
        Long idMoi = sanLonMoi == null ? null : sanLonMoi.getId();
        boolean doiNhom = idCu == null ? idMoi != null : !idCu.equals(idMoi);

        // Đổi nhóm hoặc ngừng hoạt động khi còn booking sắp tới sẽ làm lịch bị rối / mất booking khỏi lưới.
        if ((doiNhom || (san.isActive() && !active)) && coBookingSapToi(id)) {
            throw new NghiepVuException("Sân đang có booking sắp tới nên chưa thể đổi nhóm hoặc ngừng hoạt động. "
                    + "Hãy chuyển/hủy các booking đó trước.");
        }
        san.setTen(tenChuan);
        san.setGiaMoiGio(gia);
        san.setActive(active);
        san.setSanLon(sanLonMoi);
    }

    // ------------------------------------------------------------------

    /** Kiểm tra quy tắc nhóm và trả về sân 7 cần gắn vào (null nếu sân độc lập). */
    private SanBong timSanLon(LoaiSan loai, Long sanLonId, SanBong dangSua) {
        if (sanLonId == null) {
            return null;
        }
        if (loai != LoaiSan.SAN_5) {
            throw new NghiepVuException("Chỉ sân 5 mới thuộc về một sân 7.");
        }
        SanBong san7 = sanBongRepository.findById(sanLonId)
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy sân 7 đã chọn."));
        if (san7.getLoai() != LoaiSan.SAN_7) {
            throw new NghiepVuException("\"" + san7.getTen() + "\" không phải là sân 7.");
        }
        long daCo = sanBongRepository.countBySanLonId(sanLonId);
        boolean dangThuocSanNay = dangSua != null && dangSua.getSanLon() != null
                && sanLonId.equals(dangSua.getSanLon().getId());
        if (!dangThuocSanNay && daCo >= LoaiSan.SO_SAN_5_TREN_SAN_7) {
            throw new NghiepVuException(san7.getTen() + " đã đủ " + LoaiSan.SO_SAN_5_TREN_SAN_7 + " sân 5.");
        }
        return san7;
    }

    private boolean coBookingSapToi(Long sanId) {
        return bookingRepository.existsBySanBongIdAndStatusInAndEndAtAfter(
                sanId, BookingStatus.GIU_CHO, LocalDateTime.now(clock));
    }

    private String chuanHoaTen(String ten) {
        String t = ten == null ? "" : ten.trim().replaceAll("\\s+", " ");
        if (t.isEmpty() || t.length() > 100) {
            throw new NghiepVuException("Tên sân không được để trống (tối đa 100 ký tự).");
        }
        return t;
    }
}
