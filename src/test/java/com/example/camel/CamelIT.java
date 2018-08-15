package com.example.camel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CamelIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void sayHello() {
        ResponseEntity<String> response = restTemplate.getForEntity("/camel/say/hello", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String s = response.getBody();
        assertThat(s).contains("Hello");
    }

}
