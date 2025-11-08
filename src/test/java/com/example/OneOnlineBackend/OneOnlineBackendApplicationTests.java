package com.example.OneOnlineBackend;

import com.oneonline.backend.BackendApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = BackendApplication.class)
@ActiveProfiles("test")
class OneOnlineBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
