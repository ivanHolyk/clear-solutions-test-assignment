package com.holyk.clearsolutions;

//@formatter:off
//import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
//@formatter:on
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.RemoveOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import com.holyk.clearsolutions.controllers.UserController;
import com.holyk.clearsolutions.controllers.UserErrorResponse;
import com.holyk.clearsolutions.controllers.UserRequest;
import com.holyk.clearsolutions.controllers.UserResponse;
import com.holyk.clearsolutions.entity.User;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = ClearSolutionsTestAssignmentApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application.properties")
class IntergrationTest {
	@Autowired
	MockMvc mvc;
	@Autowired
	//@formatter:off
	/**
	 * create success 				*done
	 * create fail 					*done
	 * update success 				*done
	 * update fail					*done
	 * patch success				*done
	 * patch fail					*done
	 * delete success				*done
	 * delete fail					*done
	 * select by birthdate success
	 * select by birthdate fail
	 */
	//@formatter:on
	UserController controller;

	ObjectMapper om;
	User defaultUser = new User("mail@mail.com", "John", "Doe", LocalDate.of(2002, 1, 1), "Somewhere over the rainbow",
			"+36985214702");

	@Test
	void userCreationSuccess() throws Exception {
		User user = createUser();
		assertTrue(user.getId() > 0);

	}

	@ParameterizedTest
	@MethodSource("createNullPayloads")
	void userCreationNull(String payload) throws Exception {

		MockHttpServletResponse response = mvc
				.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isBadRequest()).andReturn().getResponse();

		HttpStatus result = HttpStatus.valueOf(response.getStatus());
		assertEquals(HttpStatus.BAD_REQUEST, result);

		ObjectMapper mapper = getMapper();

		UserErrorResponse error = stringJsonToObject(mapper, response.getContentAsString(), UserErrorResponse.class);

