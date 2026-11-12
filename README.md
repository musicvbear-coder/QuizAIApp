# 🎓 QuizMaster Mobile - QuizAIApp

Ứng dụng quản lý và khởi tạo đề thi trắc nghiệm thông minh tích hợp Google Gemini AI và Firebase Cloud dành cho giáo viên và học sinh.

---

## 🌟 Tính Năng Nổi Bật

- **✨ Tạo đề thi tự động bằng AI**: Nhập chủ đề bất kỳ hoặc tải lên tài liệu (.docx, .pdf, .txt) để Gemini AI biên soạn câu hỏi trắc nghiệm kèm đáp án và lời giải chi tiết qua REST API.
- **📱 Kiến trúc Single Activity + Fragments**: Chuyển đổi mượt mà giữa Trang chủ, Ngân hàng đề và Trang cá nhân theo chuẩn Google Android.
- **📥 Xuất file PDF kép**: Tự động sinh ra 2 file PDF chuẩn A4 (Đề thi & Đáp án kèm lời giải) lưu trực tiếp vào thư mục Downloads của thiết bị.
- **📄 Nhập file tài liệu tự động**: Đọc và bóc tách câu hỏi từ file Word/TXT/PDF vào Ngân hàng câu hỏi hoặc truyền trực tiếp PDF dạng Base64 sang AI.
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

*Ghi chú: Khi gặp lỗi kết nối hoặc khóa API không hợp lệ, ứng dụng sẽ hiển thị thông báo lỗi chi tiết (kèm mã trạng thái HTTP) để người dùng chủ động thử lại hoặc chọn sử dụng đề mẫu offline.*

---

## 💻 Công Nghệ Sử Dụng

- **Language**: Kotlin (JDK 17, Target SDK 36)
- **Database**: Room Database (SQLite) v2.8.5 & Firebase Firestore
- **Auth**: Firebase Authentication & SharedPreferences SessionManager
- **AI Integration**: Google Gemini REST API (Tự động dò tìm model Flash khả dụng, gửi key qua HTTP header, xử lý PDF inlineData)
- **PDF Engine**: Android PdfDocument & StaticLayout TextPaint
