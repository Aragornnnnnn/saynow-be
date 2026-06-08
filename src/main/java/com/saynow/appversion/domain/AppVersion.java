// 플랫폼별 앱 버전 정책을 저장하는 JPA 엔티티
package com.saynow.appversion.domain;

import com.saynow.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_versions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppVersion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppPlatform platform;

    @Column(name = "version_name", nullable = false, length = 30)
    private String versionName;

    @Column(name = "build_number", nullable = false)
    private long buildNumber;

    @Column(name = "minimum_supported_build_number", nullable = false)
    private long minimumSupportedBuildNumber;

    @Column(name = "force_update_reason", length = 500)
    private String forceUpdateReason;

    @Column(name = "soft_update_reason", length = 500)
    private String softUpdateReason;

    @Column(name = "release_note", columnDefinition = "text")
    private String releaseNote;

    @Column(name = "store_url", nullable = false, length = 500)
    private String storeUrl;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "released_at", nullable = false)
    private LocalDateTime releasedAt;
}
