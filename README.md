# Dream-Soft-Web

Ứng dụng web quản lý thông tin vật phẩm game với Spring Boot và Thymeleaf.

## Tính năng

- Quản lý nhiều game configuration
- Hỗ trợ nhiều theme màu sắc tùy chỉnh
- API RESTful để truy xuất thông tin vật phẩm
- Giao diện responsive với animations

## Cấu trúc dự án

```
src/main/resources/
├── data/                          # Thư mục chứa dữ liệu JSON
│   ├── item_info_htht.json       # Dữ liệu Hải Tặc
│   ├── item_info_nro2.json       # Dữ liệu Ngọc Rồng Online 2
│   └── item_info_tnah.json       # Dữ liệu Thiên Mệnh Anh Hùng
├── static/
│   ├── css/
│   │   └── game.css              # Style và theme configurations
│   └── js/
│       └── game.js               # JavaScript logic
├── templates/                     # Thymeleaf templates
└── application.yaml              # Spring configuration
```

## Hướng dẫn thêm game mới

### Bước 1: Thêm dữ liệu
Tạo file JSON mới trong thư mục `src/main/resources/data/`:
```json
// item_info_<tên_game>.json
[
  {
    "id": 1,
    "name": "Tên vật phẩm",
    "icon": "icon_path",
    "description": "Mô tả"
  }
]
```

### Bước 2: Cấu hình trong application.yaml
Thêm configuration vào file `src/main/resources/application.yaml`:
```yaml
game:
  configs:
    <tên_key>:
      name: "Tên Game"
      data-file: "classpath:data/item_info_<tên_game>.json"
      icon: "🎮"
      api: "/api/item-info/<tên_key>"
      color: "#HexColor"
```

**Ví dụ:**
```yaml
game:
  configs:
    htht:
      name: "Hải Tặc Huyền Thoại"
      data-file: "classpath:data/item_info_htht.json"
      icon: "⚔️"
      api: "/api/item-info/htht"
      color: "#2a5298"
```

### Bước 3: Thêm theme CSS
Thêm vào file `src/main/resources/static/css/game.css`:
```css
/* Theme cho body */
body.<tên_key>-theme {
  background: linear-gradient(135deg, #color1 0%, #color2 100%);
}

/* Theme cho button */
.game-btn.<tên_key> {
  background: linear-gradient(135deg, #color1 0%, #color2 100%);
  color: white;
}
```

**Ví dụ:**
```css
/* Game Hải Tặc */
body.htht-theme {
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
}

.game-btn.htht {
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
  color: white;
}
```

### Bước 4: Cập nhật game.js
Thêm mapping API trong file `src/main/resources/static/js/game.js`:
```javascript
const GAMES = {
  <tên_key>: {
    name: 'Tên Game',
    icon: '🎮',
    api: '/api/item-info/<tên_key>',
    color: '#HexColor'
  },
  // ... các game khác
};
```

**Ví dụ:**
```javascript
const GAMES = {
  htht: {
    name: 'Hải Tặc',
    icon: '⚔️',
    api: '/api/item-info/htht',
    color: '#2a5298'
  },
  nro2: {
    name: 'Ngọc Rồng Online 2',
    icon: '🐉',
    api: '/api/item-info/nro2',
    color: '#f5af19'
  },
  tnah: {
    name: 'Thiên Mệnh Anh Hùng',
    icon: '⚔️',
    api: '/api/item-info/tnah',
    color: '#34695e'
  }
};
```

## Yêu cầu hệ thống

- Java 11+
- Maven 3.6+
- Spring Boot 2.x

## Cài đặt và chạy

```bash
# Clone repository
git clone <repository-url>
cd Dream-Soft-Web

# Build project
mvn clean install

# Run application
mvn spring-boot:run
```

Ứng dụng sẽ chạy tại: `http://localhost:9020`

## API Endpoints

- `GET /api/item-info/{gameKey}` - Lấy thông tin vật phẩm theo game
    - gameKey: `htht`, `nro2`, `tnah`

## Công nghệ sử dụng

- **Backend**: Spring Boot, Spring MVC
- **Template Engine**: Thymeleaf
- **Frontend**: HTML5, CSS3, JavaScript (ES6+)
- **Build Tool**: Maven
- **Server**: Tomcat Embedded

## Cấu trúc package

```
com.dreamsoft.web/
├── controller/          # REST Controllers
├── service/            # Business Logic
├── dto/                # Data Transfer Objects
├── mapper/             # Entity Mappers
└── entity/             # Domain Models
```

## Đóng góp

Mọi đóng góp đều được chào đón! Vui lòng:
1. Fork repository
2. Tạo branch mới (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Mở Pull Request

## License

MIT License - xem file [LICENSE](LICENSE) để biết thêm chi tiết

## Tác giả

Dream-Soft Team

## Liên hệ

- Website: [Your Website]
- Email: [Your Email]

---

⭐ Nếu project này hữu ích, hãy cho một star nhé!