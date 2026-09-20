# CaroAI

Game cờ Caro bằng **Java 21**, có bản **desktop (Swing)** và **web**. Chơi với AI ba mức Easy / Medium / Hard hoặc hai người trên cùng thiết bị. Bàn cờ 50 × 50; X đi trước, nối từ năm quân liên tiếp để thắng.

## Cập nhật ngày 20/09/2026

- Làm mới giao diện theo phong cách Chess.com: sáng/tối, bốn kiểu quân **Bo tròn, Góc cạnh, Neon, Huy hiệu**.
- Thêm bóng đổ, viền sáng cho nút và hiệu ứng chìm khi rê chuột vào lựa chọn.
- Thêm tiếng đặt quân cho người và AI; cắt bỏ khoảng lặng, giữ một tiếng gõ **0,25 giây**, có bật/tắt âm thanh.
- Thêm **thanh lợi thế X/O** trên desktop và web, cho biết bên đang bất lợi. Phần trăm là đánh giá thế cờ, không phải xác suất thắng chính xác.
- Thêm **Gợi ý nước đi** trên desktop: khoanh vàng ô nên đi, hiện cột/hàng, không tự đánh. Gợi ý chạy nền và được hủy khi đánh nước mới, hoàn tác hoặc đổi ván.
- Khôi phục và cập nhật bản web: đấu AI, hai người cùng thiết bị, hoàn tác, gợi ý, lưu/mở ván JSON, tùy chỉnh giao diện và âm thanh.
- Chuẩn bị **Dockerfile và cấu hình Render** để triển khai thành link chơi công khai.

## Chạy bản desktop

Máy phát triển cần **JDK 21** và **Maven 3.8+**. Chạy trong thư mục dự án:

```powershell
mvn package
java -jar target/caroai.jar
```

Chọn chế độ, quân X/O và độ khó rồi bấm **START**. Dùng **WASD** để di chuyển bàn cờ. Cột trái có **Undo**, **Gợi ý nước đi**, **New Game**, **Appearance** và **Sound: ON/OFF**.

Sau khi cập nhật mã, build lại và đóng/mở lại game để dùng bản mới.

## Chạy bản web

```powershell
.\run-web.ps1
```

Mở [http://127.0.0.1:8088](http://127.0.0.1:8088). Bấm/chạm để đặt quân, kéo để di chuyển bàn cờ, cuộn hoặc dùng nút +/− để zoom. Lưu ván trước khi tải lại trang.

Địa chỉ trên chỉ dùng tại máy đang chạy server. Muốn bạn bè mở link và chơi ngay, triển khai theo [hướng dẫn Render / Docker](docs/WEB_DEPLOY.md). Người chơi bản web không cần cài Java.

**Hiện chưa triển khai link công khai**, chưa có phòng đấu giữa hai thiết bị, tài khoản hoặc bảng xếp hạng. Lưu/mở ván hiện có trên bản web.

## Kiểm thử

```powershell
mvn verify
node --test src/test/web/*.test.cjs
```

Kết quả gần nhất: **97 kiểm thử Java** và **10 kiểm thử JavaScript** đạt. Node.js chỉ cần để chạy kiểm thử JavaScript. Chưa kiểm thử tải lớn hoặc xác nhận triển khai Docker/Render thực tế.

## Công nghệ

- AI: Minimax, alpha-beta, tìm kiếm sâu dần, Zobrist hash và bảng lưu thế cờ; chạy theo ngân sách thời gian.
- Desktop: Swing / Graphics2D. Web: HTML, CSS, JavaScript và HTTP server Java dùng chung AI.
- Âm thanh: trích đoạn 1,19–1,44 giây từ file người dùng cung cấp `freesound_community-chess-pieces-60890.mp3`, đóng gói thành WAV.

Tác giả: **nbaotoan**.
