package com.example.userroleservice;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserRoleServiceApplicationTests {
	@Autowired
	private WebTestClient webTestClient;

	@Test
	public void test_get_all_users() {
		webTestClient.get().uri("/api/user")
				.exchange()
				.expectStatus().isOk()
				.expectBodyList(User.class)
				.hasSize(3);
	}

	@Test
	public void test_get_user() {
		User user = webTestClient.get().uri("/api/user/user1")
				.exchange()
				.expectStatus().isOk()
				.expectBody(User.class).returnResult().getResponseBody();

		Assert.assertEquals("user1", user.getUserId());
//				.isEqualTo(User.builder().userId("user").name("").build());

	}

}
