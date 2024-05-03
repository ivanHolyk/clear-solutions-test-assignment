package com.holyk.clearsolutions.controllers;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.holyk.clearsolutions.entity.User;
import com.holyk.clearsolutions.exceptions.DateRangeIsNotValidException;
import com.holyk.clearsolutions.exceptions.UserAgeNotSatisfyException;
import com.holyk.clearsolutions.exceptions.UserNotFoundException;
import com.holyk.clearsolutions.exceptions.UserNotValidException;
import com.holyk.clearsolutions.exceptions.UserPatchIsNotValidException;
import com.holyk.clearsolutions.services.UserService;

//@formatter:off
/**
 * 2.1. Create user. It allows to register users who are more than [18] years old. The value [18] should be taken from properties file.
 * 2.2. Update one/some user fields
 * 2.3. Update all user fields
 * 2.4. Delete user
 * 2.5. Search for users by birth date range. Add the validation which checks that “From” is less than “To”.  Should return a list of objects

 */
//@formatter:on
@RestController
@RequestMapping("/users")
public class UserController {

	private UserService service;
	@Value("${app.user.minimum.age}")
	private Integer ageRequired;

	/**
	 * @param service
	 */
	public UserController(@Autowired UserService service) {
		super();
		this.service = service;
	}

	/**
	 * 2.1. Create user. It allows to register users who are more than [18] years
	 * old. The value [18] should be taken from properties file.
	 * 
	 * @param data
	 * @return
	 */
	@PostMapping()
	public ResponseEntity<User> createUser(@RequestBody UserRequest data) {

		validateUser(data);

		User user = service.save(data);
		return ResponseEntity.status(HttpStatus.CREATED).body(user);

	}

	/**
	 * 2.2. Update one/some user fields
	 * 
	 * @param id
	 * @param patch
	 * @return
	 * @throws JsonProcessingException
	 * @throws JsonPatchException
	 */
	@PatchMapping(path = "/{id}", consumes = "application/json-patch+json")
	public ResponseEntity<UserResponse> patchUser(@PathVariable long id, @RequestBody JsonPatch patch)
			throws JsonProcessingException, JsonPatchException {
		Optional<User> optional = service.findUserById(id);
		User user = optional.orElseThrow(() -> new UserNotFoundException("User not found!"));

		validatePatch(patch, user);

		User updateUser = service.patch(id, patch);
		return ResponseEntity.status(HttpStatus.OK).body(UserResponse.of(updateUser));

	}

	@PutMapping("/{id}")
	public ResponseEntity<UserResponse> updateUser(@PathVariable long id, @RequestBody UserRequest data) {
		validateUser(data);
		User update = service.update(id, data);

		return ResponseEntity.status(HttpStatus.OK).body(UserResponse.of(update));

	}

	@GetMapping()
	public ResponseEntity<List<User>> getUsersByBirthdateRange(@RequestParam("fromDate") LocalDate from,
			@RequestParam("toDate") LocalDate to) {
		validateRange(from, to);
		List<User> list = service.getUsersByDateRange(from, to);
		if (list.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		}
		return ResponseEntity.status(HttpStatus.OK).body(list);

	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Object> deleteUser(@PathVariable Long id) {
		boolean success = service.delete(id);
		if (!success) {
			return ResponseEntity.notFound().build();
		} else
			return ResponseEntity.noContent().build();
	}

	private void validatePatch(JsonPatch patch, User user) throws JsonPatchException, JsonProcessingException {

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.findAndRegisterModules();
		JsonNode patched = patch.apply(objectMapper.convertValue(user, JsonNode.class));

		User patchedUser = objectMapper.treeToValue(patched, User.class);

		if (//@formatter:off
			patchedUser.getBirthdate() == null 
				|| !isBirthdateValid(patchedUser.getBirthdate())
				|| isNullOrEmpty(patchedUser.getEmail()) 
				|| !isEmailValid(patchedUser.getEmail())
				|| isNullOrEmpty(patchedUser.getFirstname())
				|| isNullOrEmpty(patchedUser.getLastname())
			//@formatter:on
		) {
			throw new UserPatchIsNotValidException("Patch is not valid!");
		}

	}

	private void validateUser(UserRequest request) {
		var data = request.data();
		var birthdate = data.birthdate();

		if (isNullOrEmpty(data.email()) || isNullOrEmpty(data.firstname()) || isNullOrEmpty(data.lastname())
				|| data.birthdate() == null) {
			throw new NullPointerException("One or more required fields are null!");
		}

		if (!isEmailValid(data.email()) || birthdate.isAfter(LocalDate.now())) {
			throw new UserNotValidException("User is not valid!");
		}

		if (!isUserAgeSaitsfy(birthdate)) {
			throw new UserAgeNotSatisfyException("Too young!");
		}

	}

	private void validateRange(LocalDate from, LocalDate to) {
		if (from==null||to==null) {
			throw new NullPointerException("Range of dates is not presented!");
		}
		if (Period.between(from, to).isNegative() || to.isAfter(LocalDate.now()))
			throw new DateRangeIsNotValidException("Range of dates is not valid!");

		/**
		 * Meant to be unreachable lol
		 */
		if (from.isBefore(LocalDate.of(1900, 1, 1)) && to.isAfter(LocalDate.now())) {
			throw new DateRangeIsNotValidException("І мертвим, і живим, і ненародженим...");

		}
	}

	private boolean isUserAgeSaitsfy(LocalDate birthdate) {
		return birthdate.plusYears(ageRequired).isBefore(LocalDate.now());
	}

	private boolean isEmailValid(String email) {
		return email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
	}

	private boolean isNullOrEmpty(String string) {
		return string == null || "".equals(string);
	}

	private boolean isBirthdateValid(LocalDate birthdate) {
		return birthdate != null && birthdate.isBefore(LocalDate.now()) && isUserAgeSaitsfy(birthdate);
	}

	@ExceptionHandler(value = { DateRangeIsNotValidException.class, NullPointerException.class,
			UserPatchIsNotValidException.class, UserNotValidException.class })
	public ResponseEntity<UserErrorResponse> handle(RuntimeException ex) {

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new UserErrorResponse(HttpStatus.BAD_REQUEST.toString(), ex.getMessage()));
	}

	@ExceptionHandler
	public ResponseEntity<UserErrorResponse> handle(UserAgeNotSatisfyException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new UserErrorResponse(HttpStatus.FORBIDDEN.toString(), ex.getMessage()));
	}

	@ExceptionHandler
	public ResponseEntity<UserErrorResponse> handle(UserNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new UserErrorResponse(HttpStatus.NOT_FOUND.toString(), ex.getMessage()));
	}

}
