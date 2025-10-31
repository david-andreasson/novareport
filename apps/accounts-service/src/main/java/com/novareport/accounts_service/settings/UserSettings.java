package com.novareport.accounts_service.settings;

import com.novareport.accounts_service.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_settings")
public class UserSettings {
    @Id
    private UUID userId;

    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    private String locale = "sv-SE";
    @Builder.Default
    private String timezone = "Europe/Stockholm";
    @Builder.Default
    private Boolean marketingOptIn = false;
    @Builder.Default
    private Boolean twoFactorEnabled = false;
}
