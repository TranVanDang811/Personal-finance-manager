# Tài liệu API — DangCap Personal Finance Manager

Base URL mặc định: `/api/v1`

Tất cả phản hồi đều chuẩn hóa theo cấu trúc `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "...",
  "data": {},
  "errorCode": null,
  "errors": null,
  "timestamp": "2026-11-12T08:00:00Z"
}
```

Các endpoint cần xác thực yêu cầu header:
`Authorization: Bearer <accessToken>`

---

## 1. Health & System
- `GET /api/v1/health` *(Public)* — Kiểm tra trạng thái ứng dụng và kết nối Database.

## 2. Authentication (`/api/v1/auth`)
*(Áp dụng Rate Limit cho các endpoint POST công khai)*
- `POST /api/v1/auth/register` *(Public)* — Đăng ký tài khoản mới (`fullName`, `email`, `password`, `currency`).
- `POST /api/v1/auth/login` *(Public)* — Đăng nhập, trả về `accessToken`, `refreshToken`, `user`.
- `POST /api/v1/auth/refresh-token` *(Public)* — Làm mới token theo cơ chế Rotation.
- `POST /api/v1/auth/logout` *(Auth)* — Đưa Access Token (`jti`) vào Redis Blacklist và thu hồi Refresh Token.
- `POST /api/v1/auth/forgot-password` *(Public)* — Gửi yêu cầu khôi phục mật khẩu qua email.
- `POST /api/v1/auth/reset-password` *(Public)* — Đặt lại mật khẩu bằng token xác thực.

## 3. User Profile (`/api/v1/users`)
- `GET /api/v1/users/me` — Lấy thông tin cá nhân hiện tại.
- `PUT /api/v1/users/me` — Cập nhật `fullName`, `currency`.
- `POST /api/v1/users/me/change-password` — Đổi mật khẩu và thu hồi toàn bộ Refresh Token cũ.
- `POST /api/v1/users/me/avatar` *(`multipart/form-data`, field `file`)* — Tải lên ảnh đại diện (JPG/PNG/WEBP, tối đa 2MB).

## 4. Accounts (`/api/v1/accounts`)
- `POST /api/v1/accounts` — Tạo ví/tài khoản (`CASH`, `BANK_ACCOUNT`, `SAVINGS`).
- `GET /api/v1/accounts` — Danh sách tài khoản (hỗ trợ lọc `status`).
- `GET /api/v1/accounts/summary` — Tổng số dư theo nhóm tài khoản.
- `GET /api/v1/accounts/{id}` — Chi tiết tài khoản.
- `GET /api/v1/accounts/{id}/transactions` — Danh sách giao dịch liên quan đến tài khoản (phân trang).
- `PUT /api/v1/accounts/{id}` — Cập nhật thông tin tài khoản.
- `PATCH /api/v1/accounts/{id}/archive` — Lưu trữ tài khoản.
- `PATCH /api/v1/accounts/{id}/unarchive` — Khôi phục tài khoản.
- `DELETE /api/v1/accounts/{id}` — Xóa tài khoản (chưa phát sinh giao dịch).

## 5. Categories (`/api/v1/categories`)
- `GET /api/v1/categories` — Danh sách danh mục hệ thống + cá nhân (hỗ trợ lọc `type`).
- `GET /api/v1/categories/{id}` — Chi tiết danh mục.
- `POST /api/v1/categories` — Tạo danh mục cá nhân (`INCOME` / `EXPENSE`).
- `PUT /api/v1/categories/{id}` — Cập nhật danh mục cá nhân.
- `DELETE /api/v1/categories/{id}` — Xóa danh mục cá nhân.

## 6. Transactions (`/api/v1/transactions`)
- `POST /api/v1/transactions` — Tạo giao dịch (`INCOME`, `EXPENSE`, `TRANSFER`), hỗ trợ header `X-Idempotency-Key`.
- `GET /api/v1/transactions` — Tìm kiếm, lọc (`keyword`, `transactionType`, `accountId`, `categoryId`, `startDate`, `endDate`) và phân trang (`size` tối đa `100`).
- `GET /api/v1/transactions/{id}` — Chi tiết giao dịch.
- `PUT /api/v1/transactions/{id}` — Cập nhật giao dịch (tự động hoàn nguyên số dư cũ & áp dụng số dư mới).
- `DELETE /api/v1/transactions/{id}` — Xóa giao dịch và hoàn nguyên số dư.
- `POST /api/v1/transactions/{id}/attachment` *(`multipart/form-data`, field `file`)* — Upload ảnh hóa đơn/minh chứng (JPG/PNG/WEBP, tối đa 5MB).

