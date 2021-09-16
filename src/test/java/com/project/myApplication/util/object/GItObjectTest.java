package com.project.myApplication.util.object;

import org.junit.jupiter.api.Test;

public class GItObjectTest {

	@Test
	void makeBlob() {
		
	}
	
	@Test
	void makeTree() {
		Tree tree = new Tree();

		// given
		Blob gitignore = new Blob("72a984f539c6f51160e217a15328ebc48a8232a0", 0);
		Blob readme = new Blob("6e414b3ce0b979a2280f1db57eff18dd7681f025", 0);
		Tree webinf = new Tree("265bccde2198f45bbcbce3c4c0bc94256dd29abc");
		Blob buildgradle = new Blob("a6e4c16a5214077e3b063a8d22ac119248a6b3fc",0);
		Blob gradlew  = new Blob("744e882ed57263a19bf3a504977da292d009345f", 0);
		Blob gradlewbat  = new Blob("107acd32c4e687021ef32db511e8a206129b88ec", 0);
		Blob settingsgradle = new Blob("5698ff9861f08baa7bf9dd0d26dce66567a57a36", 0);
		Tree src = new Tree("2489c0335d9331d0c4a6dec58fc5dfa797867224");
		
		tree.addObject(".gitignore", gitignore);
		tree.addObject("README.md", readme);
		tree.addObject("WEB-INF", webinf);
		tree.addObject("build.gradle", buildgradle);
		tree.addObject("gradlew", gradlew);
		tree.addObject("gradlew.bat", gradlewbat);
		tree.addObject("settings.gradle", settingsgradle);
		tree.addObject("src", src);
		
		
		
		//then
	}
	
	
}
