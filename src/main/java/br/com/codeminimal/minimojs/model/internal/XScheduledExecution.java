package br.com.codeminimal.minimojs.model.internal;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import br.com.codeminimal.minimojs.XJsonIgnore;
import br.com.codeminimal.minimojs.model.XEntity;

@Entity
public class XScheduledExecution extends XEntity {

	private static final long serialVersionUID = 11432123879823078L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column
	private String scheduleName;

	@Column
	@XJsonIgnore
	private Date executionDate;

	@Column
	@XJsonIgnore
	private Date realExecutionDate;

	@Column
	@XJsonIgnore
	private Boolean executed;

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

	public Date getExecutionDate() {
		return executionDate;
	}

	public void setExecutionDate(Date executionDate) {
		this.executionDate = executionDate;
	}

	public Boolean getExecuted() {
		return executed;
	}

	public void setExecuted(Boolean executed) {
		this.executed = executed;
	}

	public Date getRealExecutionDate() {
		return realExecutionDate;
	}

	public void setRealExecutionDate(Date realExecutionDate) {
		this.realExecutionDate = realExecutionDate;
	}

}
