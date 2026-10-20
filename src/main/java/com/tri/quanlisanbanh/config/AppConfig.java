package com.tri.quanlisanbanh.config;

import java.time.Clock;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    /** Đồng hồ theo giờ Việt Nam: mọi chỗ cần "bây giờ" đều dùng bean này. */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    @Bean
    public CauHinhLich cauHinhLich(@Value("${app.lich.gio-mo-cua:5}") int gioMoCua,
                                   @Value("${app.lich.gio-dong-cua:23}") int gioDongCua) {
        if (gioMoCua < 0 || gioMoCua > 23 || gioDongCua <= gioMoCua || gioDongCua > 24) {
            throw new IllegalStateException(
                    "app.lich.gio-mo-cua / app.lich.gio-dong-cua không hợp lệ: " + gioMoCua + " - " + gioDongCua);
        }
        return new CauHinhLich(gioMoCua, gioDongCua);
    }

    /**
     * Chạy Flyway. Nếu đặt biến môi trường APP_RESET_DB=true thì XÓA SẠCH mọi bảng rồi tạo lại.
     * Chỉ dùng 1 lần để bỏ các bảng cũ do Hibernate tạo trước đây. Xong phải gỡ biến này đi.
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(@Value("${app.reset-db:false}") boolean resetDb) {
        return flyway -> {
            if (resetDb) {
                log.warn("!!! app.reset-db=true : đang XÓA SẠCH toàn bộ dữ liệu trong database rồi tạo lại !!!");
                flyway.clean();
            }
            flyway.migrate();
        };
    }
}
