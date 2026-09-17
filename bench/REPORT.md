# Kết quả tối ưu AI Caro

Đo ngày 17/09/2026 trên Windows, Oracle JDK 21.0.12, JVM 64-bit, heap
`-Xms256m -Xmx512m`; CPU nhận diện Intel64 Family 6 Model 186 Stepping 2.
Không dùng số liệu Linux/apt trong bản bàn giao vì không có mã bản đó để kiểm chứng.
Baseline là AI.java và Difficulty.java thực tế tìm thấy trong dự án trước,
được lưu nguyên văn trong `bench/baseline/` để tái lập.

## Thay đổi

- Zobrist 64-bit theo ô/màu và lượt đi; bảng 131.072 ô kiểm tra toàn bộ khóa,
  lưu depth, EXACT/LOWER/UPPER, nước tốt nhất và chuẩn hóa khoảng cách đến thắng.
  Bảng tạo mới mỗi lần tìm kiếm để không lẫn góc nhìn hoặc cấu hình bàn cờ.
- Điểm hai bên cập nhật từ các đầu chuỗi có thể bị ảnh hưởng quanh nước vừa đi;
  chỉ quét toàn bàn khi khởi tạo. Undo khôi phục chính xác điểm và hash.
- Duy trì tập ô lân cận, cache điểm xếp nước và số cửa sổ có thể tạo đe dọa.
  Cache nước chỉ vô hiệu hóa trên bốn đường đi qua ô thay đổi.
- Tìm kiếm đe dọa tại chân cây giới hạn 6 ply: thắng ngay, chặn thắng ngay hoặc
  tạo bốn. Không mở rộng chỉ vì có bộ ba. Các nước bắt buộc không bị cắt theo
  giới hạn nhánh. Đây là threat quiescence có giới hạn, không phải bộ chứng minh
  toàn bộ không gian đe dọa; chưa giải đầy đủ các tổ hợp open-three.
- HARD tăng độ sâu tối đa 6 → 7, ngân sách 2000 → 1350 ms. Kiểm tra thời gian
  ở từng nút và lúc duyệt ứng viên, hoàn tác bằng finally, chỉ nhận kết quả của
  vòng iterative deepening hoàn tất. EASY/MEDIUM vẫn 2/4 ply, nhánh 8/12/16 giữ nguyên.

## Đo so sánh cùng giới hạn độ sâu

`PairedBenchmark.java` dùng hai class loader tách biệt trong cùng JVM; làm nóng
3 lượt mỗi cấu hình, đo 5 lượt, đổi thứ tự chạy gốc/mới qua từng cặp. Bốn thế cố
định 50×50 được định nghĩa trong `Benchmark.java`. Cột giảm dương là nhanh hơn.
Thời gian đo bao gồm toàn bộ findBestMove; không gồm khởi động JVM hay độ trễ
hiển thị giả lập của giao diện. HARD cả hai giới hạn 6 ply; bản mới còn tìm
thêm tối đa 6 ply đe dọa. Vì thuật toán khác nhau, đây không phải cùng cây tìm kiếm.

| Mức | Thế cờ | Gốc, trung vị ms | Mới, trung vị ms | Giảm thời gian |
|---|---|---:|---:|---:|
| EASY | Mở (2 quân) | 0.967 | 2.293 | -137.1% |
| EASY | Giữa ván (8 quân) | 1.454 | 1.833 | -26.1% |
| EASY | Dày (16 quân) | 1.565 | 3.370 | -115.3% |
| EASY | Nhiều đe dọa (10 quân) | 1.240 | 2.524 | -103.5% |
| MEDIUM | Mở (2 quân) | 11.477 | 11.762 | -2.5% |
| MEDIUM | Giữa ván (8 quân) | 11.896 | 12.392 | -4.2% |
| MEDIUM | Dày (16 quân) | 16.176 | 12.380 | 23.5% |
| MEDIUM | Nhiều đe dọa (10 quân) | 19.331 | 27.733 | -43.5% |
| HARD | Mở (2 quân) | 1015.396 | 1080.383 | -6.4% |
| HARD | Giữa ván (8 quân) | 862.979 | 318.529 | 63.1% |
| HARD | Dày (16 quân) | 612.583 | 328.518 | 46.4% |
| HARD | Nhiều đe dọa (10 quân) | 2037.631 | 1004.458 | 50.7% |

HARD cải thiện rõ ở ba thế có nhiều quân, nhưng thế mở chậm hơn khoảng 6,4%.
EASY chậm thêm khoảng 0,4–1,8 ms; MEDIUM nhiều đe dọa chậm thêm khoảng 8,4 ms.
Không tuyên bố mọi thế đều nhanh hơn. Chi phí duy trì cache và tìm đe dọa không
luôn có lợi ở cây nông. Các mức này vẫn thấp hơn nhiều ngân sách của chúng.

Baseline nhiều đe dọa chạm giới hạn 2 giây trong phép đo xen kẽ và không ghi
completedDepth; không khẳng định nó đã hoàn tất depth 6. Bản mới hoàn tất depth 6
ở cả 5 lần đo mỗi thế trong phép đo này. Giá trị -1 trong CSV là không có chỉ số,
không phải depth âm.

