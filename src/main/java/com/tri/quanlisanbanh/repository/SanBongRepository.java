package com.tri.quanlisanbanh.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tri.quanlisanbanh.model.SanBong;

import jakarta.persistence.LockModeType;

public interface SanBongRepository extends JpaRepository<SanBong, Long> {

    boolean existsByTen(String ten);

    boolean existsByTenAndIdNot(String ten, Long id);

    /** Số sân 5 đang thuộc một sân 7. */
    long countBySanLonId(Long sanLonId);

    /**
     * Khóa dòng của sân "gốc" trong cụm (SELECT ... FOR UPDATE).
     * Mọi lượt đặt sân trong cùng một cụm (sân 7 + 3 sân 5) đều khóa cùng một dòng này
     * nên được xử lý lần lượt từng cái một => không thể lọt 2 lượt đặt trùng giờ cùng lúc.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SanBong s where s.id = :id")
    Optional<SanBong> findByIdForUpdate(@Param("id") Long id);
}
