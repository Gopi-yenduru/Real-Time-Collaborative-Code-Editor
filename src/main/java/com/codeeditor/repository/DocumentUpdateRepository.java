package com.codeeditor.repository;

import com.codeeditor.model.DocumentUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DocumentUpdateRepository extends JpaRepository<DocumentUpdate, Long> {

    List<DocumentUpdate> findByDocumentIdOrderByIdAsc(String documentId);

    long countByDocumentId(String documentId);

    @Query("SELECT MAX(u.id) FROM DocumentUpdate u WHERE u.documentId = :documentId")
    Long findMaxId(@Param("documentId") String documentId);

    @Transactional
    void deleteByDocumentId(String documentId);

    /** Delete only rows up to a marker — preserves concurrent updates during compaction. */
    @Transactional
    void deleteByDocumentIdAndIdLessThanEqual(String documentId, Long maxId);
}
