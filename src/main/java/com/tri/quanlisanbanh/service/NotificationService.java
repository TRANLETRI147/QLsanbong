package com.tri.quanlisanbanh.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tri.quanlisanbanh.model.Booking;
import com.tri.quanlisanbanh.model.Notification;
import com.tri.quanlisanbanh.model.NotificationType;
import com.tri.quanlisanbanh.model.User;
import com.tri.quanlisanbanh.repository.NotificationRepository;

/** Thông báo gửi lên chủ sân (hiện ở biểu tượng chuông trên thanh menu). */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    public NotificationService(NotificationRepository notificationRepository, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.clock = clock;
    }

    @Transactional
    public void guiChoChu(NotificationType loai, Booking booking, User nguoiGui, String noiDung) {
        Notification n = new Notification();
        n.setType(loai);
        n.setBooking(booking);
        n.setCreatedBy(nguoiGui);
        n.setMessage(noiDung.length() > 500 ? noiDung.substring(0, 497) + "..." : noiDung);
        n.setCreatedAt(LocalDateTime.now(clock));
        notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public List<Notification> moiNhat() {
        return notificationRepository.findTop50ByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public long soChuaDoc() {
        return notificationRepository.countByReadAtIsNull();
    }

    @Transactional
    public void danhDauDaDocHet() {
        notificationRepository.danhDauDaDocHet(LocalDateTime.now(clock));
    }
}
