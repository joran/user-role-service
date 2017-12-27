package com.example.userroleservice;

import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
				.expectBodyList(User.class).hasSize(3);
	}

	@Test
	public void test_with_jsonPath_get_by_userId() {
		webTestClient.get().uri("/api/user/user1")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.userId").exists()
				.jsonPath("$.userId").isNotEmpty()
				.jsonPath("$.userId").isEqualTo("user1")
				.jsonPath("$.name").exists()
				.jsonPath("$.name").isNotEmpty()
				.jsonPath("$.name").isEqualTo("Karl Benknäckare")
				.jsonPath("$.roles").exists()
				.jsonPath("$.roles").isNotEmpty()
				.jsonPath("$.roles").isArray()
				.jsonPath("$.roles[0].id").isEqualTo("1-1-1-1-1");

	}

	@Test
	public void test_by_example_get_by_userId() {
		ArrayList<Role> roles = new ArrayList<>();
		roles.add(Role.builder().id("1-1-1-1-1").rolename("R1").description("Beskrivning av roll R1").build());
		User user = User.builder().userId("user1").name("Karl Benknäckare").roles(roles).build();

		webTestClient.get().uri("/api/user/user1")
				.exchange()
				.expectStatus().isOk()
				.expectBody(User.class)
				.isEqualTo(user);
	}

	@Test
	public void test_not_found_for_non_existing_user() {
		webTestClient.get().uri("/api/user/nonexistinguser")
				.exchange()
				.expectStatus().isNotFound();
	}

	@Test
	public void test_crud_user() {
		webTestClient.get().uri("/api/user/x")
				.exchange()
				.expectStatus().isNotFound();

		Role role = Role.builder().id("1-1-1-1-1").build();
		User user = User.builder().userId("x").name("Testanvändare").roles(Arrays.asList(role)).build();

		webTestClient.post().uri("/api/user")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(user), User.class)
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
				.expectBody()
				.jsonPath("$.userId").isEqualTo(user.getUserId())
				.jsonPath("$.name").isEqualTo(user.getName())
				.jsonPath("$.roles[0].id").isEqualTo(role.getId());

		webTestClient.get().uri("/api/user/x")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.userId").isEqualTo(user.getUserId())
				.jsonPath("$.name").isEqualTo(user.getName())
				.jsonPath("$.roles[0].id").isEqualTo(role.getId());

		User user2 = user.toBuilder().name("Testanvändare2").build();
		webTestClient.put().uri("/api/user/x")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(user2), User.class)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.userId").isEqualTo(user2.getUserId())
				.jsonPath("$.name").isEqualTo(user2.getName())
				.jsonPath("$.roles[0].id").isEqualTo(role.getId());

		webTestClient.get().uri("/api/user/x")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.userId").isEqualTo(user2.getUserId())
				.jsonPath("$.name").isEqualTo(user2.getName())
				.jsonPath("$.roles[0].id").isEqualTo(role.getId());

		webTestClient.delete().uri("/api/user/x")
				.exchange()
				.expectStatus().isOk();

		webTestClient.get().uri("/api/user/x")
				.exchange()
				.expectStatus().isNotFound();
	}

	@Test
	public void test_crud_role() {
		Role role = Role.builder().rolename("x").description("Testroll x").build();

		Role newRole = webTestClient.post().uri("/api/role")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(role), Role.class)
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
				.expectBody(Role.class).returnResult().getResponseBody();

		assertEquals(role.getRolename(), newRole.getRolename());
		assertEquals(role.getDescription(), newRole.getDescription());
		assertNotNull(newRole.getId());

		String uid = newRole.getId();
		webTestClient.get().uri("/api/role/" + uid)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.id").isEqualTo(uid)
				.jsonPath("$.rolename").isEqualTo(role.getRolename())
				.jsonPath("$.description").isEqualTo(role.getDescription());

		Role role2 = newRole.toBuilder().description("Testroll2").build();
		webTestClient.put().uri("/api/role/" + uid)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(role2), Role.class)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.id").isEqualTo(uid)
				.jsonPath("$.rolename").isEqualTo(role2.getRolename())
				.jsonPath("$.description").isEqualTo(role2.getDescription());

		webTestClient.get().uri("/api/role/" + uid)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.id").isEqualTo(uid)
				.jsonPath("$.rolename").isEqualTo(role2.getRolename())
				.jsonPath("$.description").isEqualTo(role2.getDescription());

		webTestClient.delete().uri("/api/role/" + uid)
				.exchange()
				.expectStatus().isOk();

		webTestClient.get().uri("/api/role/" + uid)
				.exchange()
				.expectStatus().isNotFound();
	}

	@Test
	public void test_list_all_roles() {
		webTestClient.get().uri("/api/role")
				.exchange()
				.expectStatus().isOk()
				.expectBodyList(Role.class).hasSize(3)
				.consumeWith(result -> {
					List<Role> roles = result.getResponseBody();
					Role R1 = roles.stream().filter(r -> "R1".equals(r.getRolename())).findFirst().orElse(null);
					assertNotNull(R1);
				});
	}

	@Test
	public void test_existing_role_with_findRoleById() {
		Role role = webTestClient.get().uri("/api/role/1-1-1-1-1")
				.exchange()
				.expectStatus().isOk()
				.expectBody(Role.class).returnResult().getResponseBody();

		assertNotNull(role);
		assertEquals("1-1-1-1-1", role.getId());
		assertEquals("R1", role.getRolename());
	}

	@Test
	public void test_not_found_for_non_existing_role() {
		webTestClient.get().uri("/api/role/nonexistinguser")
				.exchange()
				.expectStatus().isNotFound();
	}

	@Test
	public void test_role_should_be_removed_from_user_when_deleted() {
		Role role = Role.builder().rolename("x2").description("Testroll x").build();

		Role newRole = webTestClient.post().uri("/api/role")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(role), Role.class)
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
				.expectBody(Role.class).returnResult().getResponseBody();

		String uid = newRole.getId();
		User user = User.builder().userId("x2").name("Testanvändare").roles(Arrays.asList(newRole)).build();

		webTestClient.post().uri("/api/user")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(user), User.class)
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
				.expectBody()
				.jsonPath("$.userId").isEqualTo(user.getUserId())
				.jsonPath("$.name").isEqualTo(user.getName())
				.jsonPath("$.roles[0].id").isEqualTo(uid);

		webTestClient.delete().uri("/api/role/" + uid)
				.exchange()
				.expectStatus().isOk();

		webTestClient.get().uri("/api/user/" + user.getUserId())
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.userId").isEqualTo(user.getUserId())
				.jsonPath("$.name").isEqualTo(user.getName())
				.jsonPath("$.roles").isEmpty();

		webTestClient.delete().uri("/api/user/" + user.getUserId())
				.exchange()
				.expectStatus().isOk();

	}
}
