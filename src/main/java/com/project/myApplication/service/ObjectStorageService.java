package com.project.myApplication.service;

import java.io.BufferedReader;
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
import com.project.myApplication.util.Util;
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
	private Util util = Util.getInstance();
	
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
	public ZonedDateTime store(List<FileMetadata> list, Map<String, String> commit) {
		ZonedDateTime now = util.getTime();
		try {
			String repositoryId = commit.get("repositoryId");
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
		return now;
	}

	

	private Map<String, String> parseCommitObject(Path object) {
		String decompressed = bytesToString(object);
		Map<String, String> found = util.parseCommit(decompressed);
		return found;
	}
	
	private List<FileMap> parseTreeObject(Path object) {
		byte[] decompressed = decompressObject(object);
		List<FileMap> entry =  util.parseTree(decompressed);		
		return entry;
	}
	
	private byte[] decompressObject (Path path) {
		byte[] result = null;
		try(InputStream is = Files.newInputStream(path)) {
			byte[] compressed = is.readAllBytes();
			byte[] decompressed = zipUtil.decompress(compressed);
			result = decompressed;
		} catch (IOException e) {
			log.error("{}을 읽다가 오류가 났다", path.toString(), e);
		}
		return result;
	}
	
	private String bytesToString (Path path) {
		String result = "";
		byte[] decompressed = decompressObject(path);
		result = new String(decompressed, StandardCharsets.UTF_8);
		return result;
	}
	

	/**
	 * 프로젝트 내 디렉토리별 목록을 반환
	 * @param repositoryId
	 * @param branch
	 * @param path
	 * @return
	 */
	public List<FileMap> getEntry(Long repositoryId, String branch, String path) {
		log.debug("branch is = {}, path is = {}", branch, path);
		String id = String.valueOf(repositoryId);
		String[] tokens = path.split("/");

		List<FileMap> entry = new ArrayList<>();
		
		if(branch.equals("main")) {
			Tree index = getIndex(id);
			Map<String, GitObject> tree = index.getTree();
			for(int i = 1; i < tokens.length ; i++) {
				Tree treePointer = (Tree) tree.get(tokens[i]);
				tree = treePointer.getTree();
			}
			List<FileMap> cur = new ArrayList<>();
			tree.forEach((k, v) -> {
				FileMap map = new FileMap();
				map.setName(k);	
				map.setType(v.getType());
				map.setMtime(v.getMtime());
				map.setHash(v.getHash());
				cur.add(map);
			});
			entry = cur;
		} else {
			// 요청이 들어온 branch에 맞는 index를 생성해야 한다.
			// commit 부터 시작해서 차근차근 순회
			Path commit = getObjectPath(id, branch);
			String treeHash = parseCommitObject(commit).get("tree");
			Path tree = getObjectPath(id, treeHash);
			List<FileMap> cur = parseTreeObject(tree);
			for(int i = 1; i< tokens.length ; i++) {
				FileMap treePointer = null;
				for(FileMap map : cur) {
					if(map.getName().equals(tokens[i])) {
						treePointer = map;
						treeHash = treePointer.getHash();
						break;
					}
				}
				tree = getObjectPath(id, treeHash);
				cur = parseTreeObject(tree);
				
			}
			entry = cur;
			for(FileMap file: entry) {
				String hash = file.getHash();
				Path filePath = getObjectPath(id, hash);
				String mtime = util.getLastModifiedTime(filePath);
				file.setMtime(mtime);
			}
		}
		return entry;
	}
	
	/**
	 * 해당 path의 blob파일을 문자열로 변환하여 반환
	 * @param repositoryId
	 * @param path 파일 경로
	 * @return
	 */
	public List<String> getBlob(Long repositoryId, String branch, String path) {
		List<FileMap> entry = null;
		
		String filename = util.parseFilename(path);
		String prefix = path.replace("/"+filename, "");
		entry = getEntry(repositoryId, branch, prefix);
		
		
		String hash = "";
		for(FileMap file: entry) {
			if(file.getName().equals(filename)) {
				log.debug("this is THE file: {}",file.toString());
				hash = file.getHash();
				break;
			}
		}
		
		Path destination = getObjectPath(String.valueOf(repositoryId), hash);
		log.debug("uri is {}, filename is {}",destination.toString(), filename);
		String decompressed = bytesToString(destination);
		String[] tmp = decompressed.split("\n");
		
		List<String> content = new ArrayList<>();
		for(String line: tmp) {
			content.add(line);
		}
		
		return content;
	}

	/**
	 * 인덱스에 있는 모든 파일의 전체 경로 프린트
	 * @param repositoryId
	 * @param branch 커밋오브젝트 해시값
	 * @return
	 */
	public List<String> findFiles(Long repositoryId, String branch) {
		List<String> fileList = new ArrayList<>();
		String id = String.valueOf(repositoryId);
		Tree index = null;
		if (branch.equals("main")) {
			index = getIndex(id);
		} else {
			// commit 오브젝트부터 순회하면서 새로운 index를 만들어야한다.
			// fileList만 반환하면 되기 때문에 time은 고려하지 않는다.
			index = new Tree();
			Path commit = getObjectPath(id, branch);
			String treeHash = parseCommitObject(commit).get("tree");
			index = createIndexByFileMap(id, index, treeHash);
						
		}
		fileList = index.print("", fileList);
		return fileList;
	}
	
	
	private Tree createIndexByFileMap(String repositoryId, Tree index, String treeHash) {
		Path treePath = getObjectPath(repositoryId, treeHash);
		List<FileMap> entry = parseTreeObject(treePath);
		Map<String, GitObject> tree = index.getTree();
		for(FileMap file : entry) {
			String type = file.getType();
			switch(type) {
			case "tree": 
				String dir = file.getName();
				tree.put(dir, new Tree(file.getHash(), null));
				createIndexByFileMap(repositoryId, (Tree)tree.get(dir), file.getHash());
				break;
			case "blob":
				tree.put(file.getName(), new Blob(file.getHash(), 0));
				break;
			}
		}
		return index;
	}
	
	
	
	public List<Map<String, String>> getCommitHistory(Long repositoryId, String branch) {
		List<Map<String, String>> history = new ArrayList<>();
		String id = String.valueOf(repositoryId);
		branch = branch == null ? getIndex(id).getHash() : branch;
		Path head = Path.of(homeLocation, id, LOGS_DIR, "HEAD");
		try(BufferedReader reader = Files.newBufferedReader(head, StandardCharsets.UTF_8)) {
			for(String line; (line = reader.readLine()) != null;) {
				Map<String, String> commit = util.parseLogs(line);
				if(commit.get("parent").equals(branch)) {
					break;
				}
				// commit message에 대한 description은 별도로 가져와야 한다.
				String hash = commit.get("hash");
				Path path = getObjectPath(id, hash);
				String description = parseCommitObject(path).get("description");
				commit.put("description", description);
				history.add(commit);
			}
		} catch (IOException ex) {
			log.error(ex.getMessage());
		}
		return history;
	}
	

		
	
	/**
	 * index를 반환하는 용도: 모든 파일의 최신 커밋 상태를 보기 위해
	 * serialized 되었던 객체를 다시 object로 deserialization 
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
		log.debug("blob 해시값 생성: {}", objectName);
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
			treePointer.setMtime(blob.getCtime());
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
		String parentHash = String.format("parent %s\n", getHead(commit.get("repositoryId")));
		String authorInfo = String.format("author %s <%s> %s %tz\n", 
				commit.get("owner"),
				commit.get("email"),
				now.toEpochSecond(),
				now);
		String commiterInfo = authorInfo.replace("author", "committer");
		String message = String.format("\n%s\n", commit.get("message"));
		String description = String.format("\n%s\n", commit.get("description"));
		final String strContent = parentHash.length() > 10 
				? treeHash + parentHash + authorInfo + commiterInfo + message + description
				: treeHash + authorInfo + commiterInfo + message + description;

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
		String parent = "0".repeat(40);
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

	
	private String getHead(String repositoryId) {
		String head = "";
		Path master = Path.of(homeLocation, repositoryId, REFS_DIR, "master");
		try {
			byte[] content = Files.readAllBytes(master);
			head = new String(content, StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("HEAD를 읽다가 오류가 났다!", e);
		}
		return head;
	}
	
	private Path getObjectPath(String repositoryId, String hash) {
		return Path.of(
			homeLocation,
			repositoryId,
			OBJECTS_DIR,
			hash.substring(0,2),
			hash.substring(2)
		);
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
