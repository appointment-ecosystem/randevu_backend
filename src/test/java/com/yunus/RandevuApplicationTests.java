package com.yunus;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Spring context'in entity ve repository bean'leriyle ayağa kalktığını doğrular. */
@SpringBootTest
@ActiveProfiles("test")
class RandevuApplicationTests {

    @Test
    void contextLoads() {
    }

}
