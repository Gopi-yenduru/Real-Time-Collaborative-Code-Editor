package com.codeeditor.websocket.dto;

import com.codeeditor.model.OpType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationMessage {
    private OpType opType;
    private String docId;
    private int position;
    private String character;
    private int revision;
    private Long userId; // The client sending this
}
