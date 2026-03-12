package com.codeeditor.websocket;

import com.codeeditor.engine.OTEngine;
import com.codeeditor.engine.Operation;
import com.codeeditor.model.OpType;
import com.codeeditor.redis.RedisMessagePublisher;
import com.codeeditor.security.UserDetailsImpl;
import com.codeeditor.service.DocumentService;
import com.codeeditor.service.RevisionService;
import com.codeeditor.service.SessionManager;
import com.codeeditor.websocket.dto.CursorMoveMessage;
import com.codeeditor.websocket.dto.CursorUpdateMessage;
import com.codeeditor.websocket.dto.JoinSessionMessage;
import com.codeeditor.websocket.dto.OperationAppliedMessage;
import com.codeeditor.websocket.dto.OperationMessage;
import com.codeeditor.websocket.dto.UserJoinedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@Slf4j
@RequiredArgsConstructor
public class EditorWebSocketHandler {

    private final OTEngine otEngine;
    private final RevisionService revisionService;
    private final DocumentService documentService;
    private final RedisMessagePublisher redisPublisher;
    private final SessionManager sessionManager;

    @MessageMapping("/editor/join")
    public void joinSession(@Payload JoinSessionMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        Long userId = getUserId(headerAccessor);
        
        if (userId == null) {
            log.warn("Unauthenticated user tried to join session");
            return;
        }

        String docId = message.getDocId();
        
        // Add to active session and assign a random color
        sessionManager.addUserToSession(docId, userId, sessionId);
        String userColor = sessionManager.getUserColor(docId, userId);
        String username = getUsername(headerAccessor);

        // Broadcast that user joined
        UserJoinedMessage joinMsg = UserJoinedMessage.builder()
                .userId(userId)
                .username(username)
                .color(userColor)
                .build();
                
        redisPublisher.publishUserJoined(docId, joinMsg);
    }

    @MessageMapping("/editor/operation")
    public void processOperation(@Payload OperationMessage message, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserId(headerAccessor);
        if (userId == null) return;

        String docId = message.getDocId();
        
        // Find current document revision to apply OT
        int currentRevision = revisionService.getLatestRevisionNumber(docId);
        
        Operation incomingOp = Operation.builder()
                .type(message.getOpType())
                .position(message.getPosition())
                .character(message.getCharacter())
                .revision(message.getRevision())
                .userId(userId)
                .build();

        // 1. Fetch missing revisions to transform the operation if client is behind
        if (message.getRevision() < currentRevision) {
             List<Operation> pastOps = revisionService.getOperationsAfterRevision(docId, message.getRevision());
             for (Operation pastOp : pastOps) {
                  incomingOp = otEngine.transform(pastOp, incomingOp);
             }
        }
        
        // 2. Adjust revision number to the new latest one
        int newRevisionNumber = currentRevision + 1;
        incomingOp.setRevision(newRevisionNumber);

        // 3. Save operation to database
        if (!incomingOp.isNoOp()) {
            revisionService.saveRevision(docId, incomingOp);
            
            // 4. Update the actual document text
            documentService.applyOperationToDocument(docId, incomingOp);
        }

        // 5. Broadcast transformed operation
        OperationAppliedMessage appliedMsg = OperationAppliedMessage.builder()
                .opType(incomingOp.getType())
                .position(incomingOp.getPosition())
                .character(incomingOp.getCharacter())
                .revision(newRevisionNumber)
                .appliedBy(userId)
                .build();
                
        redisPublisher.publishOperation(docId, appliedMsg);
    }

    @MessageMapping("/editor/cursor")
    public void processCursorMove(@Payload CursorMoveMessage message, SimpMessageHeaderAccessor headerAccessor) {
         Long userId = getUserId(headerAccessor);
         if (userId == null) return;
         
         String docId = message.getDocId();
         String username = getUsername(headerAccessor);
         String color = sessionManager.getUserColor(docId, userId);
         
         CursorUpdateMessage updateMsg = CursorUpdateMessage.builder()
                 .userId(userId)
                 .username(username)
                 .cursorPosition(message.getCursorPosition())
                 .color(color)
                 .build();
                 
         redisPublisher.publishCursorUpdate(docId, updateMsg);
    }

    private Long getUserId(SimpMessageHeaderAccessor headerAccessor) {
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) headerAccessor.getUser();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getId();
        }
        return null;
    }
    
    private String getUsername(SimpMessageHeaderAccessor headerAccessor) {
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) headerAccessor.getUser();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getUsername();
        }
        return "Unknown";
    }
}
