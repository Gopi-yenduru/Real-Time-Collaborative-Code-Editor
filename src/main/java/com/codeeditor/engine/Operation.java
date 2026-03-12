package com.codeeditor.engine;

import com.codeeditor.model.OpType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Operation {
    private OpType type;
    private int position;
    private String character;
    private int revision;
    private Long userId;
    
    @Builder.Default
    private boolean noOp = false;
    
    public static Operation retain(int position, int revision, Long userId) {
        return Operation.builder()
                .type(OpType.RETAIN)
                .position(position)
                .revision(revision)
                .userId(userId)
                .build();
    }
    
    public static Operation insert(int position, String character, int revision, Long userId) {
         return Operation.builder()
                .type(OpType.INSERT)
                .position(position)
                .character(character)
                .revision(revision)
                .userId(userId)
                .build();
    }
    
    public static Operation delete(int position, int revision, Long userId) {
         return Operation.builder()
                .type(OpType.DELETE)
                .position(position)
                .revision(revision)
                .userId(userId)
                .build();
    }
}
