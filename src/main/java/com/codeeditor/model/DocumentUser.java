package com.codeeditor.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUser {

    @EmbeddedId
    private DocumentUserId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("documentId")
    // @JoinColumn has no length attribute, so without columnDefinition Hibernate
    // maps this as varchar(255) and tries to widen the column on every startup —
    // which MySQL refuses while a foreign key references it.
    @JoinColumn(name = "document_id", columnDefinition = "varchar(36)")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;
}
