package com.pfm.personalfinancemanager.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu đầu vào không hợp lệ"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Định dạng dữ liệu gửi lên không đúng"),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "Thiếu tham số bắt buộc"),
    TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", "Kiểu dữ liệu tham số không hợp lệ"),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Xác thực thất bại hoặc phiên đăng nhập đã hết hạn"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email hoặc mật khẩu không chính xác"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Token không hợp lệ hoặc đã hết hạn"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này"),
    ACCESS_DENIED_OWNERSHIP(HttpStatus.FORBIDDEN, "ACCESS_DENIED_OWNERSHIP", "Bạn không có quyền truy cập tài nguyên này"),

    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy dữ liệu yêu cầu"),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "Dữ liệu đã tồn tại trong hệ thống"),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", "Thao tác vi phạm ràng buộc dữ liệu"),

    INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE", "Số dư tài khoản không đủ để thực hiện giao dịch"),
    BUSINESS_RULE_VIOLATION(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION", "Vi phạm quy tắc nghiệp vụ tài chính"),
    INVALID_RESET_TOKEN(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", "Token đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"),
    INVALID_TRANSACTION_AMOUNT(HttpStatus.BAD_REQUEST, "INVALID_TRANSACTION_AMOUNT", "Số tiền giao dịch phải lớn hơn 0"),
    CATEGORY_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "CATEGORY_TYPE_MISMATCH", "Loại danh mục không khớp với loại giao dịch"),
    SAME_ACCOUNT_TRANSFER(HttpStatus.BAD_REQUEST, "SAME_ACCOUNT_TRANSFER", "Tài khoản nguồn và tài khoản đích không được trùng nhau"),
    IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", "Yêu cầu giao dịch đang được xử lý hoặc đã tồn tại"),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Quá nhiều yêu cầu, vui lòng thử lại sau");

    private final HttpStatus httpStatus;
    private final String code;
    private final String defaultMessage;
}

