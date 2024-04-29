package com.holyk.clearsolutions.controllers;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

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
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.holyk.clearsolutions.entity.User;
import com.holyk.clearsolutions.entity.UserRecord;
import com.holyk.clearsolutions.exceptions.DateRangeIsNotValidException;
import com.holyk.clearsolutions.exceptions.UserNotFoundException;
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
	 * @param userR
	 */
	@PostMapping()
	public ResponseEntity<User> createUser(@RequestBody UserData data) {

		UserRecord userR = data.data();

		if (!isUserAgeSaitsfy(userR)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		User user = service.save(userR);
		return ResponseEntity.status(HttpStatus.CREATED).body(user);

	}

	/**
	 * 2.2. Update one/some user fields
	 * 
	 * @return
	 * @throws JsonPatchException
	 * @throws JsonProcessingException
	 */
	@PatchMapping(path = "/{id}", consumes = "application/json-patch+json")
	public ResponseEntity<User> patchUser(@PathVariable long id, @RequestBody JsonPatch patch)
			throws JsonProcessingException, JsonPatchException {
		User updateUser = service.patch(id, patch);
		return ResponseEntity.status(HttpStatus.OK).body(updateUser);

	}

	@PutMapping("/{id}")
	public ResponseEntity<User> updateUser(@PathVariable long id, UserData data) {

		UserRecord userR = data.data();
		User update = service.update(id, userR);

		return ResponseEntity.status(HttpStatus.OK).body(update);

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
	public ResponseEntity<?> deleteUser(@PathVariable Long id) {
		boolean success = service.delete(id);
		if (!success) {
			return ResponseEntity.notFound().build();
		} else
			return ResponseEntity.noContent().build();
	}

	private void validateRange(LocalDate from, LocalDate to) {
		if (Period.between(from, to).isNegative())
			// TODO add controller advice for exception handling
			throw new DateRangeIsNotValidException();
	}

	private boolean isUserAgeSaitsfy(UserRecord user) {

		return user.birthdate().plusYears(ageRequired).isBefore(LocalDate.now());
	}

	@ExceptionHandler
	public ResponseEntity<String> handle(UserNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found!");
	}
}
