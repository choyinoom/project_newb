package com.project.myApplication.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project.myApplication.PropertiesConfig;
import com.project.myApplication.domain.FileMetadata;
import com.project.myApplication.util.HashGenerator;
import com.project.myApplication.util.ZipUtil;
import com.project.myApplication.util.object.Blob;
import com.project.myApplication.util.object.GitObject;
import com.project.myApplication.util.object.Tree;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ObjectStorageService {

	private static final String GIT_DIR = ".git/";
	private static final String OBJECTS_DIR = ".git/objects/";

	private HashGenerator hashGenerator = HashGenerator.getInstance();
	private ZipUtil zipUtil = ZipUtil.getInstance();

	private final String tmpLocation;
	private final String homeLocation;
	
	
	public ObjectStorageService(PropertiesConfig config) {
		this.tmpLocation = config.getStorageTmp();
		this.homeLocation = config.getStorageHome();
	}
	
	
	public void store(List<FileMetadata> list) {
		try {
			String repositoryId = String.valueOf(list.get(0).getRepositoryId());
			Tree index = getIndex(repositoryId);
			for(FileMetadata f : list) {
				Blob blob = saveBlob(f);
				String fileName = f.getDirectory() == null? f.getName(): Path.of(f.getDirectory(), f.getName()).toString();			
				index = updateIndex(index, blob, fileName);
			}
			index.makeTreeHash(hashGenerator); // 모든 tree의 해시값을 생성한다.
			saveTree(index, repositoryId);
			saveIndex(index, repositoryId);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	

	private Tree getIndex(String repositoryId) throws IOException, ClassNotFoundException {
		Path path = Paths.get(homeLocation, repositoryId, GIT_DIR, "index");
		File file = path.toFile();
		Tree index = null;
		if(file.exists()) {
			try (InputStream is = Files.newInputStream(path);
				 ObjectInputStream ois = new ObjectInputStream(is)) {
				index = (Tree) ois.readObject();
			}
		} else {
			index = new Tree();
		}
		return index;
	}
	
	/**
	 * blob을 만들어서 .git/objects 아래 경로에 저장한다.
	 * @param data
	 * @return
	 * @throws IOException
	 */
	private Blob saveBlob(FileMetadata data) throws IOException {
		String repositoryId = String.valueOf(data.getRepositoryId());
		Path srcDirectory = Paths.get(
				tmpLocation,
				String.valueOf(repositoryId));
		Path srcPath = null;
		File[] fileList = srcDirectory.toFile().listFiles();
		for(File f : fileList) {
			if (f.getName().indexOf(data.getFileId()) > 0) {
				srcPath = f.toPath();
				break;
			}
		}
		
		byte[] content = Files.readAllBytes(srcPath);
		String objectName = getObjectName("blob", content);
		byte[] objectContent = getObjectContent(content);
		
		saveObject(repositoryId, objectName, objectContent);
		
		Blob blob = new Blob(objectName, objectContent.length);
		return blob;
	}
	
	
	private Tree updateIndex(Tree index, Blob blob, String filename) throws Exception {
		if (filename.indexOf(File.separator) >= 0) {
			String[] filenameToken = filename.split("\\\\");
			Map<String, GitObject> entries = index.getTree(); // 파일 또는 폴더 이름, blob 또는 tree
			Tree treePointer = null;
			int i = 0;
			// 재귀적으로 트리 속으로 들어간다.
			for (; i < filenameToken.length - 1 ; i++) {
				if (!entries.containsKey(filenameToken[i])) {
					// 트리가 없으면 만든다.
					entries.put(filenameToken[i], new Tree());
				}
				treePointer = (Tree)entries.get(filenameToken[i]);
				entries = treePointer.getTree();
			}
			// 맨 마지막 트리에 blob을 넣는다. 예전 값은 대체된다.
			entries.put(filenameToken[i], blob);
		}else {
			index.addObject(filename, blob);
		}
		return index;
	}
	
	
	private void saveObject(String repositoryId, String objectName, byte[] objectContent) {
		
		Path dstPath = Path.of(
				homeLocation,
				repositoryId,
				OBJECTS_DIR,
				objectName.substring(0, 2)
				);
		
		try {
			File file = Path.of(dstPath.toString(), objectName.substring(2)).toFile();
			if(!file.exists()) {
				createDirectories(dstPath);
				createFile(file.toPath(), objectContent);							
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private void saveTree(Tree index, String repositoryId) {
		
		Map<String, GitObject> entry = index.getTree();
		entry.forEach((k, v) -> {
			if (v instanceof Tree) {
				saveTree((Tree) v, repositoryId);
			}
		});
		
		byte[] content = index.getContent();
		String objectName = index.getHash();
		byte[] objectContent = getObjectContent(content);
		
		saveObject(repositoryId, objectName, objectContent);			
	}

	
	
	
	private void saveIndex(Tree index, String repositoryId) {
		Path destination = Path.of(homeLocation, repositoryId, GIT_DIR, "index");
		try (FileOutputStream fos = new FileOutputStream(destination.toFile());
				ObjectOutputStream oos = new ObjectOutputStream(fos)) {
			oos.writeObject(index);
			oos.flush();			
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	
	private String getTime() {
		LocalDateTime date = LocalDateTime.now();
		Date today = Timestamp.valueOf(date);
		return String.format("%s", Long.toHexString(today.getTime()/1000));
	}
	

	

	
	
	
	private void createDirectories(Path path) throws IOException {
		Files.createDirectories(path);
	}

	private Path createFile(Path path, byte[] content) throws IOException {
		return Files.write(path, content);
	}


	private String getObjectName(String type, byte[] content){
		return hashGenerator.getObjectName(type, content);
	}

	private byte[] getObjectContent(byte[] content) {
		return zipUtil.compress(content);
	}
}
