# 🎓 QuizMaster Mobile - QuizAIApp

Ứng dụng quản lý và khởi tạo đề thi trắc nghiệm thông minh tích hợp Google Gemini AI và Firebase Cloud dành cho giáo viên và học sinh.

---

## 🌟 Tính Năng Nổi Bật

- **✨ Tạo đề thi tự động bằng AI**: Nhập chủ đề bất kỳ và số lượng câu hỏi để Gemini AI biên soạn câu hỏi trắc nghiệm kèm đáp án và lời giải chi tiết.
- **📱 Kiến trúc Single Activity + Fragments**: Chuyển đổi mượt mượt giữa Trang chủ, Ngân hàng đề và Trang cá nhân theo chuẩn Google Android.
- **📥 Xuất file PDF kép**: Tự động sinh ra 2 file PDF chuẩn A4 (Đề thi & Đáp án kèm lời giải) lưu trực tiếp vào thư mục Downloads của thiết bị.
- **📄 Nhập file tài liệu tự động**: Đọc và bóc tách câu hỏi từ file Word/TXT vào Ngân hàng câu hỏi.
- **☁️ Đồng bộ đám mây Firebase**: Đăng ký/Đăng nhập tài khoản và sao lưu đề thi lên Firebase Cloud Authentication & Firestore Database.
- **🎨 Giao diện Light Theme Hiện đại**: Tông màu Sáng Trắng & Đen nhám tương phản cao, thẻ bo góc hiện đại.

---

## 🛠️ Hướng Dẫn Cấu Hình API Key (Dành cho Giáo viên & Người đánh giá)

Dự án sử dụng cơ chế bảo mật khóa API thông qua file `local.properties` (không commit lên VCS để đảm bảo an toàn):

1. Mở file `local.properties` ở thư mục gốc dự án.
2. Thêm dòng sau với khóa Gemini API của bạn:
   ```properties
   GEMINI_API_KEY=AIzaSyYourGeminiApiKeyHere
   ```
3. Bấm **Sync Project with Gradle Files** và chạy ứng dụng.

*Ghi chú: Nếu không cấu hình `GEMINI_API_KEY`, ứng dụng sẽ tự động kích hoạt **Chế độ Thi Thông Minh Dự Phòng (Smart Local Engine)** cho các môn Toán, Tiếng Anh, Tin Học, Lịch Sử, Hóa Học mà không gây ra bất kỳ lỗi hệ thống nào.*

---

## 💻 Công Nghệ Sử Dụng

- **Language**: Kotlin (JDK 17, Target SDK 36)
- **Database**: Room Database (SQLite) v2.8.5 & Firebase Firestore
- **Auth**: Firebase Authentication & SharedPreferences SessionManager
- **AI Model**: Google Gemini AI (1.5 Flash / 2.0 Flash) REST API
- **PDF Engine**: Android PdfDocument & StaticLayout TextPaint
