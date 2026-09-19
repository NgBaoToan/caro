# CaroAI

**Ngôn ngữ:** [English](README.md) · **Tiếng Việt**

Trò chơi cờ Caro trên máy tính, viết bằng Java 21 và Swing. Bạn có thể chơi hai người trên cùng máy hoặc đấu với AI ở ba mức độ khó. Giao diện được vẽ riêng và có chế độ Sáng/Tối.

## Tổng quan

| Phần | Đã triển khai |
|---|---|
| Luật chơi | Bàn cờ 50 × 50, thắng khi có năm quân liên tiếp, chơi hai người, hoàn tác, chơi lại và tô sáng đường thắng |
| AI | Minimax với cắt tỉa alpha-beta, tìm kiếm sâu dần, kiểm tra thắng/chặn ngay và tìm kiếm đe dọa có giới hạn |
| Tốc độ tìm kiếm | Sắp xếp nước đi, chọn ô gần quân cờ, cập nhật điểm gia tăng và bảng lưu thế cờ bằng Zobrist hash |
| Giao diện | Thành phần Swing/Graphics2D vẽ riêng, di chuyển khung nhìn, xem trước nước đi, màn hình kết quả và lưu theme |
| Kiểm thử | JUnit 5, AssertJ, cấu hình JaCoCo và công cụ benchmark có thể chạy lại |

Mức AI mặc định là **Medium**. **X luôn đi trước**; nếu bạn chọn O thì máy sẽ mở đầu.

## Yêu cầu

- JDK **21** trở lên (kiểm tra bằng `java -version` và `javac -version`)
- Maven **3.8+** để build và chạy bộ kiểm thử
- Môi trường desktop để hiển thị cửa sổ Swing

Lần build Maven đầu tiên cần tải thư viện kiểm thử. Khi chơi, ứng dụng không cần mạng.

## Chạy dự án

Mở terminal tại thư mục gốc của dự án:

```bash
mvn clean package
java -jar target/caroai.jar
```

Lệnh đầu sẽ biên dịch mã mới nhất, chạy kiểm thử và tạo JAR. Sau khi sửa mã, hãy build lại để tránh chạy nhầm JAR cũ.

Nếu chưa cài Maven, chạy trực tiếp bằng JDK:

```bash
mkdir -p out
javac -d out src/main/java/caroai/*.java
java -cp out caroai.Main
```

