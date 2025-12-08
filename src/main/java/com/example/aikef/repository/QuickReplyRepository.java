package com.example.aikef.repository;

import com.example.aikef.model.QuickReply;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuickReplyRepository extends JpaRepository<QuickReply, UUID> {

    List<QuickReply> findBySystemTrue();
}
