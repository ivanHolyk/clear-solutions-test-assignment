package com.holyk.clearsolutions.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.holyk.clearsolutions.entity.User;
import com.holyk.clearsolutions.entity.UserRecord;
import com.holyk.clearsolutions.exceptions.UserNotFoundException;

@Service
public class UserService {
	private List<User> list;
	private long increment;

	public UserService() {
		this.list = new ArrayList<>();
		this.increment = 1L;
	}

	/**
	 * For test purpose only!
	 * 
	 * @param list
	 * @param increment
	 */
	public UserService(List<User> list, Long increment) {
		this.list = list;
		this.increment = increment;
	}

	public User update(long id, User user) {
		list = list.stream().map(u -> {
			if (u.getId() == id) {
				return user;
			}
			return u;
		}).toList();
		return user;
	}

	public List<User> getUsersByDateRange(LocalDate from, LocalDate to) {
		return list.stream().filter(u -> isDateBetween(u.getBirthdate(), from, to)).toList();
	}

	public User save(UserRecord userR) {
		User user = User.of(userR);
		user.setId(increment++);
		this.list.add(user);
		return user;
	}

	public boolean delete(long id) {
		return list.removeIf(u -> u.getId() == id);
	}

	private boolean isDateBetween(LocalDate date, LocalDate from, LocalDate to) {
		return from.compareTo(date) * date.compareTo(to) >= 0;

	}

	public Optional<User> update(long id, UserRecord userR) {
		checkIfUserExistElseThrow(id);
		// TODO
		return null;
	}

	public User patch(long id, JsonPatch patch) throws JsonProcessingException, JsonPatchException {

		return update(applyPatchToUser(patch, checkIfUserExistElseThrow(id)));
	}

	/**
	 * Ensure that user is exist
	 * 
	 * @param newUser
	 * @return
	 */
	private User update(User newUser) {

		int index = IntStream.range(0, list.size()).filter(i -> list.get(i).getId() == newUser.getId()).findFirst()
				.orElse(-1);
		list.set(index, newUser);
		return newUser;
	}

	private User checkIfUserExistElseThrow(long id) {
		Optional<User> optional = findUserById(id);
		if (optional.isEmpty()) {
			throw new UserNotFoundException();
		}
		return optional.get();
	}

	private Optional<User> findUserById(long id) {
		return list.stream().filter(u -> u.getId() == id).findFirst();
	}

	private User applyPatchToUser(JsonPatch patch, User targetUser) throws JsonPatchException, JsonProcessingException {

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.findAndRegisterModules();
		JsonNode patched = patch.apply(objectMapper.convertValue(targetUser, JsonNode.class));
		return objectMapper.treeToValue(patched, User.class);
	}

	/**
	 * For test purpose only!
	 */
	public List<User> getList() {
		return list;
	}

	/**
	 * For test purpose only!
	 */
	void setList(List<User> list) {
		this.list = list;
	}

	/**
	 * For test purpose only!
	 */
	public long getIncrement() {
		return increment;
	}

	/**
	 * For test purpose only!
	 */
	void setIncrement(long increment) {
		this.increment = increment;
	}

}
