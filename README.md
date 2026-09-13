# TikTok TV cho Android TV (Mi Box Gen 2)

Ứng dụng TikTok TV được thiết kế riêng cho các thiết bị Android TV / Google TV (đặc biệt là Xiaomi Mi Box Gen 2 / Box S). 

- **Giao diện Web mượt mà**: Không bị khóa vùng (Geo-block) tại Việt Nam như app chính chủ.
- **Tối ưu Remote D-Pad**: Lướt video, tạm dừng, tua bằng điều khiển từ xa cực nhạy.
- **Không cần cài môi trường**: Dự án đã tích hợp sẵn GitHub Actions để tự động build file `.apk` trên máy chủ đám mây.

---

## 🎮 Cách sử dụng Remote Mi Box

| Nút trên Remote | Tác vụ trong ứng dụng |
| :--- | :--- |
| **Mũi tên Xuống** | Chuyển sang video kế tiếp |
| **Mũi tên Lên** | Quay lại video trước đó |
| **Nút Tròn (OK / Enter)** | Tạm dừng (Pause) / Tiếp tục phát (Play) |
| **Mũi tên Trái** | Tua lùi 5 giây |
| **Mũi tên Phải** | Tua tới 5 giây |
| **Nút Menu** | Bật / Tắt tiếng (Mute / Unmute) |
| **Nút Back** | Quay lại trang trước (Nhấn 2 lần để thoát ứng dụng) |

---

## 🚀 Hướng dẫn lấy file APK qua GitHub (Không cần cài Java/Android Studio)

Bạn chỉ cần thực hiện 3 bước đơn giản sau:

### Bước 1: Tạo Repository trên GitHub
1. Truy cập [github.com](https://github.com) và đăng nhập (nếu chưa có tài khoản, đăng ký miễn phí mất 1 phút).
2. Tạo một repository mới (ví dụ đặt tên là `tiktok-tv`, chọn chế độ **Public** hoặc **Private** đều được).

### Bước 2: Đẩy mã nguồn lên GitHub
Mở cửa sổ dòng lệnh (Terminal / PowerShell) ngay tại thư mục này (`d:\Tiktok`) và chạy các lệnh:

```bash
git init
git add .
git commit -m "Khoi tao TikTok TV cho Mi Box"
git branch -M main
git remote add origin https://github.com/TÊN_TÀI_KHOẢN_CỦA_BẠN/tiktok-tv.git
git push -u origin main
```
*(Thay `TÊN_TÀI_KHOẢN_CỦA_BẠN` bằng username GitHub của bạn).*

### Bước 3: Tải file APK về
1. Sau khi `git push`, bạn vào lại trang repo của bạn trên GitHub, bấm vào tab **Actions**.
2. Bạn sẽ thấy tiến trình **"Build TikTok TV APK"** đang tự động chạy.
3. Chờ khoảng 1 - 2 phút cho đến khi hiện dấu tích xanh ✅ hoàn thành.
4. Bấm vào tên tiến trình đó, kéo xuống mục **Artifacts** ở dưới cùng:
   - Bấm tải **`TikTok-TV-APK`** về máy tính.
   - Giải nén file `.zip` vừa tải về, bạn sẽ nhận được file **`TikTok-TV.apk`** sẵn sàng cài đặt!

---

## 📺 Cài đặt lên Mi Box Gen 2

1. Chép file `TikTok-TV.apk` vào một chiếc USB.
2. Cắm USB vào cổng USB phía sau Mi Box Gen 2.
3. Trên Mi Box, mở Google Play Store tải một app quản lý file miễn phí (ví dụ: **AnExplorer** hoặc **File Commander**).
4. Mở app quản lý file $\rightarrow$ Chọn ổ USB $\rightarrow$ Bấm vào file `TikTok-TV.apk` $\rightarrow$ Chọn **Cài đặt (Install)**.
5. Cài xong, biểu tượng **TikTok TV** sẽ xuất hiện ngay trên màn hình chính Google TV của Mi Box!
