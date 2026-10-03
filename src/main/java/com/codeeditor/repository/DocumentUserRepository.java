package com.codeeditor.repository;

import com.codeeditor.model.DocumentUser;
import com.codeeditor.model.DocumentUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentUserRepository extends JpaRepository<DocumentUser, DocumentUserId> {
    List<DocumentUser> findByIdUserId(Long userId);
    List<DocumentUser> findByIdDocumentId(String documentId);
    Optional<DocumentUser> findByIdDocumentIdAndIdUserId(String documentId, Long userId);
    boolean existsByIdDocumentIdAndIdUserId(String documentId, Long userId);
}
