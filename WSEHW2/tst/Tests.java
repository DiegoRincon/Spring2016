import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class Tests {

//	@Test
	public void test() {
		String str = "hello my dear how are you";
		String[] strArray = str.split(" ");
//		System.out.println(Crawler.findStringsInArray(strArray, "how are"));
//		System.out.println(str.indexOf("how are you"));
//		System.out.println(str.lastIndexOf("how are you") + new String("how are you").length());
//		System.out.println(str.length());
	}
	
	@Test
	public void testScore() {
		Crawler crawler = new Crawler();
		Link link = new Link("file.html", "WalrusAndCarpenter");
		String content = "<a href=\"file1.html\">WalrusAndCarpenter</a>";
		String query = "walrus carpenter bread";
		Page page = new Page("filename", content);
		assertEquals(100, crawler.score(link, page, query), 0);
		content = "<a href=\"walrus5.html\">Cute Poem</a>";
		page.content = content;
		link.anchor = "Cute Poem";
		link.url = "walrus5.html";
		assertEquals(40, crawler.score(link, page, query), 0);
		String content2 = "walrus word walrus word3 word4 <a href=\"dummy.html\">Cute Poem</a> w1 w2 w3 w4 w5 bread dude bread";
		page.content = content2;
		link.url = "dummy.html";
		assertEquals(5, crawler.score(link, page, query), 0);
		
	}
	
//	@Test
	public void testUrlRequest() {
		String urlString = "http://stackoverflow.com/robots2.txt";
		URL url = null;
		try {
			url = new URL(urlString);
		} catch (Exception e) {
			System.out.println("exception 1");
		}
		try {
			URLConnection uc = url.openConnection();
			uc.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:19.0) Gecko/20100101 Firefox/19.0");
			InputStream is = uc.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			String content = "";
			while ((line = br.readLine()) != null) {
				content += line;
			}
			System.out.println(content);
			is.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

}
