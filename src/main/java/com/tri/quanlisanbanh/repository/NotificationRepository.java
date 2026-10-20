package com.tri.quanlisanbanh.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tri.quanlisanbanh.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByOrderByIdDesc();

    long countByReadAtIsNull();

    @Modifying
    @Query("update Notification n set n.readAt = :luc where n.readAt is null")
    int danhDauDaDocHet(@Param("luc") LocalDateTime luc);
}
