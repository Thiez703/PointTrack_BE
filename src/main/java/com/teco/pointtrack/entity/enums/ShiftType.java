package com.teco.pointtrack.entity.enums;

public enum ShiftType {
    /** Ca thường – chỉ trong ngày, end > start */
    NORMAL,

    /** Ca Lễ/Tết – chỉ trong ngày, end > start, hệ số OT x2.0–x3.0 */
    HOLIDAY,

    /** Ca OT đột xuất – cho phép end < start (qua đêm), hệ số x1.5 */
    OT_EMERGENCY
}