package com.dbwb.platform.auth;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountToken;
import com.dbwb.platform.account.entity.TokenType;
import com.dbwb.platform.account.repository.AccountTokenRepository;
import com.dbwb.platform.testsupport.TestEntities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRevokerTest {

    @Mock
    private AccountTokenRepository tokenRepository;

    @Test
    void marksEveryActiveTokenForTheAccountAsUsed() {
        UUID accountId = UUID.randomUUID();
        AccountToken tokenA = tokenFor(accountId);
        AccountToken tokenB = tokenFor(accountId);
        when(tokenRepository.findActiveByAccountIdAndType(accountId, TokenType.REFRESH))
                .thenReturn(List.of(tokenA, tokenB));

        new RefreshTokenRevoker(tokenRepository).revokeAllActive(accountId, TokenType.REFRESH);

        assertThat(tokenA.getUsedAt()).isNotNull();
        assertThat(tokenB.getUsedAt()).isNotNull();
    }

    private AccountToken tokenFor(UUID accountId) {
        Account account = TestEntities.withId(new Account(), accountId);
        AccountToken token = new AccountToken();
        token.setAccount(account);
        token.setType(TokenType.REFRESH);
        token.setToken(UUID.randomUUID().toString());
        return TestEntities.withId(token, UUID.randomUUID());
    }
}
