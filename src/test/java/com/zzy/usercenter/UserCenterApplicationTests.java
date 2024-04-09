package com.zzy.usercenter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class UserCenterApplicationTests {

	@Test
	void contextLoads() {
		String pattern = "abba";
		String s = "dog cat cat dog";
		wordPattern(pattern,s);
	}
	public boolean wordPattern(String pattern, String s) {
		String[] words = s.split(" ");
		if(pattern.length()!= words.length){
			return false;
		}
		Map<String,Character> hasPattern_b= new HashMap<>();
		Map<Character,String>  hasPattern = new HashMap<>();
		for(int i = 0 ; i < pattern.length();i++){
			char c  = pattern.charAt(i);
			String h = words[i];
			if(hasPattern.containsKey(c)&&hasPattern.get(c).equals(h)||hasPattern_b.containsKey(h)&&hasPattern_b.get(h).equals(c)){
				return false;
			}
			hasPattern.put(c,h);
			hasPattern_b.put(h,c);
		}
		return true;
	}
}
