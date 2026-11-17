#!/usr/bin/env python3
"""Package a built CaroAI JAR using only the Python standard library."""
import argparse
import hashlib
from pathlib import Path
import re
import shutil
from urllib.parse import urlsplit
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--tag', required=True, help='Release tag, for example v1.0.0')
    parser.add_argument('--jar', type=Path, default=Path('target/caroai.jar'))
    parser.add_argument('--output', type=Path, default=Path('dist'))
    parser.add_argument('--play-url', help='Verified public HTTPS game URL; omit for offline packaging')
    parser.add_argument('--repository', help='GitHub owner/repository for source attribution')
    parser.add_argument('--source-sha', help='Source commit SHA for attribution')
    args = parser.parse_args()
    if not re.fullmatch(r'v[0-9]+\.[0-9]+\.[0-9]+', args.tag):
        parser.error('--tag must have the form vMAJOR.MINOR.PATCH')
    play_url = args.play_url.rstrip('/') if args.play_url else None
    if play_url:
        parts = urlsplit(play_url)
        if (parts.scheme != 'https' or not parts.hostname or parts.username
                or parts.password or parts.query or parts.fragment):
            parser.error('--play-url must be HTTPS without credentials, query or fragment')
    if not args.jar.is_file():
        parser.error(f'JAR not found: {args.jar}; run mvn verify first')

    args.output.mkdir(parents=True, exist_ok=True)
    jar = args.output / f'caroai-{args.tag}.jar'
    shutil.copyfile(args.jar, jar)
    play_line = f'Chơi ngay trên trình duyệt: {play_url}\n\n' if play_url else ''
    source_line = ''
    if args.repository and args.source_sha:
        source_line = f'\nMã nguồn: https://github.com/{args.repository}/tree/{args.source_sha}\n'
    readme = f'''# CaroAI {args.tag}

{play_line}Chạy từ gói tải xuống cần Java 21 trở lên. Không cần Maven hoặc mã nguồn.
Giải nén ZIP, mở terminal trong thư mục chứa caroai.jar.

Desktop (cần môi trường đồ họa):
```sh
java -jar caroai.jar
```

Web chạy trên máy của bạn:
```sh
java "-Djava.awt.headless=true" -cp caroai.jar caroai.web.WebServer
```
Mở http://127.0.0.1:8088 và giữ terminal đang chạy. Ctrl+C để dừng server.
Hai người chơi trên cùng thiết bị; chưa có phòng đấu giữa hai thiết bị.
Lưu/mở JSON và tự lưu ván chỉ có trên bản web.

JAR tải riêng chạy bằng cùng lệnh, thay caroai.jar bằng tên tệp đã tải.
{source_line}'''
    archive = args.output / f'CaroAI-{args.tag}.zip'
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as z:
        z.write(jar, f'CaroAI-{args.tag}/caroai.jar')
        z.writestr(f'CaroAI-{args.tag}/README.md', readme)
    checksums = args.output / 'SHA256SUMS.txt'
    checksums.write_text(''.join(
        f'{hashlib.sha256(path.read_bytes()).hexdigest()}  {path.name}\n'
        for path in (jar, archive)
    ), encoding='utf-8')
    first_link = f'[Chơi ngay]({play_url})\n\n' if play_url else ''
    notes = f'''{first_link}CaroAI {args.tag}: chơi Caro với AI ba độ khó hoặc hai người trên cùng thiết bị.

- Tải ZIP để chạy desktop/web tại máy; cần Java 21 trở lên, không cần Maven.
- ZIP có JAR và hướng dẫn chạy. SHA256SUMS.txt dùng kiểm tra tệp tải xuống.
'''
    if play_url:
        notes += '- Link Chơi ngay mở bản web, không cần cài Java.\n'
    if args.source_sha:
        notes += f'\nMã nguồn phát hành: `{args.source_sha}`.\n'
    (args.output / 'release-notes.md').write_text(notes, encoding='utf-8')
    for path in (jar, archive, checksums):
        print(path)


if __name__ == '__main__':
    main()
