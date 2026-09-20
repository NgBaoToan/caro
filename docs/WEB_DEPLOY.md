# Cho mọi người chơi CaroAI bằng link

Bản web dùng AI Java hiện tại. Người chơi không cần cài Java, tải mã nguồn hay đăng nhập.
Hỗ trợ đấu máy, hai người cùng thiết bị, hoàn tác, gợi ý, lưu/mở ván, sáng/tối,
bốn kiểu X/O và tiếng đặt quân đã cắt còn 0,25 giây.
Thanh X/O trên bàn cờ hiển thị lợi thế ước tính, kèm cảnh báo nước thắng ngay và
nước buộc phải chặn. Phần trăm dựa trên điểm hình cờ và mối đe dọa trực tiếp,
không phải xác suất thắng đã hiệu chỉnh hoặc kết quả tìm kiếm sâu. Cập nhật khi
máy chủ xác nhận nước đi, hoàn tác hoặc mở ván; ẩn kết quả cũ trong lúc chờ.
Chưa có phòng đấu giữa hai thiết bị hoặc tài khoản/xếp hạng.

## Chạy thử trên máy

Trong PowerShell tại `D:\proj\caro`:

```powershell
.\run-web.ps1
```

Mở http://127.0.0.1:8088. Địa chỉ này chỉ dùng trên máy đang chạy game.
Ctrl+C để dừng. Lưu ván trước khi tải lại trang.

## Đưa lên Render

1. Đưa mã nguồn cập nhật lên repo GitHub `NgBaoToan/caro`. Bao gồm `src`, `pom.xml`,
   `Dockerfile`, `.dockerignore`, `render.yaml`; không cần đưa `target` hoặc `logs`.
2. Đăng nhập https://dashboard.render.com bằng tài khoản của bạn.
3. Chọn **New → Blueprint**, kết nối repo `NgBaoToan/caro`, chọn nhánh chứa bản web.
4. Render đọc `render.yaml`: một Web Service chạy Docker, gói Free, kiểm tra `/healthz`.
   Kiểm tra gói dịch vụ trước khi tạo; cấu hình này không yêu cầu cơ sở dữ liệu.
5. Khi dịch vụ báo **Live**, mở địa chỉ HTTPS `onrender.com` Render cấp và thử một nước đi.
   Gửi chính địa chỉ đó cho bạn bè; máy cá nhân của bạn có thể tắt.

Không có URL công khai cho đến khi triển khai thành công. Tên miền thực tế do Render cấp,
không nhất thiết là `caroai.onrender.com` vì tên có thể đã được sử dụng.

Gói Free ngủ sau 15 phút không có truy cập nên lần mở lại có thể cần chờ khởi động.
Phù hợp thử nghiệm ít người; server hiện giới hạn bốn yêu cầu tính toán đồng thời,
khi bận người chơi nhận thông báo thử lại. Chưa kiểm thử tải lớn.

Tài liệu chính thức: https://render.com/docs/your-first-deploy,
https://render.com/docs/docker, https://render.com/docs/blueprint-spec.

## Máy chủ khác / Docker

```sh
docker build -t caroai .
docker run --rm -p 8088:8088 caroai
```

Docker tự build bằng Java 21, chạy kiểm thử rồi tạo image chạy dưới user không phải root.
Máy chủ lắng nghe `HOST=0.0.0.0` và cổng `PORT` do nhà cung cấp cấp.
Desktop vẫn chạy bằng `java -jar target/caroai.jar`.
