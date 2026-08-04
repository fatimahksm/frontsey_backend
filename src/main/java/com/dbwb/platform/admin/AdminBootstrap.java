package com.dbwb.platform.admin;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.common.config.AdminBootstrapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the platform's first Super Admin account on startup, if
 * dbwb.admin.bootstrap-email/-password are configured and no Super Admin
 * exists yet. Intentionally a one-time bootstrap, not a general admin
 * account manager - subsequent admins are created by an existing Super
 * Admin (out of scope for the MVP admin console).
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminBootstrapProperties properties;

    public AdminBootstrap(AccountRepository accountRepository, PasswordEncoder passwordEncoder, AdminBootstrapProperties properties) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (properties.getBootstrapEmail() == null || properties.getBootstrapEmail().isBlank()
                || properties.getBootstrapPassword() == null || properties.getBootstrapPassword().isBlank()) {
            return;
        }

        if (accountRepository.existsByEmailIgnoreCase(properties.getBootstrapEmail())) {
            return;
        }

        Account admin = new Account();
        admin.setEmail(properties.getBootstrapEmail());
        admin.setPasswordHash(passwordEncoder.encode(properties.getBootstrapPassword()));
        admin.setFullName("Super Admin");
        admin.setRole(Role.SUPER_ADMIN);
        admin.setStatus(AccountStatus.ACTIVE);
        admin.setEmailVerified(true);
        accountRepository.save(admin);

        log.info("Bootstrapped Super Admin account for {}", properties.getBootstrapEmail());
    }
}
