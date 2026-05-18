// 세션별 슬롯 충족 여부를 저장하는 엔티티
package com.saynow.session.domain;

import com.saynow.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "session_slot_statuses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionSlotStatus extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(name = "slot_name", nullable = false, length = 80)
    private String slotName;

    @Column(name = "is_fulfilled", nullable = false)
    private boolean fulfilled;

    public SessionSlotStatus(Session session, String slotName) {
        this.session = session;
        this.slotName = slotName;
        this.fulfilled = false;
    }

    public void fulfill() {
        this.fulfilled = true;
    }
}
