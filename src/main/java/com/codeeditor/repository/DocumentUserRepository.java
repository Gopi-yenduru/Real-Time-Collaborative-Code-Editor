package com.codeeditor.repository;

import com.codeeditor.model.DocumentUser;
import com.codeeditor.model.DocumentUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentUserRepository extends JpaRepository<DocumentUser, DocumentUserId> {
    List<DocumentUser> findByIdUserId(Long userId);
    List<DocumentUser> findByIdDocumentId(String documentId);
    Optional<DocumentUser> findByIdDocumentIdAndIdUserId(String documentId, Long userId);
    boolean existsByIdDocumentIdAndIdUserId(String documentId, Long userId);

    /** Removes every membership row for a document (used when deleting it). */
    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentUser du WHERE du.id.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") String documentId);
}
