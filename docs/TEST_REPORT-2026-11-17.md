# Kiểm thử game thực tế — 17/11/2026

Kiểm thử trên Linux x86_64, OpenJDK 21.0.13.0.3, Chrome 157.0.8103.78,
Node.js 22.16.0. Web chạy bằng HTTP server Java thật tại
http://127.0.0.1:8088. Desktop chạy Swing trên màn hình Xvfb riêng,
điều khiển bằng Java Robot; không dùng màn hình hoặc dữ liệu trình duyệt
đang làm việc của người dùng.

## Kết quả

| Phần | Kết quả | Phạm vi |
|---|---|---|
| Desktop/Robot | 24 bước + tình huống Undo hồi quy đều đạt | Hai người, AI, gợi ý, thắng, chơi lại, hoàn tác, giao diện/âm thanh, pan và cửa sổ tối thiểu |
| Java/JUnit | 101/101 đạt | Engine, AI, desktop, HTTP; gồm 4 kiểm thử mới cho vòng đời hiệu ứng thắng |
| JavaScript/Node | 16/16 đạt | JSON, âm thanh và tự lưu/khôi phục |
| Chrome | 31/31 đạt | Chơi thực tế qua Canvas, bàn phím, cảm ứng giả lập; các luồng lưu/khôi phục và lỗi kết nối |
| HTTP thật | 42/42 đạt | Luật, ba độ khó, dữ liệu sai, ván độc lập, giới hạn tìm kiếm đồng thời |

Desktop: đã xác nhận bằng Robot rằng Undo trong hiệu ứng thắng lùi bàn về
8 quân và không xuất hiện thông báo thắng cũ sau 2,2 giây. Xem
`target/manual-test/desktop-final/undo.log` và ảnh sau sửa.

## Web

- Đấu AI Easy/Medium/Hard với cả X và O; hai người cùng thiết bị; hoàn tác,
  ô đã có quân, gợi ý không đặt quân.
- Năm quân theo ngang, dọc và hai đường chéo; không cho đánh thêm khi đã
  thắng; hoàn tác nước thắng; tải lại ván đã kết thúc.
- Xuất JSON thật bằng download, đổi ván, mở lại bằng upload; từ chối JSON
  hỏng và nước đi sau khi đã thắng, giữ nguyên ván đang chơi.
- Tự lưu và khôi phục khi tải lại, kể cả giữa lượt AI; ván mới hủy phản hồi
  AI cũ. Trình duyệt chặn Storage vẫn chơi được và nhắc lưu JSON.
- Ngắt mạng/thử lại, ván tự lưu sai luật bị server từ chối, giao diện khi
  nhận HTTP 429; sáng/tối, bốn kiểu quân và bật/tắt âm thanh.
- Kéo bàn không đặt nhầm quân; WASD, phím mũi tên/Enter, zoom và về tâm.
  Cảm ứng giả lập 390×844; không tràn ngang tại 320/768/1024 px.

Không phát hiện lỗi gameplay web trong các luồng đã thử. Hai console error
chủ động tạo trong bộ smoke là HTTP 400 khi mở ván sai và mất mạng giả lập;
không có lỗi JavaScript chưa được xử lý.

## HTTP và yêu cầu đồng thời

Bốn ván gửi đồng thời giữ lịch sử riêng. Khi gửi 12 yêu cầu HARD cùng lúc,
4 yêu cầu nhận HTTP 200, 8 yêu cầu nhận HTTP 429; yêu cầu kế tiếp thành công.
Thời gian lớn nhất của lượt gửi dồn này là **853,072 ms**. Đây là kiểm tra
ngắn trên máy local, chưa đại diện cho tải nhiều người kéo dài.

Xác nhận luật hiện tại: từ năm quân liên tiếp trở lên thắng, kể cả bị chặn
hai đầu. Chuỗi sáu quân được công nhận; giao diện đánh dấu năm ô thắng.

## Lỗi phát hiện và thay đổi

1. **Desktop: thông báo thắng cũ xuất hiện sau Undo.** Đánh nước thắng rồi
   bấm Undo trong khoảng 1,8 giây hiệu ứng; bàn lùi về 8 quân nhưng timer cũ
   vẫn mở màn hình thắng. Sửa `BoardPanel` để giữ và hủy timer khi hoàn tác,
   reset, rời ván hoặc gỡ panel; bỏ qua callback đã xếp hàng sau khi hủy.
   Có bốn kiểm thử hồi quy cho các đường hủy này; tình huống lỗi đã qua
   kiểm tra lại bằng chuột thật do Robot điều khiển.
2. **Desktop: icon nút hiện ô vuông trên Linux.** Font vật lý Noto Sans
   thiếu ký tự; chuyển riêng phần icon trong `ClassicButton` sang font logic
   `Dialog` có fallback. Giữ font nhãn. Đã xem ảnh trước/sau ở cả sáng và tối.
3. **Hướng dẫn tự lưu chưa đồng nhất.** Sửa trợ giúp web và hướng dẫn chạy
   local để phản ánh tính năng tự lưu đã bổ sung.

## Bằng chứng và chạy lại

Các tệp sinh khi kiểm thử được giữ local, trong thư mục bị Git bỏ qua:

- `target/manual-test/maven-verify.log`, `javascript-tests.log`.
- `target/manual-test/web/REPORT.md`, `results.json`, `edge-results.json`,
  `smoke.cjs`, `edge.cjs`, ảnh desktop/mobile và tệp JSON xuất từ trình duyệt.
- `target/manual-test/api/REPORT.md`, `probe.py`, `results.json`,
  `saturation.json`, `probe.log`.
- `target/manual-test/desktop/`: mã Robot, log ban đầu, ảnh lỗi và ảnh icon.
- `target/manual-test/desktop-final/`: log và ảnh chơi lại trên bản sửa.
  Loại bỏ biến `WAYLAND_DISPLAY` và dùng Xvfb riêng khi chạy Robot, để công
  cụ không gọi portal điều khiển màn hình Wayland của phiên làm việc chính.

Kiểm thử tự động trong repository:

```sh
mvn verify
node --test src/test/web/*.test.cjs
```

Maven tạo bản chạy mới ở `target/caroai.jar`. Chạy desktop bằng
`java -jar target/caroai.jar`; chạy website bằng
`java -Djava.awt.headless=true -cp target/caroai.jar caroai.web.WebServer`.

## Giới hạn

Chưa có URL website công khai để kiểm thử bản triển khai, TLS, reverse proxy
hoặc khởi động nguội của hosting. Chưa thử Firefox/Safari, thiết bị cảm ứng
vật lý, nghe âm thanh qua loa hoặc tải kéo dài. Cảm ứng là giả lập Chrome;
âm thanh được kiểm tra tài nguyên WAV và trạng thái bật/tắt. Chưa commit/push
các thay đổi hoặc triển khai hosting.
