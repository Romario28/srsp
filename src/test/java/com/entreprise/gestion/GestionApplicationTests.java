package com.entreprise.gestion;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// H2 en mémoire pour ce test (les autres tests n'utilisent pas de base)
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class GestionApplicationTests {

    @Test
    void contextLoads() {
    }

}
