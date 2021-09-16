package com.project.myApplication.util;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import lombok.extern.slf4j.Slf4j;

/**
 * Objects are compressed when they are stored in git directories
 * @author Owner
 *
 */
@Slf4j
public class ZipUtil {

	private static final int BUFFER_SIZE = 1024;
	private static final ZipUtil instance = new ZipUtil();
	
	public static ZipUtil getInstance() {
		return instance;
	}
	
	public byte[] compress(byte[] content) {

		byte[] result = null;
		Deflater compresser = new Deflater();
		compresser.setInput(content);

		try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream(content.length)) {

			compresser.finish();
			byte[] buffer = new byte[BUFFER_SIZE];
			while (!compresser.finished()) {
				int compressedDataLength = compresser.deflate(buffer);
				outputStream.write(buffer, 0, compressedDataLength);
			}
			
			result =  outputStream.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public byte[] decompress(byte[] content) {
		
		byte[] result = null;
		Inflater decompresser = new Inflater();
		decompresser.setInput(content);
		
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(content.length)) {
			byte[] buffer = new byte[BUFFER_SIZE];
			while(!decompresser.finished()) {
				int resultLength = decompresser.inflate(buffer);
				outputStream.write(buffer, 0, resultLength);
			}
			result = outputStream.toByteArray();
		} catch (Exception e) {
			log.error("decompress 하다가 에러가 났따", e);
		}
		
		return result;
	}
}
