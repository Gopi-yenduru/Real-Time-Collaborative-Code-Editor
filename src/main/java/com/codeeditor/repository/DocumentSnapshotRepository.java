package com.codeeditor.repository;

import com.codeeditor.model.DocumentSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DocumentSnapshotRepository extends JpaRepository<DocumentSnapshot, Long> {

    Page<DocumentSnapshot> findByDocumentIdOrderByIdDesc(String documentId, Pageable pageable);

    /** Bulk delete — avoids loading each snapshot's LONGBLOB state to remove it. */
    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentSnapshot s WHERE s.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") String documentId);
}