		assertEquals("One or more required fields are null!", error.message());
		assertEquals(HttpStatus.BAD_REQUEST.toString(), error.status());
	}

	@ParameterizedTest
	@MethodSource("createFailPayloads")
	void userCreationFail(String payload) throws Exception {

		MockHttpServletResponse response = mvc
				.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isBadRequest()).andReturn().getResponse();

		HttpStatus result = HttpStatus.valueOf(response.getStatus());
		assertEquals(HttpStatus.BAD_REQUEST, result);

		ObjectMapper mapper = getMapper();

		UserErrorResponse error = stringJsonToObject(mapper, response.getContentAsString(), UserErrorResponse.class);

		assertEquals("User is not valid!", error.message());
		assertEquals(HttpStatus.BAD_REQUEST.toString(), error.status());

	}

	private static Stream<Arguments> createFailPayloads() {

		String valid = "{ \"data\": {\"email\": \"mail@mail.com\"," + "\"firstname\": \"John\","
				+ "\"lastname\": \"Doe\",\n" + "\"birthdate\": \"2000-01-01\","
				+ "\"address\": \"Pro state, Pen Ins drive\",\n" + "\"phone\": \"+369852147\"" + "}}";

		String nonValidEmail = "{ \"data\": {\"email\": \"mail_mail.com\"," + "\"firstname\": \"John\","
				+ "\"lastname\": \"Doe\",\n" + "\"birthdate\": \"2000-01-01\","
				+ "\"address\": \"Pro state, Pen Ins drive\",\n" + "\"phone\": \"+369852147\"" + "}}";

		String nonValidBirthdate = "{ \"data\": {\"email\": \"mail@mail.com\"," + "\"firstname\": \"John\","
				+ "\"lastname\": \"Doe\",\n" + "\"birthdate\": \"2222-01-01\","
				+ "\"address\": \"Pro state, Pen Ins drive\",\n" + "\"phone\": \"+369852147\"" + "}}";

		return Stream.of(Arguments.of(nonValidEmail), Arguments.of(nonValidBirthdate));
	}

	private static Stream<Arguments> createNullPayloads() {

		String valid = "{ \"data\": {\"email\": \"mail@mail.com\"," + "\"firstname\": \"John\","
				+ "\"lastname\": \"Doe\",\n" + "\"birthdate\": \"2000-01-01\","
				+ "\"address\": \"Pro state, Pen Ins drive\",\n" + "\"phone\": \"+369852147\"" + "}}";

		String emptyEmail = "{ \"data\": {\"email\": \"\"," + "\"firstname\": \"John\"," + "\"lastname\": \"Doe\",\n"
				+ "\"birthdate\": \"2000-01-01\"," + "\"address\": \"Pro state, Pen Ins drive\",\n"
				+ "\"phone\": \"+369852147\"" + "}}";
		String emptyFirstname = "{ \"data\": {\"email\": \"mail@mail.com\"," + "\"firstname\": \"\","
				+ "\"lastname\": \"Doe\",\n" + "\"birthdate\": \"2000-01-01\","
				+ "\"address\": \"Pro state, Pen Ins drive\",\n" + "\"phone\": \"+369852147\"" + "}}";

		String emptyLastname = "{ \"data\": {\"email\": \"mail@mail.com\"," + "\"firstname\": \"John\","
				+ "\"lastname\": \"\",\n" + "\"birthdate\": \"2000-01-01\","
				+ "\"address\": \"Pro state, Pen Ins drive\",\n" + "\"phone\": \"+369852147\"" + "}}";
		String emptyBirthdate = "{ \"data\": {\"email\": \"mail@mail.com\"," + "\"firstname\": \"John\","
				+ "\"lastname\": \"Doe\",\n" + "\"birthdate\":\"\"," + "\"address\": \"Pro state, Pen Ins drive\",\n"
				+ "\"phone\": \"+369852147\"" + "}}";

		return Stream.of(Arguments.of(emptyEmail), Arguments.of(emptyFirstname), Arguments.of(emptyLastname),
				Arguments.of(emptyBirthdate));
	}

	@Test
	void userCreationForbidden() throws Exception {

		MockHttpServletResponse response = mvc
				.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
						.content("{ \"data\": {\"email\": \"mail@mail.com\"," + "\"firstname\": \"firstname\","
								+ "\"lastname\": \"lastname\",\n" + "\"birthdate\": \"2012-01-01\","
								+ "\"address\": \"Pro state, Pen Ins drive\",\n" + "\"phone\": \"+369852147\"" + "}}"))
				.andExpect(status().isForbidden()).andReturn().getResponse();

		HttpStatus result = HttpStatus.valueOf(response.getStatus());
		assertEquals(HttpStatus.FORBIDDEN, result);

		ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();

		UserErrorResponse error = stringJsonToObject(mapper, response.getContentAsString(), UserErrorResponse.class);

		assertEquals("Too young!", error.message());
		assertEquals(HttpStatus.FORBIDDEN.toString(), error.status());
	}

	@Test
	void userUpdateSuccess() throws Exception {
		User user = createUser();

		ObjectMapper mapper = getMapper();

		user.setEmail("newEmail@mail.com");

		UserRequest data = UserRequest.of(user);
		String payload = mapper.valueToTree(data).toString();

		MockHttpServletResponse response = mvc
				.perform(put("/users/{id}", user.getId()).contentType(MediaType.APPLICATION_JSON).content(payload))
				.andReturn().getResponse();

		String updatedUserJsonData = response.getContentAsString();
		UserResponse updatedUser = stringJsonToObject(mapper, updatedUserJsonData, UserResponse.class);

		assertEquals(user, User.of(updatedUser));

	}

	@Test
	void userUpdateFail() throws Exception {
		User user = createUser();

		ObjectMapper mapper = getMapper();

		UserRequest data = UserRequest.of(user);
		String payload = mapper.valueToTree(data).toString();

		MockHttpServletResponse response = mvc
				.perform(put("/users/{id}", -1L).contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isNotFound()).andReturn().getResponse();

		assertEquals(HttpStatus.NOT_FOUND, HttpStatus.valueOf(response.getStatus()));
		assertEquals("User with id -1 not found!",
				stringJsonToObject(mapper, response.getContentAsString(), UserErrorResponse.class).message());

	}

	private ObjectMapper getMapper() {
		if (this.om == null) {
			this.om = new ObjectMapper();
			om.findAndRegisterModules();
			om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		}
		return om;
	}

	@ParameterizedTest
	@MethodSource("patchOperationsSuccess")
	void userPatchSuccess(JsonPatchOperation op) throws Exception {
		User user = createUser();

		ObjectMapper mapper = getMapper();

		JsonNode opTree = mapper.valueToTree(new JsonPatch(List.of(op)));
		String payload = opTree.toString();

		MockHttpServletResponse response = mvc
				.perform(patch("/users/{id}", user.getId()).contentType("application/json-patch+json").content(payload))
				.andExpect(status().isOk()).andReturn().getResponse();

		String updatedUserJsonData = response.getContentAsString();
		UserResponse updatedUser = stringJsonToObject(mapper, updatedUserJsonData, UserResponse.class);

//		assertEquals("Lostname", updatedUser.data().lastname());

	}

	@ParameterizedTest
	@MethodSource("patchOperationsFail")
	void userPatchFail(JsonPatchOperation op) throws Exception {
		User user = createUser();

		ObjectMapper mapper = getMapper();

		var payload = mapper.valueToTree(new JsonPatch(List.of(op))).toString();

		MockHttpServletResponse response = mvc
				.perform(patch("/users/{id}", user.getId()).contentType("application/json-patch+json").content(payload))
				.andExpect(status().isBadRequest()).andReturn().getResponse();

		String updatedUserJsonData = response.getContentAsString();
		UserErrorResponse errorRepsonse = stringJsonToObject(mapper, updatedUserJsonData, UserErrorResponse.class);

		assertEquals(HttpStatus.BAD_REQUEST.toString(), errorRepsonse.status());
		assertEquals("Patch is not valid!", errorRepsonse.message());

	}

	@Test
	void userPatchNotFound() throws Exception {

		ObjectMapper mapper = getMapper();

		var payload = mapper.valueToTree(new JsonPatch(List.of(new RemoveOperation(JsonPointer.of("lastname")))))
				.toString();

		MockHttpServletResponse response = mvc
				.perform(patch("/users/{id}", -1L).contentType("application/json-patch+json").content(payload))
				.andExpect(status().isNotFound()).andReturn().getResponse();

		String updatedUserJsonData = response.getContentAsString();
		UserErrorResponse errorRepsonse = stringJsonToObject(mapper, updatedUserJsonData, UserErrorResponse.class);

		assertEquals(HttpStatus.NOT_FOUND.toString(), errorRepsonse.status());
		assertEquals("User not found!", errorRepsonse.message());

	}

	@Test
	void userDeleteSuccess() throws Exception {
		User user = createUser();

		mvc.perform(delete("/users/{id}", user.getId()).contentType("application/json").content(""))
				.andExpect(status().isNoContent());
	}

	@Test
	void userDeleteFail() throws Exception {

		mvc.perform(delete("/users/{id}", -1L).contentType("application/json").content(""))
				.andExpect(status().isNotFound());
	}

	@ParameterizedTest
	@MethodSource("selectByBirthdatesSuccess")
	void selectUsersByDateSuccess(LocalDate[] birthdates, LocalDate[] expectedBirthdates,
			ResultMatcher expectedStatusCode, LocalDate from, LocalDate to)
			throws UnsupportedEncodingException, Exception {

		for (LocalDate birthdate : birthdates) {
			createUser(birthdate);
		}
		ObjectMapper mapper = getMapper();
		List<LocalDate> list = mapper
				.readValue(
						mvc.perform(get("/users?fromDate={fromDate}&toDate={toDate}", from, to))
								.andExpect(expectedStatusCode).andReturn().getResponse().getContentAsString(),
						new TypeReference<List<User>>() {
						})
				.stream().map(u -> u.getBirthdate()).toList();

		assertTrue(list.containsAll(Arrays.asList(expectedBirthdates)));

	}

	@Test
	void selectUsersByDateNoContent() throws Exception {
		mvc.perform(get("/users?fromDate={fromDate}&toDate={toDate}", LocalDate.of(1900, 10, 10),
				LocalDate.of(1901, 10, 10))).andExpect(status().isNoContent());

	}

	@Test
	void selectUsersByDateFail() throws Exception {
		mvc.perform(get("/users?fromDate={fromDate}&toDate={toDate}", 
				LocalDate.of(1991, 10, 10),LocalDate.of(1990, 10, 10)))
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("$.message").value("Range of dates is not valid!"))
		.andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.toString()));
		
		
		
		mvc.perform(get("/users?fromDate={fromDate}&toDate={toDate}", 
				null,null))
		.andExpect(status().isBadRequest());
