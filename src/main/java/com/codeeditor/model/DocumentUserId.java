package com.codeeditor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUserId implements Serializable {

    // Length must match Document.id (36). Note that under @MapsId it is the
    // association's @JoinColumn in DocumentUser that actually defines the
    // column, so the width is declared there too.
    @Column(name = "document_id", length = 36)
    private String documentId;

    @Column(name = "user_id")
    private Long userId;
}
