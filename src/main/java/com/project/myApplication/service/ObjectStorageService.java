package com.project.myApplication.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project.myApplication.PropertiesConfig;
import com.project.myApplication.domain.FileMetadata;
import com.project.myApplication.util.FileMap;
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
	private static final String LOGS_DIR = ".git/logs";
	private static final String REFS_DIR = ".git/refs/heads";
	private static final String OBJECTS_DIR = ".git/objects/";
	
	private HashGenerator hashGenerator = HashGenerator.getInstance();
	private ZipUtil zipUtil = ZipUtil.getInstance();

	private final String tmpLocation;
	private final String homeLocation;
	
	
	public ObjectStorageService(PropertiesConfig config) {
		this.tmpLocation = config.getStorageTmp();
		this.homeLocation = config.getStorageHome();
	}
	
	/**
	 * git 디렉토리 초기 구성
	 * @param repositoryId
	 */
	public void init(Long repositoryId) {
		log.debug("Git Init!!!!!");
		String id = String.valueOf(repositoryId);
		Path git = Path.of(homeLocation, id, GIT_DIR);
		Path logs = Path.of(homeLocation, id, LOGS_DIR);
		Path objects = Path.of(homeLocation, id, OBJECTS_DIR);
		Path refs = Path.of(homeLocation, id, REFS_DIR);
		
		Path head = Path.of(homeLocation, id, LOGS_DIR, "HEAD");
		Path master = Path.of(homeLocation, id, REFS_DIR, "master");
		
		try {
			createDirectories(git);
			createDirectories(logs);
			createDirectories(refs);
			createDirectories(objects);

			saveIndex(new Tree(), id);
			head.toFile().createNewFile();
			master.toFile().createNewFile();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 커밋한 파일을 깃 레파지토리에 저장
	 * @param list 커밋한 파일 목록
	 * @param commit 커밋 메시지와 커밋한 유저에 대한 정보
	 */
	public void store(List<FileMetadata> list, Map<String, String> commit) {
		ZonedDateTime now = getTime();
		try {
			String repositoryId = String.valueOf(list.get(0).getRepositoryId());
			Tree index = getIndex(repositoryId);
			for(FileMetadata f : list) {
				Blob blob = saveBlob(f, now);
				String fileName = f.getDirectory() == null? f.getName(): Path.of(f.getDirectory(), f.getName()).toString();			
				index = updateIndex(index, blob, fileName);
			}
			index.makeTreeHash(hashGenerator); // 모든 tree의 해시값을 생성한다.
			saveTree(index, repositoryId);
			saveIndex(index, repositoryId);
			String commitHash = saveCommit(index, commit, now);
			updateLog(commitHash, commit, now);
			updateHead(repositoryId, commitHash);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	
	public List<FileMap> getEntry(Long repositoryId, String path) {
		String id = String.valueOf(repositoryId);
		Tree index = getIndex(id);
		Map<String, GitObject> tree =  index.getTree();
		
		// path 가 null이면 루트 디렉토리의 엔트리를 보여줘야 하므로 아래 로직 생략.
		if(path != null && path.indexOf("/") >= 0) {
			String[] tokens = path.split("/");
			Tree treePointer = null;
			for(int i = 1; i < tokens.length ; i++) {
				treePointer = (Tree) tree.get(tokens[i]);
				tree = treePointer.getTree();
			}
		}
		
		List<FileMap> entry = new ArrayList<>();
		tree.forEach((k, v) -> {
			FileMap map = new FileMap();
			map.setName(k);
			map.setMtime(v.getMtime());
			map.setType(v.getType());
			entry.add(map);
		});
		
		return entry;
	}
	
	
	public List<String> getBlob(Long repositoryId, String path) {
		String id = String.valueOf(repositoryId);
		Tree index = getIndex(id);
		Map<String, GitObject> tree =  index.getTree();
		Blob blob = null;
		if(path.indexOf("/") >= 0) {
			String[] tokens = path.split("/");
			Tree treePointer = null;
			int i = 1;
			for(; i < tokens.length-1 ; i++) {
				treePointer = (Tree) tree.get(tokens[i]);
				tree = treePointer.getTree();
			}
			blob = (Blob)tree.get(tokens[i]);
			
		} else {
			blob = (Blob)tree.get(path);
		}
		
		String hash = blob.getHash();
		Path destination = Path.of(
				homeLocation,
				id,
				OBJECTS_DIR,
				hash.substring(0,2),
				hash.substring(2));
		
		List<String> content = new ArrayList<>();
		try {
			byte[] compressed = Files.readAllBytes(destination);
			String decompressed = zipUtil.decompress(compressed);
			String[] tmp = decompressed.split("\n");
			for(String line: tmp) {
				content.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		return content;
	}

	public List<String> findFiles(Long repositoryId) {
		List<String> fileList = new ArrayList<>();
		String id = String.valueOf(repositoryId);
		Tree index = getIndex(id);
		fileList = index.print("", fileList);
		return fileList;
	}
	
	
	
	/**
	 * index를 반환하는 용도: 모든 파일의 최신 커밋 상태를 보기 위해
	 * @param repositoryId
	 * @return index
	 */
	private Tree getIndex(String repositoryId) {
		Path path = Paths.get(homeLocation, repositoryId, GIT_DIR, "index");
		Tree index = null;
		try (InputStream is = Files.newInputStream(path);
			 ObjectInputStream ois = new ObjectInputStream(is)) {
			index = (Tree) ois.readObject();
		} catch (Exception e) {
			log.error("index 불러오기 실패");
			e.printStackTrace();
		}
		return index;
	}
	
	/**
	 * blob을 만들어서 .git/objects 아래 경로에 저장한다.
	 * @param data
	 * @return
	 * @throws IOException
	 */
	private Blob saveBlob(FileMetadata data, ZonedDateTime now) throws IOException {
		String repositoryId = String.valueOf(data.getRepositoryId());
		Path srcDirectory = Paths.get(
				tmpLocation,
				String.valueOf(repositoryId));
		Path src = null;
		File[] fileList = srcDirectory.toFile().listFiles();
		for(File f : fileList) {
			if (f.getName().indexOf(data.getFileId()) > 0) {
				src = f.toPath();
				break;
			}
		}
		
		byte[] content = Files.readAllBytes(src);
		String objectName = getObjectName("blob", content);
		byte[] objectContent = getObjectContent(content);
		
		saveObject(repositoryId, objectName, objectContent);
		
		String mtime = now.format(DateTimeFormatter.ISO_LOCAL_DATE);
		Blob blob = new Blob(objectName, objectContent.length, mtime);
		return blob;
	}
	
	/**
	 * 파일 내용이 바뀜에 따라 새로 생성된 해시값들을 index에 반영
	 * @param index
	 * @param blob
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	private Tree updateIndex(Tree index, Blob blob, String filename) throws Exception {
		if (filename.indexOf(File.separator) >= 0) {
			// 파일이 루트 경로에 있지 않은 경우
			String[] filenameToken = filename.split("\\\\");
			Map<String, GitObject> entries = index.getTree();
			Tree treePointer = null;
			int i = 0;
			// 재귀적으로 트리 속으로 들어간다.
			for (; i < filenameToken.length - 1 ; i++) {
				if (!entries.containsKey(filenameToken[i])) {
					// 트리가 없으면 만든다.
					entries.put(filenameToken[i], new Tree(blob.getMtime()));
				}
				treePointer = (Tree)entries.get(filenameToken[i]);
				entries = treePointer.getTree();
			}
			// 맨 마지막 트리에 blob을 넣는다. 예전 값은 대체된다.
			if (entries.containsKey(filenameToken[i])) {
				Blob old = (Blob) entries.get(filenameToken[i]);
				if (old.getHash().equals(blob.getHash())) { // 해시값이 같으면
					return index;							// 업데이트 할 내용 없음
				} 
			} 
			entries.put(filenameToken[i], blob);
		}else {
			// 파일이 루트 경로에 있는 경우
			index.addObject(filename, blob);
		}
		return index;
	}
	
	
	private void saveObject(String repositoryId, String objectName, byte[] objectContent) {
		
		Path destination = Path.of(
				homeLocation,
				repositoryId,
				OBJECTS_DIR,
				objectName.substring(0, 2)
				);
		
		try {
			File file = Path.of(destination.toString(), objectName.substring(2)).toFile();
			if(!file.exists()) {
				createDirectories(destination);
				createFile(file.toPath(), objectContent);							
			} 
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * tree를 만들어서 .git/objects 아래 경로에 저장한다.
	 * @param index
	 * @param repositoryId
	 */
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

	/**
	 * commit object를 만들어서 .git/objects 아래 경로에 저장한다.
	 * @param index
	 * @param commit
	 * @param now
	 * @return
	 */
	private String saveCommit(Tree index, Map<String, String> commit, ZonedDateTime now) {

		String treeHash = String.format("tree %s\n", index.getHash());
		String authorInfo = String.format("author %s <%s> %s %tz\n", 
				commit.get("owner"),
				commit.get("email"),
				now.toEpochSecond(),
				now);
		String commiterInfo = authorInfo.replace("author", "commiter");
		String message = String.format("\n%s\n", commit.get("message"));
		String description = String.format("\n%s\n", commit.get("description"));
		String strContent = treeHash + authorInfo + commiterInfo + message + description;

		byte[] content = strContent.getBytes(StandardCharsets.UTF_8);
		String objectName = getObjectName("commit", content);
		byte[] objectContent = getObjectContent(content);
		
		String repositoryId = commit.get("repositoryId");
		saveObject(repositoryId, objectName, objectContent);
		return objectName;
	}
	
	/**
	 * 현재 브랜치의 모든 커밋을 저장하고 있는 HEAD 파일에 로그를 추가
	 * @param repositoryId
	 * @param commitHash
	 * @param commit
	 */
	private void updateLog(String commitHash, Map<String, String> commit, ZonedDateTime now) {
		String repositoryId = commit.get("repositoryId");
		Path master = Path.of(homeLocation, repositoryId, REFS_DIR, "master");
		Path head = Path.of(homeLocation, repositoryId, LOGS_DIR, "HEAD");
		String parent = "0".repeat(20);
		if (master.toFile().length() > 0) {
			try {
				byte[] content = Files.readAllBytes(master);
				parent = new String(content, StandardCharsets.UTF_8);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		String line = String.format("%s %s %s <%s> %s %tz commit: %s\n", 
				parent, 
				commitHash, 
				commit.get("owner"), 
				commit.get("email"),
				now.toEpochSecond(),
				now,
				commit.get("message"));
		try(OutputStream os = Files.newOutputStream(head, StandardOpenOption.APPEND)) {
			os.write(line.getBytes(StandardCharsets.UTF_8));
			os.flush();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 현재 브랜치의 가장 최신 커밋 정보를 가지고 있는 master(브랜치 이름) 파일을 수정
	 * @param repositoryId
	 * @param commitHash
	 */
	private void updateHead(String repositoryId, String commitHash) {
		Path master = Path.of(homeLocation, repositoryId, REFS_DIR, "master");
		try (OutputStream os = Files.newOutputStream(master)) {
			os.write(commitHash.getBytes(StandardCharsets.UTF_8));
			os.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private ZonedDateTime getTime() {
		ZoneId id = ZoneId.of("Asia/Seoul");
		return Instant.now().atZone(id);
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
