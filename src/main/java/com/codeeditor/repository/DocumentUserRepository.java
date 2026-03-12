package com.codeeditor.repository;

import com.codeeditor.model.DocumentUser;
import com.codeeditor.model.DocumentUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentUserRepository extends JpaRepository<DocumentUser, DocumentUserId> {
    List<DocumentUser> findByUserId(Long userId);
    List<DocumentUser> findByDocumentId(String documentId);
}
