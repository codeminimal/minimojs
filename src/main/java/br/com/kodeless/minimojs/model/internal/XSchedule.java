package br.com.kodeless.minimojs.model.internal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import br.com.kodeless.minimojs.XJsonIgnore;
import br.com.kodeless.minimojs.model.XEntity;

@Entity
public class XSchedule extends XEntity {

	private static final long serialVersionUID = 1143212366513423078L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column
	private String scheduleName;

	@Column
	@XJsonIgnore
	private Long lastExecution;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getScheduleName() {
		return scheduleName;
	}

	public void setScheduleName(String scheduleName) {
		this.scheduleName = scheduleName;
	}

	public Long getLastExecution() {
		return lastExecution;
	}

	public void setLastExecution(Long lastExecution) {
		this.lastExecution = lastExecution;
	}

}
