package com.yanapure.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "twilio.account-sid=",
        "twilio.auth-token=",
        "twilio.phone-number="
})
class YanaBackendApplicationTests {

    // @Test
    // void contextLoads() {
    //     // This test verifies that the Spring context loads successfully
    // }

}
