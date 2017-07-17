package com.app;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import com.app.login.Application;
import com.app.login.domain.User;
import com.app.login.service.UserService;
import com.app.login.service.dto.UserDTO;
import com.app.login.web.rest.vm.LoginVM;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LoginApplicationIntegrationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private UserService userService;

	private String jwtTokenForTest;

	@Before
	public void setUp() {

		User user = userService.createUser("roger", "perfect", "roger", "federer", "roger@wimbledon.com", "", "en",
				Instant.now(), "127.0.0.1");

		LoginVM loginVM = new LoginVM();
		loginVM.setUsername("roger");
		loginVM.setPassword("perfect");
		loginVM.setRememberMe(true);

		Map<String, String> params = new HashMap();
		ResponseEntity<String> response = restTemplate.postForEntity("/api/authenticate", loginVM, String.class,
				params);
		jwtTokenForTest = response.getHeaders().get("Authorization").get(0);
	}

	@Test
	public void getUserAccount() {
		Map<String, String> params = new HashMap();
		ResponseEntity<UserDTO> responseEntity = restTemplate.getForEntity("/api/account", UserDTO.class, params);
		UserDTO user = responseEntity.getBody();
		assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", jwtTokenForTest);
		HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
		responseEntity = restTemplate.exchange("/api/account", HttpMethod.GET, entity, UserDTO.class);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals("roger", responseEntity.getBody().getFirstName());
	}
	
}
