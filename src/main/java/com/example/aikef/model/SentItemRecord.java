package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.model.enums.SentItemType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sent_item_records")
@Getter
@Setter
public class SentItemRecord extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private SentItemType itemType;

    @Column(name = "item_value", nullable = false)
    private String itemValue;

    @Column(name = "amount")
    private String amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "sent_by", nullable = false)
    private SenderType sentBy;

    @Column(name = "note")
    private String note;
}
