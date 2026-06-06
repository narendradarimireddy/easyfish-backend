package com.easyfish.backend3.repository;

import com.easyfish.backend3.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {
    Optional<AppSetting> findBySettingKey(String settingKey);
}
