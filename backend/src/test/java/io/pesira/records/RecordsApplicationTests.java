package io.pesira.records;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RecordsApplicationTests {

    @Test
    void contextLoads() {
    }

}
