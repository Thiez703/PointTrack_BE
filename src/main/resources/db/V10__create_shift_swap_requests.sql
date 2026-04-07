-- ============================================================
-- V10: Tính năng Đổi Ca (Shift Swap)
-- ============================================================

CREATE TABLE shift_swap_requests (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,

    type                ENUM('SWAP','SAME_DAY','OTHER_DAY','TRANSFER') NOT NULL
                        COMMENT 'SWAP=hoán đổi; SAME_DAY=ca khác cùng ngày; OTHER_DAY=ngày khác; TRANSFER=nhường ca',

    status              ENUM('PENDING_EMPLOYEE','PENDING_ADMIN','APPROVED','REJECTED','CANCELLED') NOT NULL
                        DEFAULT 'PENDING_EMPLOYEE'
                        COMMENT 'PENDING_EMPLOYEE=chờ NV xác nhận; PENDING_ADMIN=chờ Admin duyệt',

    -- NV yêu cầu
    requester_id        BIGINT NOT NULL COMMENT 'NV_A - người gửi yêu cầu',
    requester_shift_id  BIGINT NOT NULL COMMENT 'Ca của NV_A muốn đổi đi',

    -- NV nhận (null nếu gửi Admin)
    receiver_id         BIGINT          COMMENT 'NV_B - người nhận yêu cầu (SWAP/TRANSFER)',
    receiver_shift_id   BIGINT          COMMENT 'Ca của NV_B (SWAP: bắt buộc; TRANSFER: null)',

    -- Ca mục tiêu (SAME_DAY / OTHER_DAY)
    target_shift_id     BIGINT          COMMENT 'Ca mới muốn chuyển sang (SAME_DAY / OTHER_DAY)',
    target_date         DATE            COMMENT 'Ngày muốn đổi sang (OTHER_DAY)',

    reason              TEXT NOT NULL   COMMENT 'Lý do đổi ca (bắt buộc)',
    reject_reason       TEXT            COMMENT 'Lý do từ chối (NV_B hoặc Admin)',

    -- Deadline NV_B phản hồi (mặc định 24h, null nếu gửi Admin)
    expired_at          DATETIME        COMMENT 'Deadline phản hồi (PENDING_EMPLOYEE)',

    -- Ai duyệt / từ chối
    reviewed_by         BIGINT          COMMENT 'ID người duyệt/từ chối (NV_B hoặc Admin)',
    reviewed_at         DATETIME        COMMENT 'Thời điểm duyệt/từ chối',

    -- BaseEntity audit
    created_at          DATETIME,
    updated_at          DATETIME,
    created_by_user_id  BIGINT,
    updated_by_user_id  BIGINT,

    CONSTRAINT fk_swap_requester       FOREIGN KEY (requester_id)       REFERENCES users(id),
    CONSTRAINT fk_swap_receiver        FOREIGN KEY (receiver_id)        REFERENCES users(id),
    CONSTRAINT fk_swap_req_shift       FOREIGN KEY (requester_shift_id) REFERENCES shifts(id),
    CONSTRAINT fk_swap_rec_shift       FOREIGN KEY (receiver_shift_id)  REFERENCES shifts(id),
    CONSTRAINT fk_swap_target_shift    FOREIGN KEY (target_shift_id)    REFERENCES shifts(id),
    CONSTRAINT fk_swap_reviewed_by     FOREIGN KEY (reviewed_by)        REFERENCES users(id)
);

CREATE INDEX idx_swap_requester ON shift_swap_requests(requester_id, status);
CREATE INDEX idx_swap_receiver  ON shift_swap_requests(receiver_id,  status);
CREATE INDEX idx_swap_status    ON shift_swap_requests(status, created_at);
