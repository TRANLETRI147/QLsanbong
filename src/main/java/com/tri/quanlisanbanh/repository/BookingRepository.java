package com.tri.quanlisanbanh.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tri.quanlisanbanh.model.Booking;
import com.tri.quanlisanbanh.model.BookingStatus;

import jakarta.persistence.LockModeType;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Các booking đang giữ chỗ trên những sân này mà giao nhau về giờ (giờ liền kề nhau không tính là trùng). */
    @Query("""
            select b from Booking b join fetch b.sanBong
            where b.sanBong.id in :sanIds
              and b.status in :trangThai
              and b.startAt < :den
              and b.endAt > :tu
              and b.id <> :boQuaId
            order by b.startAt
            """)
    List<Booking> timXungDot(@Param("sanIds") Collection<Long> sanIds,
                             @Param("trangThai") Collection<BookingStatus> trangThai,
                             @Param("tu") LocalDateTime tu,
                             @Param("den") LocalDateTime den,
                             @Param("boQuaId") Long boQuaId);

    /** Các booking có giờ giao với khoảng [tu, den). Dùng để vẽ lưới lịch một ngày. */
    @Query("""
            select b from Booking b join fetch b.sanBong
            where b.startAt < :den and b.endAt > :tu and b.status in :trangThai
            order by b.startAt
            """)
    List<Booking> timTrongKhoang(@Param("tu") LocalDateTime tu,
                                 @Param("den") LocalDateTime den,
                                 @Param("trangThai") Collection<BookingStatus> trangThai);

    @Query("""
            select b from Booking b join fetch b.sanBong join fetch b.createdBy
            where b.status = :trangThai
            order by b.startAt
            """)
    List<Booking> timTheoTrangThai(@Param("trangThai") BookingStatus trangThai);

    long countByStatus(BookingStatus status);

    long countByStartAtGreaterThanEqualAndStartAtLessThanAndStatusIn(LocalDateTime tu, LocalDateTime den,
                                                                       Collection<BookingStatus> trangThai);

    boolean existsBySanBongIdAndStatusInAndEndAtAfter(Long sanBongId, Collection<BookingStatus> trangThai,
                                                      LocalDateTime sau);

    /** Khóa dòng booking khi sửa tiền (tránh 2 nhân viên cùng thu tiền một lúc). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);
}
