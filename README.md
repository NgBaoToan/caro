# CaroAI

**CaroAI** là dự án game cờ Caro viết bằng **Java 21**, hỗ trợ chơi với máy hoặc hai người trên cùng thiết bị. Dự án có hai giao diện: ứng dụng **desktop sử dụng Swing** và phiên bản **web chạy trên trình duyệt**, cùng tái sử dụng engine xử lý luật chơi và AI.

Dự án tập trung vào xây dựng AI tìm kiếm nước đi, tách biệt phần xử lý với giao diện và kiểm thử tính đúng đắn của các tình huống trong ván cờ.

## 1. Giới thiệu dự án

- **Bàn cờ:** 50 × 50 ô, có thể di chuyển vùng nhìn để chơi trên toàn bàn.
- **Chế độ chơi:** đấu với AI hoặc hai người luân phiên trên cùng thiết bị.
- **Độ khó:** Easy, Medium và Hard.
- **Luật hiện tại:** X đi trước; bên có từ năm quân liên tiếp theo hàng ngang, dọc hoặc đường chéo sẽ thắng, kể cả khi hai đầu bị chặn.
- **Nền tảng:** desktop có cài Java và trình duyệt kết nối tới máy chủ web của game.

Hai người cùng thiết bị là chế độ chơi luân phiên trên một bàn cờ; phiên bản hiện tại chưa hỗ trợ phòng đấu giữa hai thiết bị qua mạng.

### Chạy dự án để trải nghiệm

Cần **JDK 21** và **Maven** để build. Desktop cần môi trường đồ họa; người chơi bản web chỉ cần trình duyệt khi đã có máy chủ chạy game.

```sh
mvn verify
```

Chạy desktop:

```sh
java -jar target/caroai.jar
```

Chạy web:

```sh
java "-Djava.awt.headless=true" -cp target/caroai.jar caroai.web.WebServer
```

