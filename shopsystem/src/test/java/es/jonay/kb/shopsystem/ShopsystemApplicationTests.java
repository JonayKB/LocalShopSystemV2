package es.jonay.kb.shopsystem;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@org.springframework.context.annotation.Import(TestDatabase.class)
class ShopsystemApplicationTests {

	@Test
	void contextLoads() {
	}

}
