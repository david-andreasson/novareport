package com.novareport.accounts_service.settings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserSettingsRepository extends JpaRepository<UserSettings, UUID> {
    List<UserSettings> findByReportEmailOptInTrue();
}
