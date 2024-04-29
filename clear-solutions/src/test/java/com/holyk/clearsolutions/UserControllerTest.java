package com.holyk.clearsolutions;

//@formatter:off
//import static org.junit.jupiter.api.Assertions.*;
//@formatter:on
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.holyk.clearsolutions.controllers.UserController;
import com.holyk.clearsolutions.controllers.UserData;
import com.holyk.clearsolutions.entity.User;
import com.holyk.clearsolutions.entity.UserRecord;
import com.holyk.clearsolutions.exceptions.DateRangeIsNotValidException;
import com.holyk.clearsolutions.exceptions.UserNotFoundException;
import com.holyk.clearsolutions.services.UserService;

@SpringBootTest(properties = {"app.user.minimum.age=18"})
class UserControllerTest {
	@Mock
	UserService service;
	@InjectMocks
	@Autowired
	UserController controller;

//	@BeforeAll
//	void setup() {
//		controller = new UserController(service);
//	}

	@BeforeEach
	void init() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testUserCreation() {
		UserRecord user = new UserRecord("mail", "firstname", "lastname", LocalDate.of(2002, 1, 1), "address", "phone");
		User user1 = User.of(user);
		user1.setId(1);

		when(service.save(Mockito.any(UserRecord.class))).thenReturn(user1);

		ResponseEntity<User> responseEntity = controller.createUser(new UserData(user));

		User user2 = responseEntity.getBody();
		assertEquals(user1, user2);
		assertEquals(1, user2.getId());
	}

	@Test
	void testUserCreationTooYoung() {
		UserRecord user = new UserRecord("mail", "firstname", "lastname", LocalDate.of(2012, 1, 1), "address", "phone");
		User user1 = User.of(user);
		user1.setId(1);

		when(service.save(Mockito.any(UserRecord.class))).thenReturn(user1);

		ResponseEntity<User> responseEntity = controller.createUser(new UserData(user));

		assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());

	}

	@Test
	void testUserDeleteFail() {

		when(service.delete(Mockito.anyLong())).thenReturn(false);

		ResponseEntity<?> response = controller.deleteUser(1L);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

	}

	@Test
	void testUserDeleteSuccess() {

		when(service.delete(Mockito.anyLong())).thenReturn(true);

		ResponseEntity<?> response = controller.deleteUser(1L);

		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

	}

	@Test
	void testUserUpdate() {
		UserRecord userRecord = new UserRecord("mail", "firstname", "lastname", LocalDate.of(2002, 1, 1), "address",
				"phone");
		User userBefore = User.of(userRecord);
		userBefore.setId(1);

		UserRecord userRecordToUpdate = new UserRecord("mail2", "firstname2", "lastname", LocalDate.of(2002, 1, 1),
				"address", "phone");
		User userAfter = User.of(userRecordToUpdate);
		userAfter.setId(1);

		when(service.update(Mockito.anyLong(), Mockito.any(UserRecord.class))).thenReturn(Optional.of(userAfter));

		ResponseEntity<User> responseEntity = controller.updateUser(1L, new UserData(userRecord));

		User user2 = responseEntity.getBody();
		assertNotEquals(userBefore, user2);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(userAfter, user2);
		assertEquals(1, user2.getId());
	}

	@Test
	void testUserUpdateFail() {

		UserRecord userRecord = new UserRecord("mail", "firstname", "lastname", LocalDate.of(2002, 1, 1), "address",
				"phone");

		when(service.update(Mockito.anyLong(), Mockito.any(UserRecord.class))).thenReturn(Optional.empty());

		ResponseEntity<User> responseEntity = controller.updateUser(1L, new UserData(userRecord));

		assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
	}

	@Test
	void testUserPatchSuccess() throws IOException, JsonPatchException {
		UserRecord userRecord = new UserRecord("mail", "firstname", "lastname", LocalDate.of(2002, 1, 1), "address",
				"phone");
		User userBefore = User.of(userRecord);
		userBefore.setId(1);

		ObjectMapper mapper = new ObjectMapper();
		//@formatter:off
		JsonNode json = mapper.readTree("[{\"op\": \"replace\","
				+ "\"path\": \"/1\","
				+ "\"value\": {"
					+ "\"email\": \"mail2\"}}]");
		//@formatter:on
		JsonPatch patch = JsonPatch.fromJson(json);

		User userAfter = User.of(userRecord);
		userAfter.setEmail("mail2");
		userAfter.setId(1);

		when(service.patch(Mockito.anyLong(), Mockito.any(JsonPatch.class))).thenReturn(userAfter);

		ResponseEntity<User> responseEntity = controller.patchUser(1L, patch);

		User user2 = responseEntity.getBody();
		assertNotEquals(userBefore, user2);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(userAfter, user2);
		assertEquals(1, user2.getId());
	}

	@Test
	void testUserPatchFail() throws JsonProcessingException, JsonPatchException {
		fail("need to be refactor or leave it to integration tests(mockmvc)");
		when(service.patch(Mockito.anyLong(), Mockito.any(JsonPatch.class))).thenThrow(UserNotFoundException.class);

		try {
			controller.patchUser(1L, null);
		} catch (UserNotFoundException e) {
			System.out.println(e);
			return;
		}

//		assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
	}

	@Test
	void testUserSelectSuccess() {
		List<User> list = new ArrayList<User>();

		UserRecord userRecord = new UserRecord("mail", "firstname", "lastname", LocalDate.of(2002, 1, 1), "address",
				"phone");
		UserRecord userRecord2 = new UserRecord("mail2", "firstname2", "lastname2", LocalDate.of(2012, 1, 1), "address",
				"phone");

		User user1 = User.of(userRecord);
		User user2 = User.of(userRecord2);
		user1.setId(1L);
		user2.setId(2L);
		list.add(user1);
		list.add(user2);

		when(service.getUsersByDateRange(Mockito.any(LocalDate.class), Mockito.any(LocalDate.class)))
				.thenReturn(List.of(user1));

		ResponseEntity<List<User>> response = controller.getUsersByBirthdateRange(LocalDate.of(2002, 1, 1),
				LocalDate.of(2002, 1, 1));
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(1, response.getBody().size());
		assertEquals(user1, response.getBody().get(0));

		when(service.getUsersByDateRange(Mockito.any(LocalDate.class), Mockito.any(LocalDate.class))).thenReturn(list);

		response = controller.getUsersByBirthdateRange(LocalDate.of(2002, 1, 1), LocalDate.of(2013, 1, 1));
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(2, response.getBody().size());
		assertEquals(user1, response.getBody().get(0));
		assertEquals(user2, response.getBody().get(1));

	}

	@Test
	void testUserSelectEmptySuccess() {

		ResponseEntity<List<User>> response = controller.getUsersByBirthdateRange(LocalDate.of(2002, 1, 1),
				LocalDate.of(2002, 1, 1));
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

	}

	@Test
	void testUserSelectFail() {
		assertThrows(DateRangeIsNotValidException.class,
				() -> controller.getUsersByBirthdateRange(LocalDate.of(2012, 1, 1), LocalDate.of(2002, 1, 1)));
	}

}
