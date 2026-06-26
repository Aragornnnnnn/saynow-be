// 앱 버전 정책 엔티티를 조회하는 Spring Data 저장소
package com.saynow.appversion.infrastructure;

import com.saynow.appversion.domain.AppPlatform;
import com.saynow.appversion.domain.AppVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {

    Optional<AppVersion> findFirstByPlatformAndActiveTrueOrderByBuildNumberDesc(AppPlatform platform);
}
