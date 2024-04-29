package com.holyk.clearsolutions;

//@formatter:off
//import static org.junit.jupiter.api.Assertions.*;
//@formatter:on
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.holyk.clearsolutions.entity.User;
import com.holyk.clearsolutions.entity.UserRecord;
import com.holyk.clearsolutions.exceptions.UserNotFoundException;
import com.holyk.clearsolutions.services.UserService;

class UserServiceTest {

	@Test
	void testUserCreate() {
		UserService service = new UserService();
		UserRecord userR = new UserRecord("mail", "firstname", "lastname", LocalDate.of(2002, 1, 1), "address",
				"phone");

		User user = service.save(userR);

		assertEquals(1, service.getList().size());
		assertTrue(user.getId() > 0);
		assertEquals(service.getIncrement() - 1, user.getId());

	}

	@Test
	void testUserDelete() {
		UserService service = new UserService();
		UserRecord userR = new UserRecord("mail", "firstname", "lastname", LocalDate.of(2002, 1, 1), "address",
				"phone");

		User user = service.save(userR);
		service.save(userR);
		assertEquals(2, service.getList().size());
		assertTrue(service.delete(user.getId()));
		assertEquals(1, service.getList().size());

		assertFalse(service.delete(user.getId()));

	}

	@Test
	void testGetUsersByDateRange() {
		UserRecord userR = new UserRecord("mail", "firstname", "lastname", LocalDate.of(2002, 1, 1), "address",
				"phone");
		User user = User.of(userR);
		User user2 = User.of(userR);
		user2.setBirthdate(LocalDate.of(2004, 1, 2));

		UserService service = new UserService(List.of(user, user2), 3L);

		List<User> result = service.getUsersByDateRange(LocalDate.of(2001, 1, 1), LocalDate.of(2003, 1, 1));
		assertEquals(1, result.size());

		result = service.getUsersByDateRange(LocalDate.of(2001, 1, 1), LocalDate.of(2005, 1, 1));
		assertEquals(2, result.size());

		result = service.getUsersByDateRange(LocalDate.of(2005, 1, 1), LocalDate.of(2006, 1, 1));
		assertTrue(result.isEmpty());

		result = service.getUsersByDateRange(LocalDate.of(2004, 1, 2), LocalDate.of(2004, 1, 2));
		assertEquals(1, result.size());

		result = service.getUsersByDateRange(LocalDate.of(2004, 1, 1), LocalDate.of(2004, 1, 2));
		assertEquals(1, result.size());

		result = service.getUsersByDateRange(LocalDate.of(2004, 1, 2), LocalDate.of(2004, 1, 3));
		assertEquals(1, result.size());

		result = service.getUsersByDateRange(LocalDate.of(2005, 1, 1), LocalDate.of(2001, 1, 1));
		assertEquals(2, result.size());

	}

	@Test
	void testUserUpdate() {
		UserService service = new UserService();
		UserRecord userR = new UserRecord("mail", "firstname", "lastname", LocalDate.of(2002, 1, 1), "address",
				"phone");

		User user = service.save(userR);
		service.save(userR);
		user.setFirstname("Other");
		user.setLastname("Lost name");

		long id = user.getId();
		UserRecord userToUpdate = new UserRecord("mail", "Other", "Lost name", LocalDate.of(2002, 1, 1), "address",
				"phone");

		User user2 = service.update(id, userToUpdate);

		assertEquals(user, user2);
		assertEquals(2, service.getList().size());

	}

	@Test
	void testUserPatch() throws IOException, JsonPatchException {
		UserService service = new UserService();
		UserRecord userR = new UserRecord("mail", "firstname", "lastname", LocalDate.of(2002, 1, 1), "address",
				"phone");

		User user = service.save(userR);
		service.save(userR);

		long id = user.getId();
		user.setFirstname("Other");
		user.setLastname("Lost name");

		ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();

		//@formatter:off
		JsonNode json = mapper.readTree("[{\"op\": \"replace\","
				+ "\"path\": \"/email\","
				+ "\"value\": \"mail2\"}]");
		//@formatter:on
		JsonPatch patch = JsonPatch.fromJson(json);
		User user2 = service.patch(id, patch);

		assertEquals("mail2", user2.getEmail());
		assertEquals(2, service.getList().size());

	}

	@Test
	void testUserPatchFail() throws JsonPatchException, IOException {
		UserService service = new UserService();

		ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();

		//@formatter:off
		JsonNode json = mapper.readTree("[{\"op\": \"replace\","
				+ "\"path\": \"/email\","
				+ "\"value\": \"mail2\"}]");
		//@formatter:on
		JsonPatch patch = JsonPatch.fromJson(json);
		assertThrows(UserNotFoundException.class, () -> service.patch(1L, patch));

	}

}
