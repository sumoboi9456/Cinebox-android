# Cập nhật Android: tính năng mới + nút Back

Giải nén gói `CineBox_CapNhat_Android.zip`, chép đè các file vào đúng đường dẫn trong project (giữ nguyên cấu trúc thư mục).
**KHÔNG chép đè `ApiConfig.kt`** (file đó đang trỏ tới máy chủ Render của nhóm).

File Kotlin (app/src/main/java/com/uth/cinebox/):
- ApiClient.kt (thêm put, delete; tăng thời gian chờ)
- Movie.kt, FavoriteDbHelper.kt (SQLite phiên bản 2: thêm trailerKey, note)
- LoginActivity.kt, RegisterActivity.kt, MainActivity.kt, AccountFragment.kt
- MovieDetailActivity.kt
- MỚI: ChangePasswordActivity.kt, MyReviewsActivity.kt

Layout (app/src/main/res/layout/):
- activity_login.xml, activity_register.xml, activity_movie_detail.xml, fragment_account.xml
- MỚI: activity_change_password.xml, activity_my_reviews.xml, item_my_review.xml

Manifest: AndroidManifest.xml (thêm 2 Activity mới). Nếu project đã có chỉnh sửa riêng thì chỉ thêm 2 khối:
ChangePasswordActivity và MyReviewsActivity (exported="false").

Sau khi chép: Build, Rebuild Project. Gỡ app cũ khỏi máy ảo trước khi chạy lại (hoặc Clear data) vì cơ sở dữ liệu yêu thích nâng lên phiên bản 2.
Máy chủ: chép đè server.js, seed.js trong gói cinebox-server.zip, commit và push, Render sẽ tự triển khai lại.
