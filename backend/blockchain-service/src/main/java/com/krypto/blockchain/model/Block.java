package com.krypto.blockchain.model;

import com.krypto.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "blocks")
public class Block extends BaseEntity {

    @Column(name = "block_index", nullable = false, unique = true)
    private long index;

    @Column(nullable = false, length = 128)
    private String hash;

    @Column(nullable = false, length = 128)
    private String previousHash;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private long nonce;

    @Builder.Default
    @OneToMany(mappedBy = "block", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @jakarta.persistence.OrderBy("positionInBlock ASC")
    private List<ChainTransaction> transactions = new ArrayList<>();
}
