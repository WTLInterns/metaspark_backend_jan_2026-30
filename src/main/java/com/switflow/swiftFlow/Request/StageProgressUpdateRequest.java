package com.switflow.swiftFlow.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StageProgressUpdateRequest {

    private String stage;

    private Integer progress;
}
