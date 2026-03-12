package com.codeeditor.engine;

import com.codeeditor.model.OpType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OTEngineTest {

    private OTEngine otEngine;

    @BeforeEach
    void setUp() {
        otEngine = new OTEngine();
    }

    @Test
    void testApplyInsert() {
        String doc = "Hello World";
        Operation op = Operation.insert(5, ",", 1, 1L);
        String result = otEngine.apply(doc, op);
        assertEquals("Hello, World", result);
    }

    @Test
    void testApplyDelete() {
        String doc = "Hello, World";
        Operation op = Operation.delete(5, 1, 1L);
        String result = otEngine.apply(doc, op);
        assertEquals("Hello World", result);
    }

    // Scenario 1: INSERT vs INSERT at same position
    @Test
    void testTransformInsertVsInsertSamePosition() {
        // User 1 inserts 'A' at pos 0 (opA)
        Operation opA = Operation.insert(0, "A", 1, 1L);
        // User 2 inserts 'B' at pos 0 (opB)
        Operation opB = Operation.insert(0, "B", 1, 2L);

        // Transform opB against opA assuming opA was applied first.
        // Since opA has lower userId (1 < 2), it "wins" the tie-breaker and goes first.
        // Therefore, opB's insertion position must shift right by 1.
        Operation transformedOpB = otEngine.transform(opA, opB);

        assertEquals(OpType.INSERT, transformedOpB.getType());
        assertEquals(1, transformedOpB.getPosition());
        assertEquals("B", transformedOpB.getCharacter());
    }

    // Scenario 2: INSERT vs INSERT at different positions
    @Test
    void testTransformInsertVsInsertDifferentPositions() {
        // A inserts at 0, B inserts at 5
        Operation opA = Operation.insert(0, "A", 1, 1L);
        Operation opB = Operation.insert(5, "B", 1, 2L);

        Operation transformedOpB = otEngine.transform(opA, opB);
        // B should shift right because A inserted before B's position
        assertEquals(6, transformedOpB.getPosition());
    }
    
    // Scenario 3: INSERT vs DELETE (insert position <= delete position)
    @Test
    void testTransformInsertVsDelete() {
        // A inserts at 2, B deletes at 5
        Operation opA = Operation.insert(2, "X", 1, 1L);
        Operation opB = Operation.delete(5, 1, 2L);
        
        Operation transformedOpB = otEngine.transform(opA, opB);
        // B's deletion target shifts right because A inserted before it
        assertEquals(6, transformedOpB.getPosition());
    }

    // Scenario 4: DELETE vs INSERT (delete position < insert position)
    @Test
    void testTransformDeleteVsInsert() {
         // A deletes at 2, B inserts at 5
         Operation opA = Operation.delete(2, 1, 1L);
         Operation opB = Operation.insert(5, "Y", 1, 2L);
         
         Operation transformedOpB = otEngine.transform(opA, opB);
         // B's insertion position shifts left because A removed a character before it
         assertEquals(4, transformedOpB.getPosition());
    }

    // Scenario 5: DELETE vs DELETE at same position
    @Test
    void testTransformDeleteVsDeleteSamePosition() {
        // A deletes at 3, B deletes at 3
        Operation opA = Operation.delete(3, 1, 1L);
        Operation opB = Operation.delete(3, 1, 2L);
        
        Operation transformedOpB = otEngine.transform(opA, opB);
        // Since A already deleted it, B's operation becomes a no-op
        assertTrue(transformedOpB.isNoOp());
    }

    // Scenario 6: DELETE vs DELETE at different positions
    @Test
    void testTransformDeleteVsDeleteDifferentPositions() {
        // A deletes at 2, B deletes at 5
        Operation opA = Operation.delete(2, 1, 1L);
        Operation opB = Operation.delete(5, 1, 2L);
        
        Operation transformedOpB = otEngine.transform(opA, opB);
        // B's deletion target shifts left
        assertEquals(4, transformedOpB.getPosition());
    }
}
