package com.app;

import static org.junit.Assert.*;

import java.time.Instant;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.app.login.Application;
import com.app.login.domain.User;
import com.app.login.service.UserService;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class UserServiceTest {

	@Autowired
	UserService userService;

	@Test
	public void createUserTest() {
		User user = new User();
		user.setFirstName("Bruce");
		user.setLastName("Wayne");
		user.setPassword("batman");
		user.setCreatedDate(Instant.now());
		user.setEmail("bruce@dccomics.com");
		user.setIpAddress("127.0.0.1");

		User newUser = userService.createUser(user.getFirstName(), user.getPassword(), user.getFirstName(), user.getLastName(),
				user.getEmail(), "", "en", user.getCreatedDate(), user.getIpAddress());
		assertNotNull(newUser);
	}
}
