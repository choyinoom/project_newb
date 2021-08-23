package com.project.myApplication.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Objects are compressed when they are stored in git directories
 * @author Owner
 *
 */
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
	
	public String decompress(byte[] content) {
		
		String result = "";
		Inflater decompresser = new Inflater();
		decompresser.setInput(content);
		
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(content.length)) {
			byte[] buffer = new byte[BUFFER_SIZE];
			while(!decompresser.finished()) {
				int resultLength = decompresser.inflate(buffer);
				outputStream.write(buffer, 0, resultLength);
			}
			
//			result = new String(outputStream.toByteArray(), "UTF-8");
			result = new String(outputStream.toByteArray());
		} catch (Exception e) {
			
		}
		
		return result;
	}
	
	public static void main(String[] args) throws IOException {
		String path = "D:\\vscode-workspace\\git_test\\.git\\objects\\3c\\4e9cd789d88d8d89c1073707c3585e41b0e614";
        // String path="E:\\spring-workspace\\uni17_user\\.git\\index";
        File file = new File(path);
        FileInputStream fis = new FileInputStream(file);
        
        ZipUtil util = new ZipUtil();
        String test = "d8329fc1cc938780ffdd9f94e0d364e0ea74f579";
        String result1 = new String(util.compress(test.getBytes()), "UTF-8");
        System.out.println(result1);
        String result2 = util.decompress(util.compress(test.getBytes()));
        System.out.println(result2);
        
//        String result = util.decompress(fis.readAllBytes());
//        System.out.println(result.getBytes().length);
//        System.out.println(result);
	}
}
