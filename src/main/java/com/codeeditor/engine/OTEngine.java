package com.codeeditor.engine;

import com.codeeditor.model.OpType;
import org.springframework.stereotype.Component;

/**
 * Core Operational Transformation Engine
 */
@Component
public class OTEngine {

    /**
     * Transforms operation B against operation A.
     * Assumes A was applied first, and we need to modify B so it can be applied after A.
     *
     * @param opA The operation that was already applied
     * @param opB The incoming operation to be transformed
     * @return The new transformed operation B
     */
    public Operation transform(Operation opA, Operation opB) {
        if (opB.isNoOp() || opA.isNoOp()) {
            return opB;
        }

        // Clone opB to create the transformed result
        Operation transformed = Operation.builder()
                .type(opB.getType())
                .position(opB.getPosition())
                .character(opB.getCharacter())
                .revision(opB.getRevision())
                .userId(opB.getUserId())
                .noOp(opB.isNoOp())
                .build();

        if (opA.getType() == OpType.INSERT && opB.getType() == OpType.INSERT) {
            // INSERT vs INSERT
            if (opA.getPosition() < opB.getPosition() || 
               (opA.getPosition() == opB.getPosition() && opA.getUserId() < opB.getUserId())) {
                // If A was inserted before B, B shifts right
                // Tie-breaker: if same position, lower userId wins (inserted first), so higher userId shifts right
                transformed.setPosition(opB.getPosition() + 1);
            }
        } 
        else if (opA.getType() == OpType.INSERT && opB.getType() == OpType.DELETE) {
            // INSERT vs DELETE
            if (opA.getPosition() <= opB.getPosition()) {
                // If A inserted before or exactly at B's deletion point, B's target shifts right
                transformed.setPosition(opB.getPosition() + 1);
            }
        }
        else if (opA.getType() == OpType.DELETE && opB.getType() == OpType.INSERT) {
            // DELETE vs INSERT
            if (opA.getPosition() < opB.getPosition()) {
                // If A deleted something before B's insertion point, B shifts left
                transformed.setPosition(opB.getPosition() - 1);
            }
        }
        else if (opA.getType() == OpType.DELETE && opB.getType() == OpType.DELETE) {
            // DELETE vs DELETE
            if (opA.getPosition() < opB.getPosition()) {
                // A deleted before B, so B shifts left
                transformed.setPosition(opB.getPosition() - 1);
            } else if (opA.getPosition() == opB.getPosition()) {
                // Both trying to delete the same character. B becomes a no-op
                transformed.setNoOp(true);
            }
        }
        // RETAIN operations do not alter positions for other operations

        return transformed;
    }

    /**
     * Applies an operation to a document string.
     *
     * @param document The current document text
     * @param op       The operation to apply
     * @return The resulting document text after application
     */
    public String apply(String document, Operation op) {
        if (document == null) {
            document = "";
        }
        
        if (op.isNoOp()) {
            return document;
        }

        StringBuilder sb = new StringBuilder(document);

        if (op.getType() == OpType.INSERT) {
            // Guard against out of bounds
            int pos = Math.min(Math.max(0, op.getPosition()), sb.length());
            sb.insert(pos, op.getCharacter());
        } else if (op.getType() == OpType.DELETE) {
            // Guard against out of bounds or empty document
            if (sb.length() > 0 && op.getPosition() >= 0 && op.getPosition() < sb.length()) {
                sb.deleteCharAt(op.getPosition());
            }
        }

        return sb.toString();
    }
}
