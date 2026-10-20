package com.tri.quanlisanbanh.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tri.quanlisanbanh.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Tổng tiền thực thu (bỏ phiếu đã hủy). Trả về null nếu chưa có phiếu nào. */
    @Query("select sum(p.amount) from Payment p where p.voided = false and p.paidAt >= :tu and p.paidAt < :den")
    BigDecimal tongThu(@Param("tu") LocalDateTime tu, @Param("den") LocalDateTime den);

    @Query("""
            select p from Payment p
              join fetch p.booking b join fetch b.sanBong join fetch p.receivedBy
            where p.voided = false and p.paidAt >= :tu and p.paidAt < :den
            order by p.paidAt desc
            """)
    List<Payment> danhSachThu(@Param("tu") LocalDateTime tu, @Param("den") LocalDateTime den);
}
