package com.codeeditor.websocket.dto;

import com.codeeditor.model.OpType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationAppliedMessage {
    private OpType opType;
    private int position;
    private String character;
    private int revision;
    private Long appliedBy; // The user who made the change
}
