package br.com.codeminimal.minimojs;

import java.io.InputStream;
import java.io.Serializable;

public class XFile implements Serializable {
	private static final long serialVersionUID = -393803149183140003L;
	private String fieldName;
	private String fileName;
	private String contentType;
	private boolean isInMemory;
	private long sizeInBytes;
	private byte[] data;

	protected XFile() {
	}

	public XFile(String fileName, String contentType, byte[] data) {
		super();
		this.fileName = fileName;
		this.contentType = contentType;
		this.isInMemory = false;
		this.sizeInBytes = data.length;
		this.data = data;
	}

	public String getFieldName() {
		return fieldName;
	}

	protected void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getFileName() {
		return fileName;
	}

	public String getExtension() {
		int index = fileName.lastIndexOf(".");
		if (index > 0) {
			return fileName.substring(index + 1);
		} else {
			return "";
		}
	}

	protected void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getContentType() {
		return contentType;
	}

	protected void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public boolean isInMemory() {
		return isInMemory;
	}

	protected void setInMemory(boolean isInMemory) {
		this.isInMemory = isInMemory;
	}

	public long getSizeInBytes() {
		return sizeInBytes;
	}

	protected void setSizeInBytes(long sizeInBytes) {
		this.sizeInBytes = sizeInBytes;
	}

	public byte[] getData() {
		return data;
	}

	protected void setData(byte[] data) {
		this.data = data;
	}
}
