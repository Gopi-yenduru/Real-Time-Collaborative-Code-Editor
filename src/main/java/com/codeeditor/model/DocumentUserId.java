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

    @Column(name = "document_id")
    private String documentId;

    @Column(name = "user_id")
    private Long userId;
}
