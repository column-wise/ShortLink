package io.github.columnwise.shortlink;

import io.github.columnwise.shortlink.config.TestRedisConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
class ShortLinkApplicationTests {

	@Test
	void contextLoads() {
	}

}
