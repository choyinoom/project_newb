package com.project.myApplication.util.object;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import com.project.myApplication.util.HashGenerator;

import lombok.Getter;

@Getter
public class Tree implements GitObject{

	private static final long serialVersionUID = 6414044871241752223L;
	private String hash;
	private int size;
	private String mtime;
	private Map<String, GitObject> tree = new TreeMap<>();
	private transient byte[] content;
	
	public Tree(String mtime) {
		this.size = 0;
		this.mtime = mtime;
	}
	
	
	public Tree() {
	}


	public void makeTreeHash(HashGenerator hashGenerator) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		tree.forEach((k, v) -> {
			if (v instanceof Tree) {
				((Tree) v).makeTreeHash(hashGenerator);
			}
			try {
				outputStream.write(catFile(k,v));
			} catch (Exception e) {
				e.printStackTrace();
			} 
		});
		//set hash and size
		byte[] tmp = outputStream.toByteArray();
		hash = hashGenerator.getObjectName("tree", tmp);
		size = tmp.length;
		
		String header = String.format("%s %d\u0000", "tree", size);
		try {
			outputStream.reset();
			outputStream.write(header.getBytes());
			outputStream.write(tmp);
			content = outputStream.toByteArray();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void addObject(String name, GitObject go) {
		tree.put(name, go);
	}
	
	public byte[] catFile(String name, GitObject go){
		String head = null;
		byte[] store = null;
		
		if(go instanceof Blob) {
			head = String.format("100644 %s\u0000",name);
		} else {
			head = String.format("40000 %s\u0000",name);
		}

		try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream();) {
			byte[] b = Hex.decodeHex(go.getHash().toCharArray());
			outputStream.write(head.getBytes(StandardCharsets.UTF_8));
			outputStream.write(b);
			store = outputStream.toByteArray(); 
		} catch(DecoderException de) {
			de.printStackTrace();
		} catch(IOException ie) {
			ie.printStackTrace();
		}
		
		return store;
	}

	public List<String> print(String parent, List<String> fileList) {
		tree.forEach((k,v) -> {
			if(v instanceof Blob) {
				fileList.add(parent + k); 
			} else {
				Tree tree = (Tree) v;
				if (parent.equals("")) {
					tree.print(k+"/", fileList);
				} else {
					tree.print(parent+k+"/", fileList);
				}
			}
		});
		
		return fileList;
		
	}
	@Override
	public int getSize() {
		return size;
	}

	
	@Override
	public String getHash() {
		return hash;
	}
	
	@Override
	public String getType() {
		return "tree";
	}
	
	@Override
	public String getMtime() {
		return mtime;
	}
}
