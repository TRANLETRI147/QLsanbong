package com.tri.quanlisanbanh.config;

/** Giờ mở cửa và đóng cửa của sân (đọc từ app.lich.* trong application.properties). */
public record CauHinhLich(int gioMoCua, int gioDongCua) {
}
