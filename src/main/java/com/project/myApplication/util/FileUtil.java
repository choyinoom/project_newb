package com.project.myApplication.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileUtil {
	
	private static final FileUtil instance = new FileUtil();
	
	public static FileUtil getInstance() {
		return instance;
	}
	
	public List<FileMap> listDirectory(String path) {
		List<FileMap> list = new ArrayList<>();
		File file = new File(path);
		if(file.isDirectory()) {
			File[] fileList = file.listFiles();
			for(File l : fileList) {
				FileMap fileMap = new FileMap();
				fileMap.setName(l.getName());
				if (l.isDirectory()) {
					fileMap.setType("dir");
				} else {
					fileMap.setType("file");
				}
				FileTime time;
				try {
					time = Files.getLastModifiedTime(file.toPath());
					fileMap.setMtime(time.toString());
				} catch (IOException e) {
					log.error("[FileUtil.java] cannot get file attribute");
					e.printStackTrace();
				}
				
				list.add(fileMap);
			}
		}
		return list;
	}
	
	
	public List<String> readFile(String path) {
		Path file = Paths.get(path);
		List<String> lines = new ArrayList<>();
		try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			//파일을 한 줄씩 읽어들인다.
			for(String line; (line = reader.readLine()) != null; ) {
				lines.add(line);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
		return lines;
	}
	
	private static byte[] toByteArray(String s) {
	    return DatatypeConverter.parseHexBinary(s);
	}
	
	public static void main(String[] args) {
//		String path = "D:\\choyi\\first-project\\index.js";
//		FileUtil util = new FileUtil();
//		util.listDirectory(path);
//		List<String> lines = util.readFile(path);
//		int count=0;
//		for(String line:lines) {
//			System.out.println(line.length());
//			System.out.println(line);
//			count++;
//			if(count > 5) break;
//		}

		byte[] b = toByteArray("268371bca93408f8a903e574d0b185c7f93b9686");
		System.out.println(Arrays.toString(b));
	}
}
