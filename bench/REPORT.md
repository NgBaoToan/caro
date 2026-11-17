# Benchmark cấu hình hiện tại — 17/11/2026

Đo mã nguồn hiện tại trên Linux x86_64, OpenJDK 21.0.13.0.3,
heap `-Xms256m -Xmx512m`, bàn 50×50. HARD dùng **850 ms**, tối đa 7 ply,
giới hạn nhánh 16; không sửa cấu hình bằng reflection. EASY: 250 ms / 2 ply /
8 nhánh; MEDIUM: 700 ms / 4 ply / 12 nhánh.

Mỗi mức/thế cờ làm nóng 2 lượt, sau đó đo 5 lượt trong cùng JVM.
Thời gian bao gồm `findBestMove`, không gồm khởi động JVM, HTTP hoặc thời gian
chờ hiển thị. Độ sâu chỉ tính vòng tìm kiếm thường đã hoàn tất.
Benchmark và latency probe chạy tuần tự, trước khi chạy kiểm thử Java.

| Mức | Thế cờ | Trung vị ms | Lớn nhất ms | Độ sâu hoàn tất |
|---|---|---:|---:|---|
| EASY | Mở (2 quân) | 2.646 | 5.001 | 2 |
| EASY | Giữa ván (8 quân) | 2.833 | 3.271 | 2 |
| EASY | Dày (16 quân) | 3.227 | 3.546 | 2 |
| EASY | Nhiều đe dọa (10 quân) | 3.545 | 3.683 | 2 |
| MEDIUM | Mở (2 quân) | 10.972 | 16.315 | 4 |
| MEDIUM | Giữa ván (8 quân) | 14.483 | 21.076 | 4 |
| MEDIUM | Dày (16 quân) | 15.013 | 17.611 | 4 |
| MEDIUM | Nhiều đe dọa (10 quân) | 31.231 | 33.617 | 4 |
| HARD | Mở (2 quân) | 850.038 | 850.068 | 6 |
| HARD | Giữa ván (8 quân) | 136.594 | 138.964 | 6 |
| HARD | Dày (16 quân) | 377.813 | 382.926 | 7 |
| HARD | Nhiều đe dọa (10 quân) | 850.055 | 850.071 | 6 |

HARD chạm ngân sách ở thế mở và nhiều đe dọa; thế giữa ván dừng ở độ sâu 6,
thế dày đạt 7. Deadline hợp tác có thể vượt nhẹ ngân sách; đây không phải
cam kết độ trễ trên mọi phần cứng hoặc khi có GC/tải hệ thống lớn.

Latency probe trong JVM riêng: 1 lượt mở lạnh + 12 thế ngẫu nhiên với seed
55197, 12–56 quân. **13/13 trả nước hợp lệ**, lớn nhất **850.063 ms**.
Ngưỡng kiểm tra của probe vẫn là 1.500 ms, tách biệt với ngân sách tìm kiếm.

Dữ liệu: [phép đo chính](results/current-850ms.csv),
[latency](results/current-850ms-latency.csv),
[môi trường và cấu hình](results/current-environment.txt).
Đây là phép đo thời gian, không đo tải nhiều người hoặc sức cờ qua tự đấu.

## Tái lập

Với JDK 21 và Bash, chạy độc lập với các kiểm thử hoặc profiler:

```bash
bash bench/run-current.sh 5
```

Script biên dịch vào `bench/build/current`, ghi dữ liệu và môi trường vào
`bench/results/current-*`. Dùng trực tiếp `Difficulty` trong mã nguồn.
Tên CSV `850ms` mô tả cấu hình của lần cập nhật này; nếu đổi ngân sách, hãy
đổi tên kết quả và cập nhật báo cáo tương ứng.

## Số liệu lịch sử

[Báo cáo ngày 17/09/2026](REPORT-2026-09-17.md) và các CSV cũ được giữ để
tham khảo. Báo cáo đó đo HARD với 1.350 ms trên Windows, gồm so sánh baseline
và JFR. Không dùng số liệu cũ để mô tả cấu hình phát hành hiện tại và không
suy ra mức tăng tốc giữa hai máy/hệ điều hành khác nhau.
