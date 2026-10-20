package com.tri.quanlisanbanh.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tri.quanlisanbanh.config.CauHinhLich;
import com.tri.quanlisanbanh.model.Booking;
import com.tri.quanlisanbanh.model.BookingStatus;
import com.tri.quanlisanbanh.model.SanBong;
import com.tri.quanlisanbanh.repository.BookingRepository;
import com.tri.quanlisanbanh.repository.SanBongRepository;

@Service
public class LichService {

    private final BookingRepository bookingRepository;
    private final SanBongRepository sanBongRepository;
    private final CauHinhLich cauHinh;

    public LichService(BookingRepository bookingRepository, SanBongRepository sanBongRepository, CauHinhLich cauHinh) {
        this.bookingRepository = bookingRepository;
        this.sanBongRepository = sanBongRepository;
        this.cauHinh = cauHinh;
    }

    @Transactional(readOnly = true)
    public LichView lichNgay(LocalDate ngay) {
        List<SanBong> tatCa = sanBongRepository.findAll();
        List<SanBong> hoatDong = tatCa.stream().filter(SanBong::isActive).toList();
        List<Booking> bookings = bookingRepository.timTrongKhoang(
                ngay.atStartOfDay(), ngay.plusDays(1).atStartOfDay(), BookingStatus.GIU_CHO);
        return LichBuilder.build(ngay, hoatDong, tatCa, bookings, cauHinh.gioMoCua(), cauHinh.gioDongCua());
    }
}
