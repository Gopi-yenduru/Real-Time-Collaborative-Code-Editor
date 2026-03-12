package com.codeeditor.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorMoveMessage {
    private String docId;
    private Long userId;
    private int cursorPosition;
}
