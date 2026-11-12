# Quét Tài Liệu (DocScanner)

App Android đơn giản để quét tài liệu thành PDF.

## Tính năng
- Chụp tài liệu, **tự động nhận diện cạnh và căn chỉnh lề** (nắn phẳng, cắt viền)
- Chụp **nhiều trang** liên tiếp, sắp xếp / xoay / chụp lại từng trang, gộp thành **1 file PDF**
- Có thể nhập thêm ảnh từ thư viện
- Danh sách các file PDF đã quét, mở xem, xóa
- **Chia sẻ**: Zalo, Messenger, SMS/tin nhắn, hoặc bất kỳ ứng dụng nào khác

> Phần chụp + căn chỉnh dùng ML Kit Document Scanner của Google, nên cần điện thoại Android 7.0+ có Google Play Services.
> App không cần xin quyền camera hay quyền bộ nhớ.

## Cách lấy file APK

### Cách 1: GitHub Actions (không cần cài gì)
1. Tạo repository mới trên GitHub, upload toàn bộ thư mục này lên (nhánh `main`).
2. Vào tab **Actions** → workflow **Build APK** sẽ tự chạy (hoặc bấm *Run workflow*).
3. Chạy xong, tải artifact **DocScanner-apk** → giải nén ra `app-debug.apk`.
4. Chép sang điện thoại và cài (cho phép "Cài đặt từ nguồn không xác định" nếu được hỏi).

### Cách 2: Android Studio
1. Mở thư mục này bằng Android Studio (bản mới), chờ Gradle sync.
2. Menu **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
3. File ở `app/build/outputs/apk/debug/app-debug.apk`.

### Cách 3: Dòng lệnh (cần JDK 17 + Android SDK)
```
./gradlew assembleDebug
```
