package com.teco.pointtrack.entity.enums;

public enum SwapType {
    /** Hoán đổi ca với NV khác: A lấy ca B, B lấy ca A */
    SWAP,

    /** NV đổi sang ca khác cùng ngày – luôn gửi Admin duyệt */
    SAME_DAY,

    /** NV đổi sang ca ở ngày khác – luôn gửi Admin duyệt */
    OTHER_DAY,

    /** NV nhường hẳn ca cho NV khác (A mất ca, B nhận ca) */
    TRANSFER
}
