package com.project.myApplication.util;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

		String header = String.format("%s %d\u0000", type, byteContent.length);
		
		try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream();) {
			outputStream.write(header.getBytes());
			outputStream.write(byteContent);			
			sha1 = sha1Hex(outputStream.toByteArray());
		} catch(IOException e) {
			e.printStackTrace();
		}
		return sha1;
	}
	

	
}
