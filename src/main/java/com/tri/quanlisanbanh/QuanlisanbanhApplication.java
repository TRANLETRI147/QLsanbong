package com.tri.quanlisanbanh;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QuanlisanbanhApplication {

    public static void main(String[] args) {
        // Server Render chạy giờ UTC. Ép về giờ Việt Nam để giờ đặt sân không bị lệch 7 tiếng.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(QuanlisanbanhApplication.class, args);
    }
}
