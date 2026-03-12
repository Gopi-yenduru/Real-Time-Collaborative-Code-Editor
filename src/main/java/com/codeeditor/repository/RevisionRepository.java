package com.codeeditor.repository;

import com.codeeditor.model.Revision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RevisionRepository extends JpaRepository<Revision, Long> {

    @Query("SELECT r FROM Revision r WHERE r.document.id = :docId ORDER BY r.revisionNumber ASC")
    List<Revision> findAllByDocumentIdOrderByRevisionNumberAsc(@Param("docId") String docId);

    @Query("SELECT r FROM Revision r WHERE r.document.id = :docId AND r.revisionNumber > :revisionNumber ORDER BY r.revisionNumber ASC")
    List<Revision> findByDocumentIdAndRevisionNumberGreaterThanOrderByRevisionNumberAsc(@Param("docId") String docId, @Param("revisionNumber") int revisionNumber);

    @Query("SELECT MAX(r.revisionNumber) FROM Revision r WHERE r.document.id = :docId")
    Optional<Integer> findMaxRevisionNumberByDocumentId(@Param("docId") String docId);

    Page<Revision> findByDocumentIdOrderByRevisionNumberDesc(String documentId, Pageable pageable);
}
