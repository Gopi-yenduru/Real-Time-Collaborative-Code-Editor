package com.codeeditor.repository;

import com.codeeditor.model.DocumentSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentSnapshotRepository extends JpaRepository<DocumentSnapshot, Long> {

    Page<DocumentSnapshot> findByDocumentIdOrderByIdDesc(String documentId, Pageable pageable);
}
