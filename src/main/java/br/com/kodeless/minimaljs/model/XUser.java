package br.com.kodeless.minimaljs.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import br.com.kodeless.minimaljs.XJsonIgnore;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@SuppressWarnings("serial")
@Entity
public class XUser extends XEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column
	private String login;

	@Column
	private String domain;

	@Column
	@XJsonIgnore
	private String password;

	@LazyCollection(LazyCollectionOption.FALSE)
	@ElementCollection
	@Column(name = "xuser_av_functions")
	private List<String> availableFunctions;

	@Column
	private String role;

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public List<String> getAvailableFunctions() {
		return availableFunctions;
	}

	public void setAvailableFunctions(List<String> availabeFunctions) {
		this.availableFunctions = availabeFunctions;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

}