//		
//		.andExpect(jsonPath("$.message").value("Range of dates is not presented!"))
//		.andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.toString()));
		
		
		

	}

	private static Stream<Arguments> selectByBirthdatesSuccess() throws UnsupportedEncodingException, Exception {
		//@formatter:off
		return Stream.of(
				Arguments.of(
						Arrays.array(LocalDate.of(2004, 1, 1), LocalDate.of(1995, 1, 1)),
						Arrays.array(LocalDate.of(2004, 1, 1)), 
						status().isOk(), 
						LocalDate.of(2000, 1, 1),LocalDate.of(2010, 1, 1)),
				
				Arguments.of(
						Arrays.array(LocalDate.of(2004, 1, 1), LocalDate.of(1995, 1, 1), LocalDate.of(1950, 1, 1)),
						Arrays.array(LocalDate.of(2004, 1, 1),LocalDate.of(1995, 1, 1)),
						status().isOk(),
						LocalDate.of(1989, 1, 1), LocalDate.of(2010, 1, 1))
				
//				Arguments.of(
//						Arrays.array(LocalDate.of(2004, 1, 1), LocalDate.of(1995, 1, 1), LocalDate.of(1950, 1, 1)),
//						Arrays.array(LocalDate.of(2004, 1, 1), LocalDate.of(1995, 1, 1)),
//						status().isNoContent(),
//						LocalDate.of(1999, 1, 1), LocalDate.of(2000, 1, 1))
				

		);
		//@formatter:on
	}

	private static Stream<Arguments> patchOperationsFail() {
		/**
		 * important fields are email, first and last names, birthdate. not important
		 * are address and phone
		 */
		JsonPatchOperation replaceBirthdateOOB = new ReplaceOperation(JsonPointer.of("birthdate"),
				new TextNode(LocalDate.now().plus(Period.ofYears(1)).toString()));
		JsonPatchOperation replaceBirthdateYoung = new ReplaceOperation(JsonPointer.of("birthdate"),
				new TextNode(LocalDate.now().minus(Period.ofYears(1)).toString()));
		JsonPatchOperation replaceBirthdateNull = new ReplaceOperation(JsonPointer.of("birthdate"),
				MissingNode.getInstance());
		JsonPatchOperation removeImportant = new RemoveOperation(JsonPointer.of("lastname"));
		JsonPatchOperation replaceFirstnameEmpty = new ReplaceOperation(JsonPointer.of("firstname"), new TextNode(""));
		JsonPatchOperation replaceEmailNull = new ReplaceOperation(JsonPointer.of("email"), MissingNode.getInstance());
		JsonPatchOperation replaceEmailNotValid = new ReplaceOperation(JsonPointer.of("email"),
				new TextNode("dfdferdfkljk"));

		return Stream.of(Arguments.of(replaceBirthdateOOB), Arguments.of(removeImportant),
				Arguments.of(replaceFirstnameEmpty), Arguments.of(replaceEmailNull), Arguments.of(replaceEmailNotValid),
				Arguments.of(replaceBirthdateNull), Arguments.of(replaceBirthdateYoung));
	}

	private static Stream<Arguments> patchOperationsSuccess() {
		/**
		 * important fields are email, first and last names, birthdate. not important
		 * are address and phone
		 */
		JsonPatchOperation replaceBirthdate = new ReplaceOperation(JsonPointer.of("birthdate"),
				new TextNode(LocalDate.now().minus(Period.ofYears(20)).toString()));
		JsonPatchOperation addNonImportant = new AddOperation(JsonPointer.of("address"),
				TextNode.valueOf("new address"));
		JsonPatchOperation removeNonImportnant = new RemoveOperation(JsonPointer.of("phone"));
		JsonPatchOperation replaceEmailValid = new ReplaceOperation(JsonPointer.of("email"),
				new TextNode("cool@mail.com"));

		return Stream.of(Arguments.of(replaceBirthdate), Arguments.of(removeNonImportnant),
				Arguments.of(addNonImportant), Arguments.of(replaceEmailValid));
	}

	private User createUser() throws UnsupportedEncodingException, Exception {
		return createUser(LocalDate.of(2002, 1, 1));
	}

	private User createUser(LocalDate birthdate) throws UnsupportedEncodingException, Exception {
		String result = mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
				.content("{ \"data\": {\"email\": \"" + defaultUser.getEmail() + "\"," + "\"firstname\": \""
						+ defaultUser.getFirstname() + "\"," + "\"lastname\": \"" + defaultUser.getLastname() + "\",\n"
						+ "\"birthdate\": \"" + birthdate.toString() + "\"," + "\"address\": \""
						+ defaultUser.getAddress() + "\",\n" + "\"phone\": \"" + defaultUser.getPhone() + "\"" + "}}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

		ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();
		JsonNode tree = mapper.readTree(result);
		return mapper.treeToValue(tree, User.class);
	}

	private <T> T stringJsonToObject(ObjectMapper mapper, String json, Class<T> clazz)
			throws JsonMappingException, JsonProcessingException, IllegalArgumentException {
		return mapper.treeToValue(mapper.readTree(json), clazz);

	}
}
