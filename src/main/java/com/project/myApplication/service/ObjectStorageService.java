package com.project.myApplication.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.project.myApplication.util.HashGenerator;
import com.project.myApplication.util.ZipUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ObjectStorageService {

	private static final String STORAGE_ROOT = "";
	private static final String OBJECTS_DIR = ".git/objects/";

	private HashGenerator hashGenerator = HashGenerator.getInstance();
	private ZipUtil zipUtil = ZipUtil.getInstance();

	public void save(Map<String, Object> fileMap) throws IOException {
		String owner = (String) fileMap.get("owner");
		String projectName = (String) fileMap.get("projectName");
		String path = (String) fileMap.get("path");
		MultipartFile mfile = (MultipartFile) fileMap.get("file");

		final String prefix = String.format("D:/%s/%s/", owner, projectName);
		createDirectories(prefix + OBJECTS_DIR);
		createDirectories(prefix + path.replace(mfile.getOriginalFilename(), ""));
		createFile(prefix + path, mfile.getBytes());

		String[] token = path.split("/");
		Queue<String> queue = new LinkedList<>();
		
		for (String t : token) {
			queue.add(t);
		}
		
		while (!queue.isEmpty()) {
			String splitedPath = queue.poll();
			if (queue.size() > 0) {
				// create original directory
				// Files.createDirectories(Paths.get(prefix + ), null)
				continue;

			} else {
				// save original file

				// save blob
				try {
					String name = getObjectName("blob", mfile);
					byte[] content = getObjectContent(mfile);
					createDirectories(prefix + OBJECTS_DIR + name.substring(0, 2));
					createFile(prefix + OBJECTS_DIR + name.substring(0, 2) + "/" + name.substring(2), content);
				} catch (IOException e) {

				}
			}
		}

	}

	private void createDirectories(String path) throws IOException {
		Files.createDirectories(Paths.get(path));
	}

	private void createFile(String path, byte[] content) throws IOException {
		log.debug(path);
		Files.write(Paths.get(path), content);
	}


	private String getObjectName(String type, MultipartFile file) throws IOException {
		return hashGenerator.getObjectName(type, file.getBytes());
	}

	private byte[] getObjectContent(MultipartFile file) throws IOException {
		return zipUtil.compress(file.getBytes());
	}
}
