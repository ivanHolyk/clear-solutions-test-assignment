package com.holyk.clearsolutions.entity;

import java.time.LocalDate;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

public class User {

	private long id;

	private String email;
	private String firstname;
	private String lastname;
	@JsonFormat
	private LocalDate birthdate;
	private String address;
	private String phone;

	/**
	 * 
	 */
	public User() {
		super();
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public LocalDate getBirthdate() {
		return birthdate;
	}

	public void setBirthdate(LocalDate birthdate) {
		this.birthdate = birthdate;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @param email
	 * @param firstname
	 * @param lastname
	 * @param birthdate
	 * @param address
	 * @param phone
	 */
	public User(String email, String firstname, String lastname, LocalDate birthdate, String address, String phone) {
		super();
		this.email = email;
		this.firstname = firstname;
		this.lastname = lastname;
		this.birthdate = birthdate;
		this.address = address;
		this.phone = phone;
	}

	public static User of(UserRecord userR) {
		return new User(userR.email(), userR.firstname(), userR.lastname(), userR.birthdate(), userR.address(),
				userR.phone());
	}

	@Override
	public int hashCode() {
		return Objects.hash(address, birthdate, email, firstname, id, lastname, phone);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof User))
			return false;
		User other = (User) obj;
		return Objects.equals(address, other.address) && Objects.equals(birthdate, other.birthdate)
				&& Objects.equals(email, other.email) && Objects.equals(firstname, other.firstname) && id == other.id
				&& Objects.equals(lastname, other.lastname) && Objects.equals(phone, other.phone);
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", email=" + email + ", firstname=" + firstname + ", lastname=" + lastname
				+ ", birthdate=" + birthdate + ", address=" + address + ", phone=" + phone + "]";
	}

}
