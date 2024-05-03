package com.holyk.clearsolutions.controllers;

import java.time.LocalDate;

import com.holyk.clearsolutions.entity.User;

public record UserResponse(UserResponseData data) {

	public record UserResponseData(long id, String email, String firstname, String lastname, LocalDate birthdate,
			String address, String phone) {

	}

	public static UserResponse of(User user) {

		return new UserResponse(new UserResponseData(user.getId(), user.getEmail(), user.getFirstname(),
				user.getLastname(), user.getBirthdate(), user.getAddress(), user.getPhone()));
	}
}