Trên Windows PowerShell:

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -d out (Get-ChildItem src/main/java/caroai -Filter *.java | ForEach-Object FullName)
java -cp out caroai.Main
```

Cách dùng `javac` chạy được game nhưng không chạy bộ kiểm thử JUnit.

## Cách chơi

1. Chọn **Play vs Computer** hoặc **Two Players**.
2. Nếu đấu với máy, chọn X/O và mức Easy, Medium hoặc Hard.
3. Nhấn **START**, sau đó bấm vào ô trống để đặt quân.

| Thao tác | Điều khiển |
|---|---|
| Di chuyển khung nhìn | W, A, S, D sau khi bấm vào bàn cờ |
| Hoàn tác | **Undo** ở thanh bên trái |
| Ván mới | **New Game** ở thanh bên trái |
| Về màn hình chính | **Main Menu** ở thanh bên trái |
| Đổi giao diện | **Theme: DARK/LIGHT** ở màn hình chính hoặc thanh bên trái |
| Chơi lại khi có kết quả | **PLAY AGAIN** hoặc R |
| Về menu khi có kết quả | **Main Menu** hoặc Esc |

Bộ chọn giao diện hiển thị hai thẻ xem trước Light/Dark. Đổi theme không làm mất ván đang chơi; lựa chọn được lưu bằng Java Preferences cho lần mở sau.

## AI và thời gian phản hồi

Ba mức độ dùng cùng thuật toán; khác nhau ở độ sâu, số nước ứng viên và ngân sách thời gian.

| Mức | Độ sâu tối đa | Giới hạn ứng viên | Ngân sách tìm kiếm | Thời gian phản hồi tối thiểu |
|---|---:|---:|---:|---:|
| Easy | 2 | 8 | 250 ms | 70 ms |
| Medium | 4 | 12 | 700 ms | 40 ms |
| Hard | 7 | 16 | 850 ms | Không có |

Ngân sách là mốc dừng tìm kiếm, không có nghĩa mọi nước đi đều tốn chừng ấy thời gian hoặc luôn đạt độ sâu tối đa. Thời gian phản hồi tối thiểu là khoảng chờ ngắn của giao diện khi AI tìm xong quá nhanh. Khi hết giờ, Hard dùng kết quả ở **độ sâu đã hoàn tất** gần nhất. AI chạy trong `SwingWorker` để cửa sổ không bị treo.

Trước khi tìm kiếm, AI kiểm tra nước thắng ngay và nước bắt buộc phải chặn. Sau đó, AI xét các ô gần quân đã đặt, xếp thứ tự theo điểm mẫu hình và tìm bằng alpha-beta. Zobrist hash cùng bảng thế cờ giúp tái sử dụng kết quả. Phần tìm kiếm đe dọa có giới hạn, chưa giải được mọi tổ hợp open-three.

## Cấu trúc dự án

| Đường dẫn | Vai trò |
|---|---|
| `src/main/java/caroai/GameEngine.java` | Trạng thái bàn cờ, nước đi hợp lệ, phát hiện thắng |
| `src/main/java/caroai/AI.java` | Tìm kiếm sâu dần và alpha-beta |
| `src/main/java/caroai/SearchState.java` | Trạng thái tìm kiếm và cache ứng viên |
| `src/main/java/caroai/Theme.java` | Bảng màu Sáng/Tối, font và cách vẽ quân |
| `src/main/java/caroai/GameFrame.java` | Chuyển màn hình và vòng đời ván chơi |
| `src/test/java/caroai/` | Kiểm thử luật chơi, AI và hồi quy tìm kiếm |
| `bench/` | Chương trình benchmark và kết quả đo trước đây |

`GameEngine` giữ trạng thái bàn cờ duy nhất. Giao diện đọc trực tiếp trạng thái đó; AI nhận bản chụp trước khi tìm kiếm ở luồng nền. Hoàn tác, chơi lại hoặc về menu sẽ hủy lượt tìm kiếm đang chờ.

## Kiểm thử và benchmark

```bash
mvn test
mvn clean package
```

Bộ kiểm thử bao gồm luật chơi, hoàn tác, nước chiến thuật và tính nhất quán của trạng thái tìm kiếm. Maven đã cấu hình tạo báo cáo JaCoCo trong pha test. README không công bố tỷ lệ coverage khi chưa có báo cáo hiện hành.

[Báo cáo benchmark](bench/REPORT.md) ghi lại quá trình tối ưu trước đây cùng môi trường đo, thế cờ và giới hạn kết luận. **Số liệu đó dùng ngân sách Hard 1.350 ms**; game hiện tại dùng 850 ms. Vì vậy, không dùng báo cáo cũ để khẳng định độ trễ hoặc sức cờ hiện tại.

## Giới hạn hiện tại

- Game chạy trên máy cá nhân; chưa có chơi qua mạng hoặc lưu/tải ván.
- Tìm kiếm đe dọa có giới hạn, chưa phải bộ giải cờ Caro hoàn chỉnh.
- Deadline tìm kiếm có thể bị vượt khi GC kéo dài hoặc hệ điều hành tạm dừng.
- Giao diện trên các hệ điều hành và gói phát hành hiện tại cần được kiểm tra thủ công rộng hơn.

## Tác giả

Dự án do **nbaotoan** thực hiện. Hoan nghênh đóng góp; hãy chạy `mvn clean package` trước khi gửi thay đổi.
