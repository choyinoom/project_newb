package com.project.myApplication.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.project.myApplication.util.FileMap;

@SpringBootTest
public class ObjectStorageServiceTest {

		@Autowired ObjectStorageService objectStorageService;
		
		
		@Test
		public void 커밋기록조회() {
			List<Map<String, String>> history = objectStorageService.getCommitHistory(Long.valueOf(98), null);
			assertEquals(2, history.size());
			
			List<Map<String, String>> history2 = objectStorageService.getCommitHistory(Long.valueOf(98), "01efc16b4d3031debba8ed70dbad2b5305b98e0c");
			assertEquals(2, history2.size());
			
			List<Map<String, String>> history3 = objectStorageService.getCommitHistory(Long.valueOf(98), "d39e86a4670edb64529e608ca31999df6c29764c");
			assertEquals(1 , history3.size());
		}
		
		@Test
		public void 커밋별파일리스트출력() {
			Long repositoryId = (long)98;
			List<String> fileList = objectStorageService.findFiles(repositoryId, "01efc16b4d3031debba8ed70dbad2b5305b98e0c");
			
			assertEquals(3, fileList.size());
		}
		
		@Test
		public void 파일내용출력() {
			List<String> test1 = objectStorageService.getBlob((long)98, "main", "test1.txt");
			assertThat(test1.get(0)).isEqualTo("version1");
			
			List<String> hello = objectStorageService.getBlob((long)99, "main", "/src/hello.txt");
			assertThat(hello.get(0)).isEqualTo("안녕! 반가워!");
		}
		
		@Test
		public void 파일목록출력() {
			List<FileMap> list = objectStorageService.getEntry((long)101, "629cebd658f742482090cf1da3a29cec7cf00120", "/");
			assertEquals(2, list.size());
			FileMap src = null;
			FileMap txt = null;
			for(FileMap f : list) {
				if(f.getName().equals("src"))
					src = f;
				else 
					txt = f;
			}
			assertEquals("592f4637ea68f59e2ffac981e798a32bca521c65", src.getHash());
			assertEquals("8068e75b7cab5af4893de973475b19b610170773", txt.getHash());
		}
		
			
}
