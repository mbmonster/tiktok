# TikTok TV cho Android TV (Mi Box Gen 2 / Box S / Google TV)

Ứng dụng TikTok TV được thiết kế và tối ưu riêng cho các thiết bị Android TV / Google TV (đặc biệt là Xiaomi Mi Box Gen 2 / Box S).

- **Tối ưu siêu mượt (TV Cinema Mode)**: Khử toàn bộ hiệu ứng CSS blur filter nặng nề, kích hoạt tăng tốc phần cứng GPU, phát video 60fps mượt mà không còn giật lag.
- **Âm thanh trong trẻo, đồng bộ chuẩn (Exclusive Audio Watchdog)**: Triệt tiêu hoàn toàn tiếng rè (buffer underrun) và không bị lệch tiếng so với hình; dập tắt các video phát ngầm.
- **Tích hợp Chuột ảo (Virtual Mouse)**: Hỗ trợ điều khiển chuột bằng remote để bấm bất kỳ nút nào, đóng popup hoặc chọn đăng nhập dễ dàng.
- **Hỗ trợ Đăng nhập bằng Mã QR trong 5 giây**: Tự động chuyển tab và phóng to mã QR trên màn hình TV; quét bằng điện thoại là xong, lưu đăng nhập vĩnh viễn không lo lặp lại video.
- **Tự động build APK qua GitHub Actions**: Không cần cài Java hay Android Studio trên máy tính.

---

## 🎮 Cách sử dụng Remote Mi Box / Android TV

| Nút trên Remote | Khi ở Chế độ TV (Mặc định) | Khi bật Chuột ảo (Mouse Mode) |
| :--- | :--- | :--- |
| **Mũi tên Xuống** | Chuyển sang video kế tiếp (mượt mà, chống spam) | Di chuyển con trỏ chuột xuống |
| **Mũi tên Lên** | Quay lại video trước đó | Di chuyển con trỏ chuột lên |
| **Mũi tên Trái** | Tua lùi 5 giây | Di chuyển con trỏ chuột sang trái |
| **Mũi tên Phải** | Tua tới 5 giây | Di chuyển con trỏ chuột sang phải |
| **Nút Tròn (OK / Enter)** | Nhấn nhanh: **Play / Pause** video | **Click chuột** vào vị trí con trỏ đang trỏ |
| **Giữ nút OK (1 giây)** | **BẬT / TẮT Chuột ảo** (dùng được trên mọi loại remote) | **TẮT Chuột ảo** |
| **Nút Menu (hoặc phím 0 / Info)** | **Bật / Tắt nhanh Chuột ảo** | **Bật / Tắt nhanh Chuột ảo** |
| **Nút Back** | Đóng popup thông báo / Lùi trang / Thoát app (nhấn 2 lần) | Tắt chuột ảo / Quay về chế độ TV |

---

## 📱 Hướng dẫn Đăng nhập bằng Mã QR (Khắc phục video lặp lại)

Khi xem ở chế độ khách (chưa đăng nhập), TikTok chỉ phát một số video xu hướng cố định và sau đó sẽ khóa cuộn bắt đăng nhập. Để xem video vô tận được cá nhân hóa theo sở thích của bạn:

1. Khi hộp thoại đăng nhập hiện ra trên TV (hoặc bạn dùng chuột bấm vào **Đăng nhập** ở góc trên):
2. Màn hình TV sẽ **tự động hiển thị Mã QR phóng to** ở giữa màn hình.
3. Trên điện thoại: Mở app **TikTok** $\rightarrow$ Vào mục **Hồ sơ (Profile)** $\rightarrow$ Bấm vào biểu tượng **Mã QR** (hoặc Menu 3 gạch $\rightarrow$ Mã QR của tôi $\rightarrow$ Biểu tượng Quét ở góc trên).
4. Đưa camera điện thoại quét mã QR đang hiện trên TV $\rightarrow$ Bấm **Xác nhận đăng nhập**.
5. App TV sẽ lập tức đăng nhập thành công và lưu phiên vĩnh viễn:
   - Toàn bộ feed **"Dành cho bạn" (For You)** sẽ được tải theo đúng sở thích của bạn.
   - Không bao giờ bị lặp lại video hay bị chặn bắt đăng nhập nữa!
   - Nếu lúc xem xuất hiện bảng popup đăng nhập mà bạn chưa muốn đăng nhập, chỉ cần bấm nút **Back** trên remote là popup sẽ tự đóng lại.

---

## 🚀 Hướng dẫn lấy file APK qua GitHub (Không cần cài Java/Android Studio)

### Tải file APK về
1. Sau khi `git push`, bạn vào trang repository của bạn trên GitHub, bấm vào tab **Actions**.
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
