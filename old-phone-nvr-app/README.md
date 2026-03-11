# Old Phone NVR Cloud (Android)

APK Android biến điện thoại cũ thành camera NVR mini + upload Google Drive.

## Tính năng đã có
- Foreground service chạy nền (`NvrService`)
- CameraX quay backend camera thành file segment `mp4` (mỗi 60 giây)
- Google Sign-In để lấy tài khoản Drive
- Worker upload tự động lên Google Drive (chu kỳ 15 phút)
- Xóa file local sau khi upload thành công

## Build APK
1. Mở thư mục `old-phone-nvr-app` bằng Android Studio
2. Chờ Gradle sync xong
3. Run trên máy Android hoặc Build APK:
   - `Build > Build Bundle(s) / APK(s) > Build APK(s)`

## Cách dùng nhanh
1. Mở app, bấm **Connect Google Drive**
2. Bấm **Start NVR**
3. Đặt máy cắm sạc + Wi-Fi ổn định
4. App tự quay segment và tự upload cloud theo lịch

## Lưu ý quan trọng
- Android phải cho phép Camera, Micro, Notification
- Nên tắt Battery Optimization cho app
- Nếu Drive chưa upload: kiểm tra đã sign-in thành công chưa

## Đường dẫn file local
`Android/data/com.longpc.nvrcloud/files/Movies/segments/`
