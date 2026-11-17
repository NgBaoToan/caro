# Phát hành CaroAI có link chơi trực tuyến

GitHub Release cung cấp ghi chú phiên bản và tệp tải xuống. Link **Chơi ngay**
phải trỏ tới một máy chủ đã triển khai backend Java; tải HTML lên GitHub Pages
không chạy được AI hoặc API hiện tại.

## 1. Triển khai bản web

Dự án đã có `Dockerfile`, `render.yaml` và endpoint kiểm tra `/healthz`.

- Có thể kết nối tài khoản Render để triển khai từ repository `NgBaoToan/caro`.
- Hoặc mở [Deploy to Render](https://render.com/deploy?repo=https://github.com/NgBaoToan/caro),
  đăng nhập, kiểm tra cấu hình và tạo dịch vụ từ Blueprint.
- Khi dịch vụ báo Live, lấy URL HTTPS Render cấp. Không dùng địa chỉ
  `127.0.0.1`, URL trang cấu hình hoặc URL mẫu làm link chơi công khai.
- Kiểm tra mở được bàn cờ, đánh được một nước có AI đáp lại và tải lại trang
  vẫn giữ ván. Xem [hướng dẫn triển khai](WEB_DEPLOY.md).

Một số gói hosting có thời gian ngủ khi không có truy cập. Kiểm tra điều kiện
và chi phí hiện tại của tài khoản trước khi chọn gói.

## 2. Tạo GitHub Release

Sau khi có URL hoạt động, mở workflow **Release CaroAI** trong tab Actions của
repository, chạy trên nhánh `main` với:

- `tag`: ví dụ `v1.0.0`.
- `play_url`: URL HTTPS của game vừa triển khai.

Workflow kiểm tra URL và API trước khi phát hành, chạy kiểm thử Java và
JavaScript, build JAR và gói ZIP, sau đó tạo release kèm link **Chơi ngay**
ở đầu ghi chú. Release đã tồn tại sẽ không bị ghi đè tự động.

Bản JAR/ZIP tải xuống cần Java 21. Người chơi qua link web chỉ cần trình duyệt.
Các tệp SHA-256 đi kèm dùng để kiểm tra tính toàn vẹn của bản tải xuống.

**Chỉ khi workflow thành công mới có bản phát hành tương ứng.** Workflow và
cấu hình hosting không tự tạo ra URL chơi khi chưa triển khai dịch vụ.
