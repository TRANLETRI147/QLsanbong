// Tạm tính tiền sân ngay trên form đặt sân (chỉ để xem trước, server luôn tính lại khi lưu)
(function () {
    var sanEl = document.getElementById('sanId');
    var batDauEl = document.getElementById('batDau');
    var ketThucEl = document.getElementById('ketThuc');
    var ketQuaEl = document.getElementById('tamTinh');
    if (!sanEl || !batDauEl || !ketThucEl || !ketQuaEl) {
        return;
    }

    function phut(hhmm) {
        var p = hhmm.split(':');
        return parseInt(p[0], 10) * 60 + parseInt(p[1], 10);
    }

    function dinhDang(so) {
        return String(Math.round(so)).replace(/\B(?=(\d{3})+(?!\d))/g, '.') + 'đ';
    }

    function capNhat() {
        var option = sanEl.options[sanEl.selectedIndex];
        if (!option) {
            return;
        }
        var gia = parseFloat(option.getAttribute('data-gia') || '0');
        var soPhut = phut(ketThucEl.value) - phut(batDauEl.value);
        if (soPhut <= 0) {
            ketQuaEl.textContent = 'Giờ kết thúc phải sau giờ bắt đầu';
            ketQuaEl.className = 'text-danger';
            return;
        }
        ketQuaEl.className = 'fw-bold';
        ketQuaEl.textContent = dinhDang(gia * soPhut / 60) + ' (' + (soPhut / 60) + ' giờ)';
    }

    sanEl.addEventListener('change', capNhat);
    batDauEl.addEventListener('change', capNhat);
    ketThucEl.addEventListener('change', capNhat);
    capNhat();
})();