## 7. Budgets (`/api/v1/budgets`)
- `POST /api/v1/budgets` — Tạo hạn mức chi tiêu theo danh mục & tháng/năm.
- `GET /api/v1/budgets` — Danh sách ngân sách.
- `GET /api/v1/budgets/summary` — Tổng hợp tiến độ ngân sách trong kỳ (`month`, `year`).
- `GET /api/v1/budgets/{id}` — Chi tiết ngân sách.
- `PUT /api/v1/budgets/{id}` — Cập nhật hạn mức.
- `DELETE /api/v1/budgets/{id}` — Xóa ngân sách.

## 8. Saving Goals (`/api/v1/saving-goals`)
- `POST /api/v1/saving-goals` — Tạo mục tiêu tiết kiệm.
- `GET /api/v1/saving-goals` — Danh sách mục tiêu (lọc `status`).
- `GET /api/v1/saving-goals/{id}` — Chi tiết mục tiêu.
- `PUT /api/v1/saving-goals/{id}` — Cập nhật mục tiêu.
- `PATCH /api/v1/saving-goals/{id}/cancel` — Hủy mục tiêu.
- `DELETE /api/v1/saving-goals/{id}` — Xóa mục tiêu.
- `POST /api/v1/saving-goals/{id}/deposit` — Nạp tiền từ tài khoản vào mục tiêu.
- `POST /api/v1/saving-goals/{id}/withdraw` — Rút tiền từ mục tiêu về tài khoản.
- `GET /api/v1/saving-goals/{id}/contributions` — Lịch sử nạp/rút của mục tiêu (phân trang).

## 9. Recurring Transactions (`/api/v1/recurring-transactions`)
- `POST /api/v1/recurring-transactions` — Tạo lịch giao dịch định kỳ (`DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`).
- `GET /api/v1/recurring-transactions` — Danh sách lịch định kỳ.
- `GET /api/v1/recurring-transactions/{id}` — Chi tiết lịch định kỳ.
- `PUT /api/v1/recurring-transactions/{id}` — Cập nhật lịch định kỳ.
- `PATCH /api/v1/recurring-transactions/{id}/pause` — Tạm dừng.
- `PATCH /api/v1/recurring-transactions/{id}/resume` — Tiếp tục.
- `PATCH /api/v1/recurring-transactions/{id}/cancel` — Hủy bỏ.
- `POST /api/v1/recurring-transactions/{id}/execute` — Thực thi thủ công ngay một kỳ.
- `DELETE /api/v1/recurring-transactions/{id}` — Xóa lịch định kỳ.

## 10. Reports & Analytics (`/api/v1/reports`) *(Cached on Redis)*
- `GET /api/v1/reports/dashboard` — Tổng hợp tài sản ròng, thu/chi tháng, ngân sách, mục tiêu, top danh mục & giao dịch gần đây (`month`, `year`).
- `GET /api/v1/reports/category-breakdown` — Tỷ trọng theo danh mục (`type`, `startDate`, `endDate`, `month`, `year`).
- `GET /api/v1/reports/cashflow-trend` — Xu hướng dòng tiền theo nhiều tháng (`months`, `month`, `year`).
- `GET /api/v1/reports/period-summary` — Tổng hợp thu/chi/chuyển khoản và biến động theo từng ngày trong kỳ.

## 11. Notifications (`/api/v1/notifications`)
- `GET /api/v1/notifications` — Danh sách thông báo phân trang (lọc `isRead`).
- `GET /api/v1/notifications/unread-count` — Đếm số thông báo chưa đọc.
- `PATCH /api/v1/notifications/{id}/read` — Đánh dấu một thông báo đã đọc.
- `PATCH /api/v1/notifications/read-all` — Đánh dấu tất cả đã đọc.
- `DELETE /api/v1/notifications/{id}` — Xóa thông báo.

