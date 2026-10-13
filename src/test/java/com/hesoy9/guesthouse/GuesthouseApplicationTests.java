package com.hesoy9.guesthouse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Starts the whole application on the in-memory "local-h2" database, so this
// check works on any machine without MySQL or real secrets.
@SpringBootTest
@ActiveProfiles("local-h2")
class GuesthouseApplicationTests {

	@Test
	void contextLoads() {
	}

}
