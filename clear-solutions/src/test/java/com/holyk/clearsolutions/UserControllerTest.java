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
import com.holyk.clearsolutions.controllers.UserRequest;
import com.holyk.clearsolutions.controllers.UserResponse;
import com.holyk.clearsolutions.entity.User;
import com.holyk.clearsolutions.exceptions.DateRangeIsNotValidException;
import com.holyk.clearsolutions.exceptions.UserNotFoundException;
import com.holyk.clearsolutions.exceptions.UserNotValidException;
import com.holyk.clearsolutions.services.UserService;

@SpringBootTest(properties = { "app.user.minimum.age=18" })
class UserControllerTest {
	@Mock
	UserService service;
	@InjectMocks
	@Autowired
	UserController controller;

	@BeforeEach
	void init() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testUserCreation() {
		UserRequest request = UserRequest.of("mail@mail.com", "firstname", "lastname", LocalDate.of(2002, 1, 1),
				"address", "phone");
		User user = User.of(request);
		user.setId(1);

		when(service.save(Mockito.any(UserRequest.class))).thenReturn(user);

		ResponseEntity<User> responseEntity = controller.createUser(request);

		User user2 = responseEntity.getBody();
		assertEquals(user, user2);
		assertEquals(1, user2.getId());
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
		UserRequest baseRequest = UserRequest.of("mail@mail.com", "firstname", "lastname", LocalDate.of(2002, 1, 1),
				"address", "phone");
		User userBefore = User.of(baseRequest);
		userBefore.setId(1);

		UserRequest payload = UserRequest.of("new_mail@mail.com", "firstname2", "lastname", LocalDate.of(2002, 1, 1),
				"address", "phone");
		User userAfter = User.of(payload);
		userAfter.setId(1);

		when(service.update(Mockito.anyLong(), Mockito.any(UserRequest.class))).thenReturn((userAfter));

		ResponseEntity<UserResponse> responseEntity = controller.updateUser(1L, payload);

		User user2 = User.of(responseEntity.getBody());
		assertNotEquals(userBefore, user2);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(userAfter, user2);
		assertEquals(1, user2.getId());
	}

	@Test
	void testUserUpdateFail() {

		UserRequest request = UserRequest.of("mail", "firstname", "lastname", LocalDate.of(2002, 1, 1), "address",
				"phone");

		assertThrows(UserNotValidException.class, () -> controller.updateUser(1L, request));

	}

	@Test
	void testUserPatchSuccess() throws IOException, JsonPatchException, NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException {
		UserRequest request = UserRequest.of("mail@mail.com", "firstname", "lastname", LocalDate.of(2002, 1, 1), "address",
				"phone");
		User userBefore = User.of(request);
		userBefore.setId(1);

		ObjectMapper mapper = new ObjectMapper();
		//@formatter:off
		JsonNode json = mapper.readTree("[{\"op\": \"replace\","
				+ "\"path\": \"/email\","
				+ "\"value\": \"new_mail@mail.com\"}]");
		//@formatter:on
		JsonPatch patch = JsonPatch.fromJson(json);

		User userAfter = User.of(request);
		userAfter.setEmail("mail2");
		userAfter.setId(1);
		
		when(service.findUserById(Mockito.anyLong())).thenReturn(Optional.of(userBefore));
		when(service.patch(Mockito.anyLong(), Mockito.any(JsonPatch.class))).thenReturn(userAfter);

		ResponseEntity<UserResponse> responseEntity = controller.patchUser(1L, patch);

		User user2 = User.of(responseEntity.getBody());
		assertNotEquals(userBefore, user2);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(userAfter, user2);
		assertEquals(1, user2.getId());
	}

	@Test
	void testUserSelectSuccess() {
		List<User> list = new ArrayList<User>();

		UserRequest request1 = UserRequest.of("mail", "firstname", "lastname", LocalDate.of(2002, 1, 1), "address",
				"phone");
		UserRequest request2 = UserRequest.of("mail2", "firstname2", "lastname2", LocalDate.of(2012, 1, 1), "address",
				"phone");

		User user1 = User.of(request1);
		User user2 = User.of(request2);
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
