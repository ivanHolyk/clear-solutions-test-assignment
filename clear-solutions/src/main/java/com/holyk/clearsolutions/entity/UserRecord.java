package com.holyk.clearsolutions.entity;

import java.time.LocalDate;

public record UserRecord(String email, String firstname, String lastname, LocalDate birthdate, String address,
		String phone) {
}