Mở [http://127.0.0.1:8088](http://127.0.0.1:8088). Đây là địa chỉ local trên máy đang chạy máy chủ. Trên Windows có thể dùng `./run-web.ps1` để tự build và mở server.

Chạy kiểm thử JavaScript bằng Node.js:

```sh
node --test src/test/web/*.test.cjs
```

Hướng dẫn triển khai web: [Render / Docker](docs/WEB_DEPLOY.md).

## 2. Công nghệ sử dụng

| Thành phần | Công nghệ | Vai trò |
|---|---|---|
| Ngôn ngữ chính | Java 21 | Xây dựng engine, AI, giao diện desktop và backend web |
| Giao diện desktop | Swing, Graphics2D | Vẽ bàn cờ, quân cờ, hiệu ứng và các thành phần điều khiển |
| Giao diện web | HTML, CSS, JavaScript, Canvas | Hiển thị bàn cờ và xử lý thao tác chuột, bàn phím, cảm ứng |
| Backend web | HTTP server của JDK | Phục vụ giao diện, kiểm tra lịch sử ván và xử lý yêu cầu nước đi/gợi ý |
| AI | Minimax, alpha-beta, iterative deepening | Tìm nước đi theo độ sâu và ngân sách thời gian |
| Tối ưu tìm kiếm | Zobrist hash, transposition table, cache đánh giá | Nhận diện và tái sử dụng thông tin thế cờ trong quá trình tìm kiếm |
| Lưu ván trên web | JSON, localStorage | Xuất/mở tệp ván cờ và tự lưu để khôi phục sau khi tải lại trang |
| Build và đóng gói | Maven | Biên dịch, chạy kiểm thử và tạo tệp JAR |
| Kiểm thử | JUnit 5, AssertJ, Node.js test runner, JaCoCo | Kiểm thử Java/JavaScript và tạo báo cáo độ bao phủ |
| Cấu hình triển khai | Dockerfile, Render Blueprint | Chuẩn bị môi trường build và chạy phiên bản web |

Engine và AI được tách khỏi giao diện. Desktop gọi trực tiếp phần xử lý Java; trình duyệt gửi cấu hình và lịch sử nước đi tới HTTP server. Máy chủ kiểm tra lại lịch sử và tính trạng thái ván, thay vì tin trực tiếp kết quả thắng/thua từ trình duyệt.

## 3. Các phần đã thực hiện

### Engine và luật chơi

- Xử lý đặt quân, đổi lượt, kiểm tra ô hợp lệ và xác định chuỗi thắng.
- Quản lý lịch sử nước đi, hoàn tác và bắt đầu ván mới.
- Dùng chung phần xử lý chính cho desktop và web.

### AI và tối ưu tìm kiếm

- Kiểm tra nước thắng ngay và nước cần chặn trước khi tìm kiếm sâu.
- Kết hợp Minimax, cắt tỉa alpha-beta và tìm kiếm sâu dần.
- Giữ kết quả của độ sâu đã hoàn tất khi hết ngân sách tìm kiếm.
- Sử dụng Zobrist hash, bảng lưu thế cờ và cache để giảm việc tính lại.
- Hỗ trợ hủy tìm kiếm khi hoàn tác, đổi ván hoặc rời ván.

Cấu hình hiện tại:

| Độ khó | Độ sâu tìm kiếm thường tối đa | Ngân sách tìm kiếm |
|---|---:|---:|
| Easy | 2 ply | 250 ms |
| Medium | 4 ply | 700 ms |
| Hard | 7 ply | 850 ms |

Độ sâu thực tế phụ thuộc thế cờ và thời gian còn lại; phần tìm kiếm đe dọa có thể mở rộng thêm. Ngân sách tìm kiếm không phải cam kết độ trễ của toàn bộ yêu cầu HTTP. Xem [báo cáo benchmark và cách chạy lại](bench/REPORT.md).

### Giao diện và trải nghiệm chơi

| Chức năng | Desktop | Web |
|---|:---:|:---:|
| Đấu AI ba độ khó | Có | Có |
| Hai người cùng thiết bị | Có | Có |
| Hoàn tác, gợi ý nước đi, ván mới | Có | Có |
| Di chuyển vùng nhìn bàn cờ | Có | Có |
| Phóng to/thu nhỏ bàn cờ | — | Có |
| Giao diện sáng/tối, bốn kiểu quân | Có | Có |
| Bật/tắt âm thanh | Có | Có |
| Thanh đánh giá lợi thế X/O | Có | Có |
| Lưu/mở ván bằng tệp JSON | — | Có |
| Tự lưu và khôi phục khi tải lại trang | — | Có |

Gợi ý chỉ đánh dấu ô nên đi, không tự đặt quân. Thanh lợi thế phản ánh đánh giá thế cờ, không phải xác suất thắng đã được hiệu chỉnh.

Bản web tự lưu ván trên trình duyệt và kiểm tra lại lịch sử qua máy chủ khi khôi phục. Nếu đến lượt AI, máy sẽ tiếp tục sau bước kiểm tra. Người chơi có thể xuất JSON để sao lưu hoặc chuyển ván sang thiết bị khác; khi trình duyệt chặn lưu trữ, giao diện sẽ nhắc lưu tệp.

### Kiểm thử và bảo trì

Kết quả kiểm thử ngày **17/11/2026**:

- **101 kiểm thử Java** và **16 kiểm thử JavaScript** đạt.
- Chơi thử desktop bằng Java Robot: **24 bước kiểm tra** và tình huống hoàn tác giữa hiệu ứng thắng đều đạt.
- Kiểm thử trình duyệt Chrome: **31 luồng**, gồm thao tác chơi, JSON, tự lưu, mất mạng và cảm ứng giả lập.
- Kiểm thử HTTP thực tế: **42 tình huống**, gồm dữ liệu sai, nhiều ván độc lập và giới hạn yêu cầu đồng thời.

Đã sửa lỗi thông báo thắng cũ xuất hiện sau khi hoàn tác trên desktop và lỗi thiếu ký tự biểu tượng nút trên Linux. Repository có `.gitignore` để loại trừ đầu ra build, log và tệp sinh tự động.

Xem phạm vi, bằng chứng và giới hạn trong [báo cáo kiểm thử](docs/TEST_REPORT-2026-11-17.md).

## 4. Các phần đang hoàn thiện và định hướng phát triển

Dự án đang ở giai đoạn hoàn thiện và kiểm chứng trước khi triển khai công khai. Những phần cần tiếp tục thực hiện gồm:

| Hạng mục | Trạng thái hiện tại | Công việc tiếp theo |
|---|---|---|
| Triển khai website công khai | Đã có Dockerfile và cấu hình Render; chưa có URL triển khai được xác nhận | Build/deploy thực tế, kiểm tra HTTPS, reverse proxy và khởi động dịch vụ |
| Khả năng phục vụ nhiều người | Đã thử các ván độc lập và lượt gửi dồn 12 yêu cầu | Đo tải kéo dài, tài nguyên và độ trễ khi nhiều người chơi |
| Tương thích trình duyệt, thiết bị | Đã thử Chrome và cảm ứng giả lập | Kiểm thử Firefox, Safari và thiết bị cảm ứng vật lý |
| Đánh giá chất lượng AI | Đã có benchmark thời gian và kiểm thử các thế cờ | Mở rộng bộ thế cờ, đánh giá sức cờ qua các ván tự đấu |

Các tính năng **dự kiến**, chưa được triển khai trong phiên bản hiện tại:

- Phòng đấu trực tuyến giữa hai thiết bị.
- Tài khoản người chơi, lịch sử ván gắn với tài khoản và bảng xếp hạng.
- Tùy chọn bộ luật khác, chẳng hạn điều kiện chặn hai đầu.


---

Tác giả: **nbaotoan**.
