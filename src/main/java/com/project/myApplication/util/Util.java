package com.project.myApplication.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.security.crypto.codec.Hex;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Util {

	private static final Util instance = new Util();

	public static Util getInstance() {
		return instance;
	}

	public Map<String, String> parseLogs(String line) {
		String pattern = "(?<parent>\\w{40}) (?<me>\\w{40}) (?<committer>\\S+) <(?<email>\\S+)> (?<mtime>\\d{10}) (?:[\\+|\\-][0-9]{4}) commit: (?<message>.+)";
		Matcher m = regexMatch(line, pattern);

		Map<String, String> found = new HashMap<>();
		if (m.find()) {
			found.put("parent", m.group("parent"));
			found.put("hash", m.group("me"));
			found.put("committer", m.group("committer"));
			found.put("time", EpochToDate(m.group("mtime")));
			found.put("message", m.group("message"));
		}
		return found;
	}

	public Map<String, String> parseCommit(String line) {

		String pattern = "tree (?<tree>\\w+)\\n(parent (?<parent>\\w+)\\n)?author (?<aName>\\S+) <(?<aEmail>\\S+)> (?<aMtime>\\d{10}) (?:[\\+|\\-][0-9]{4})\\ncommitter (?<cName>\\S+) <(?<cEmail>\\S+)> (?<cMtime>\\d{10}) (?:[\\+|\\-][0-9]{4})\\n\\n(?<message>.*)(\\n\\n(?<description>(.|\\n)*))?";
		Matcher m = regexMatch(line, pattern);

		Map<String, String> found = new HashMap<>();
		if (m.find()) {
			found.put("tree", m.group("tree"));
			found.put("parent", m.group("parent"));
			found.put("authorName", m.group("aName"));
			found.put("authorEmail", m.group("aEmail"));
			found.put("authorMtime", m.group("aMtime"));
			found.put("committerName", m.group("cName"));
			found.put("comitterEmail", m.group("cEmail"));
			found.put("commiterMtime", m.group("cMtime"));
			found.put("message", m.group("message"));
			found.put("description", m.group("description"));
		}
		return found;
	}

	public List<FileMap> parseTree(byte[] content) {
		String pattern = "(?<type>40000|100644) (?<name>.+?)\\x00(?<hash>.+?(?=40000|100644)|.+)";
		String line = new String(content, StandardCharsets.UTF_8);
		Matcher m = regexMatch(line, pattern);

		List<FileMap> list = new ArrayList<>();
		while (m.find()) {
			FileMap map = new FileMap();
			String type = m.group("type");
			switch (type) {
			case "40000":
				map.setType("tree");
				break;
			case "100644":
				map.setType("blob");
				break;
			}
			String name = m.group("name");
			map.setName(name);
			int index = lastIndexOf(content, name.getBytes()) + name.getBytes().length + 1;
			byte[] byteHash = Arrays.copyOfRange(content, index, index + 20);
			map.setHash(String.valueOf(Hex.encode(byteHash)));
			log.debug("[parseTree]: {}", map.toString());
			list.add(map);
		}
		return list;
	}



	public String parseFilename(String path) {
		String filename = "";
		String pattern = "([^\\/]+)$";
		Matcher m = regexMatch(path, pattern);
		if (m.find()) {
			filename = m.group(1);
		}
		return filename;
	}

	public Matcher regexMatch(String line, String pattern) {
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(line);
		return m;
	}

	public String EpochToDate(String epochSecond) {
		Instant instant = Instant.ofEpochSecond(Long.valueOf(epochSecond));
		ZoneId id = ZoneId.of("Asia/Seoul");
		ZonedDateTime time = instant.atZone(id);
		return time.format(DateTimeFormatter.ISO_LOCAL_DATE);
	}

	public ZonedDateTime getTime() {
		ZoneId id = ZoneId.of("Asia/Seoul");
		return Instant.now().atZone(id);
	}

	public String getLastModifiedTime(Path path) {
		log.debug("파일 경로는 여기: {}", path.toString());
		String mtime = "";
		try {
			BasicFileAttributes readAttributes = Files.readAttributes(path, BasicFileAttributes.class);
			Instant instant = readAttributes.lastModifiedTime().toInstant();
			String date = instant.toString();
			mtime = date.substring(0, 10);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return mtime;
	}
	
	/**
	 * http://www.java2s.com/example/java-utility-method/array-last-index-of/lastindexof-byte-source-byte-match-62eb8.html
	 * @param source
	 * @param match
	 * @return byte version of indexOf
	 */
	private int lastIndexOf(byte[] source, byte[] match) {

		if (source.length < match.length) {
			return -1;
		}

		for (int i = (source.length - match.length); i >= 0; i--) {
			if (startsWith(source, i, match)) {
				return i;
			}
		}
		return -1;
	}

	private boolean startsWith(byte[] source, int offset, byte[] match) {

		if (match.length > (source.length - offset)) {
			return false;
		}

		for (int i = 0; i < match.length; i++) {
			if (source[offset + i] != match[i]) {
				return false;
			}
		}
		return true;
	}
}
