package com.project.myApplication.util;
import java.nio.charset.StandardCharsets;

import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;

public class HashGenerator {

	/**
	 * Only make Hash Object names using SHA1
	 * @param type - blob, tree, commit
	 * @param size - byte length
	 * @param contents
	 * @return
	 */
	
	private static final HashGenerator instance = new HashGenerator();
	
	public static HashGenerator getInstance() {
		return instance;
	}
	
	public String getObjectName(String type, byte[] byteContent) {
		String sha1 = "";
		
		StringBuilder store = new StringBuilder();
		
		// header part
		store.append(type);
		store.append(" ");
		store.append(byteContent.length);
		store.append("\u0000"); // null byte
		// content part
		String content = bytesToString(byteContent);
		store.append(content);
		
		// store = header + content
		sha1=sha1Hex(store.toString());
		return sha1;
	}
	
	private String bytesToString(byte[] content) {
		return new String(content, StandardCharsets.UTF_8);
	}
	
	public static void main(String[] args) {
		
		String contents = "what is up, doc?";
		byte[] b = contents.getBytes();
		HashGenerator h = new HashGenerator();
		System.out.println(h.getObjectName("blob", b));
		
	}
}
