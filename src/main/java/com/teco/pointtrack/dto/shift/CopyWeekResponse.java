package com.teco.pointtrack.dto.shift;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CopyWeekResponse {

    int copied;
    int skipped;
    List<ConflictCheckResponse> conflicts;
}
