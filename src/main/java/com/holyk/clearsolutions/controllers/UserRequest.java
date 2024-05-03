package com.holyk.clearsolutions.controllers;

import java.time.LocalDate;

import com.holyk.clearsolutions.entity.User;

public record UserRequest(UserRequestData data) {

	public record UserRequestData(String email, String firstname, String lastname, LocalDate birthdate, String address,
			String phone) {

	}

	public static UserRequest of(User user) {

		return new UserRequest(new UserRequestData(user.getEmail(), user.getFirstname(), user.getLastname(),
				user.getBirthdate(), user.getAddress(), user.getPhone()));
	}

	public static UserRequest of(String email, String firstname, String lastname, LocalDate birthdate, String address,
			String phone) {

		return new UserRequest(new UserRequestData(email, firstname, lastname, birthdate, address, phone));
	}
}
