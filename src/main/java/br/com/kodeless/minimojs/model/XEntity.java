package br.com.kodeless.minimojs.model;

import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class XEntity implements Serializable, Cloneable {

	protected XEntity cloneEntity() {

		try {
			return (XEntity) this.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("The object cannot be cloned", e);
		}

	}

	public abstract Long getId();

	@Override
	public boolean equals(Object obj) {
		return obj != null && obj.getClass().equals(this.getClass()) && this.getId().equals(((XEntity) obj).getId());
	}

	@Override
	public String toString() {
		return getClass() + ":" + getId() != null ? getId().toString() : null;
	}

	@Override
	public int hashCode() {
		return this.getId() != null ? ((Long) this.getId()).hashCode() : super.hashCode();
	}
}
