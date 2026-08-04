package com.dbwb.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * The whole point of this test is to fail loudly if the Spring context can't
 * wire up - a missing bean, a bad @ConfigurationProperties binding, or a
 * circular dependency would otherwise only surface the next time someone
 * happens to start the real server by hand.
 */
@SpringBootTest
class PlatformApplicationTests {

    @Test
    void contextLoads() {
    }
}