Số nút thường của HARD ở bốn thế lần lượt là 27.514, 3.876, 2.842, 9.887;
số lần gọi nút đe dọa tương ứng 22.344, 7.824, 10.480, 26.121.
TT hit tương ứng 8.514, 1.865, 871, 4.025. Hit không đồng nghĩa với bỏ được nút:
`ttCutoffs` lưu riêng. Không so sánh riêng `nodes` để tuyên bố giảm tổng công việc,
vì `threatNodes` là phần tìm kiếm mới và lần vào chân cây có mặt ở cả hai bộ đếm.

## Bản phát hành: HARD tối đa 7 ply

Đo bằng JVM độc lập, 2 lượt làm nóng + 5 lượt đo mỗi thế. Độ sâu dưới đây là
độ sâu thường đã hoàn tất, không cộng phần đe dọa.

| Thế cờ | Trung vị ms | Lớn nhất ms | Độ sâu hoàn tất |
|---|---:|---:|---|
| Mở (2 quân) | 1350.080 | 1350.199 | 5, 6 |
| Giữa ván (8 quân) | 441.536 | 470.332 | 6 |
| Dày (16 quân) | 1037.914 | 1082.700 | 7 |
| Nhiều đe dọa (10 quân) | 1350.107 | 1350.561 | 6 |

JVM chưa làm nóng + 12 thế ngẫu nhiên có seed, 12–56 quân: 13/13 trả nước hợp lệ; thời gian lớn nhất 1350.178 ms. Xem [latency.csv](results/latency.csv).

Giữa ván dừng ở 6 vì đã tìm thấy thắng; thế dày đạt 7. Các thế khác có thể hết
ngân sách trước khi đạt 7. Mục tiêu dưới 1,5 giây đạt trong các phép đo này;
deadline Java hợp tác không bảo đảm tuyệt đối trước GC dài, OS tạm dừng hoặc
phần cứng khác. Khâu kiểm tra chiến thuật/khởi tạo trước vòng tìm kiếm cũng
không bị ngắt giữa chừng.

Máy có dao động tốc độ rõ: các lượt độc lập trước đó có thể nhanh hơn gần 3 lần.
Giữ cả [baseline.csv](results/baseline.csv), [depth6.csv](results/depth6.csv),
[final.csv](results/final.csv) và [paired.csv](results/paired.csv), không chọn
riêng lượt nhanh nhất. Các số này là phép đo thực nghiệm, không phải kiểm định
thống kê hoặc bằng chứng tăng sức cờ qua giải đấu tự chơi.

## Hồ sơ chuyên dụng và kiểm thử

Java Flight Recorder chạy riêng `settings=profile`, cả hai giới hạn 6 ply,
2 lượt làm nóng + 3 lượt đo mỗi thế HARD. Không dùng lượt có profiler làm số
so sánh tốc độ chính. Mở `results/baseline.jfr`, `results/final.jfr` bằng JDK
Mission Control, hoặc đọc `*-hot-methods.txt` và `*-jfr-summary.txt`.
Các phần trăm hot methods là mẫu CPU thống kê của từng bản, không phải số lần gọi.

JFR ghi 1.039 mẫu thực thi trong hồ sơ baseline (18 giây), 649 mẫu trong hồ sơ
mới (13 giây). Ở baseline, `Evaluator.rawScore` là frame trên cùng của 17 mẫu
(1,64%), `shapeThrough` 76 mẫu (7,31%). Ở bản mới, `shapeThrough` 17 mẫu (2,62%)
và `SearchState.updateWindow` 55 mẫu (8,47%): việc duy trì cửa sổ đe dọa là
một chi phí mới đáng kể. Thời lượng hồ sơ gồm làm nóng và không dùng thay cho
trung vị của phép đo xen kẽ.

67 kiểm thử JUnit qua: gồm 59 kiểm thử cũ, đối chiếu điểm/hash/cache ứng viên
trên hơn 1.600 thao tác ngẫu nhiên và toàn bộ undo; góc bàn, chuỗi dài, khe hở,
winLength 3/5/7; va chạm ô bảng, bound/depth; tìm kiếm có/không TT cho cùng kết
quả và điểm; chứng minh open-four ở ply thường đầu tiên; hủy và dùng lại AI.

## Tái lập

Yêu cầu JDK 21, PowerShell và Maven với dependencies có sẵn hoặc kết nối mạng:

```powershell
mvn test package
.\bench\Run-Benchmark.ps1 -Repeats 5 -Profile
```

Script biên dịch baseline và bản mới riêng, chạy tuần tự, ghi CSV vào
`bench/results`. JFR cần quyền ghi thư mục tạm của Java. Chạy benchmark không
cùng lúc với test/profiler khác. Tệp baseline chỉ để đo, không được Maven đóng
gói vào game. `Benchmark` chỉ đổi maxDepth bằng reflection bên trong tiến trình
benchmark khi nhận tham số độ sâu; mã game không có thay đổi cấu hình này.

Các công cụ đều dùng JDK có sẵn, không thêm thư viện runtime cho game.
