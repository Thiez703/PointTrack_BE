package com.teco.pointtrack.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

@ControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    private static final String ERROR_LOG_FORMAT = "Error: URI: {}, ErrorCode: {}, Message: {}";

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorDetail> handleNotFoundException(NotFoundException ex, WebRequest request) {
        String message = ex.getMessage();
        ErrorDetail errorVm = new ErrorDetail(HttpStatus.NOT_FOUND.toString(), "Not Found", message);
        log.warn(ERROR_LOG_FORMAT, getServletPath(request), 404, message);
        return new ResponseEntity<>(errorVm, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorDetail> handleBadRequestException(BadRequestException ex, WebRequest request) {
        String message = ex.getMessage();
        ErrorDetail errorVm = new ErrorDetail(HttpStatus.BAD_REQUEST.toString(), "Bad Request", message);
        log.warn(ERROR_LOG_FORMAT, getServletPath(request), 400, message);
        return ResponseEntity.badRequest().body(errorVm);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorDetail> handleConflictException(ConflictException ex, WebRequest request) {
        String message = ex.getMessage();
        ErrorDetail errorVm = new ErrorDetail(HttpStatus.CONFLICT.toString(), "Conflict", message);
        log.warn(ERROR_LOG_FORMAT, getServletPath(request), 409, message);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorVm);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDetail> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        String message = "Dữ liệu gửi lên không hợp lệ hoặc sai định dạng.";
        ErrorDetail errorVm = new ErrorDetail("400", "Bad Request", message);
        log.warn(ERROR_LOG_FORMAT, getServletPath(request), 400, ex.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest().body(errorVm);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage()).toList();
        ErrorDetail errorVm = new ErrorDetail("400", "Bad Request", "Dữ liệu validation không hợp lệ", errors);
        return ResponseEntity.badRequest().body(errorVm);
    }

    @ExceptionHandler(SignInRequiredException.class)
    public ResponseEntity<ErrorDetail> handleSignInRequired(SignInRequiredException ex) {
        String message = ex.getMessage();
        ErrorDetail errorVm = new ErrorDetail(HttpStatus.UNAUTHORIZED.toString(), "Authentication required", message);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorVm);
    }

    @ExceptionHandler({Forbidden.class, AccessDeniedException.class})
    public ResponseEntity<ErrorDetail> handleForbidden(Exception ex, WebRequest request) {
        String message = ex.getMessage();
        ErrorDetail errorVm = new ErrorDetail(HttpStatus.FORBIDDEN.toString(), "Forbidden", message);
        log.warn(ERROR_LOG_FORMAT, getServletPath(request), 403, message);
        return new ResponseEntity<>(errorVm, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorDetail> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        String message = "Thông tin xác thực không hợp lệ: " + ex.getMessage();
        ErrorDetail errorVm = new ErrorDetail(HttpStatus.UNAUTHORIZED.toString(), "Authentication failed", message);
        log.warn(ERROR_LOG_FORMAT, getServletPath(request), 401, message);
        return new ResponseEntity<>(errorVm, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        String detail = "Dữ liệu vi phạm ràng buộc cơ sở dữ liệu.";
        Throwable cause = ex.getRootCause();
        if (cause != null && cause.getMessage() != null && cause.getMessage().contains("Duplicate entry")) {
            detail = "Giá trị bị trùng lặp trong hệ thống.";
        }
        ErrorDetail errorVm = new ErrorDetail(HttpStatus.CONFLICT.toString(), "Xung đột dữ liệu", detail);
        log.warn(ERROR_LOG_FORMAT, getServletPath(request), 409, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorVm);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorDetail> handleNoResourceFound(NoHandlerFoundException ex, WebRequest request) {
        String message = "Không tìm thấy API này trong hệ thống: " + ex.getRequestURL();
        ErrorDetail errorVm = new ErrorDetail(HttpStatus.NOT_FOUND.toString(), "Endpoint Not Found", message);
        log.warn(ERROR_LOG_FORMAT, getServletPath(request), 404, message);
        return new ResponseEntity<>(errorVm, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorDetail> handleOtherException(Exception ex, WebRequest request) {
        String message = ex.getMessage();
        // Hiển thị message lỗi thực sự thay vì null để dễ debug
        ErrorDetail errorVm = new ErrorDetail(HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                "Internal Server Error", message != null ? message : ex.getClass().getSimpleName());
        
        log.error("SYSTEM ERROR at {}: {}", getServletPath(request), message, ex);
        return new ResponseEntity<>(errorVm, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String getServletPath(WebRequest webRequest) {
        ServletWebRequest servletRequest = (ServletWebRequest) webRequest;
        return servletRequest.getRequest().getServletPath();
    }
}

