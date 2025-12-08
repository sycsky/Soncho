package com.example.aikef.controller;

import com.example.aikef.dto.request.CreateSessionNoteRequest;
import com.example.aikef.dto.request.UpdateSessionNoteRequest;
import com.example.aikef.service.ChatSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 会话备注管理 API
 * 每个会话只能有一个备注,直接存储在chat_sessions表的note字段
 */
@RestController
@RequestMapping("/api/v1/chat/sessions/{sessionId}/note")
public class SessionNoteController {

    private final ChatSessionService chatSessionService;

    public SessionNoteController(ChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService;
    }

    /**
     * 获取会话备注
     */
    @GetMapping
    public String getNote(@PathVariable UUID sessionId) {
        String note = chatSessionService.getSessionNote(sessionId);
        if (note == null || note.isEmpty()) {
            return "";
        }
        return note;
    }

    /**
     * 创建会话备注
     * 如果已存在则返回409冲突
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String createNote(
            @PathVariable UUID sessionId,
            @Valid @RequestBody CreateSessionNoteRequest request) {
        
        // 检查是否已存在
        String existingNote = chatSessionService.getSessionNote(sessionId);
        if (existingNote != null && !existingNote.isEmpty()) {
            throw new IllegalStateException("该会话已有备注,请使用更新接口修改");
        }
        
        return chatSessionService.updateSessionNote(sessionId, request.content());
    }

    /**
     * 更新会话备注
     * 如果不存在则自动创建
     */
    @PutMapping
    public String updateNote(
            @PathVariable UUID sessionId,
            @Valid @RequestBody UpdateSessionNoteRequest request) {
        
        return chatSessionService.updateSessionNote(sessionId, request.content());
    }

    /**
     * 删除会话备注
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNote(@PathVariable UUID sessionId) {
        chatSessionService.updateSessionNote(sessionId, null);
    }
}
