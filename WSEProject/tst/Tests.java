import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.lucene.search.Weight;
import org.jsoup.Connection;
import org.jsoup.Connection.Request;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	
//	@Test
	public void testScore() {
		//TODO: fix this
		Crawler crawler = new Crawler(new String[10]);
		Link link = new Link("file.html", "WalrusAndCarpenter", "dummy abs url");
		String content = "<a href=\"file1.html\">WalrusAndCarpenter</a>";
		String query = "walrus carpenter bread";
		Page page = new Page(1L, link, new HashSet<Link>(), content, "title");
		assertEquals(100, crawler.score(link, page, query), 0);
		content = "<a href=\"walrus5.html\">Cute Poem</a>";
		page.setContent(content);
		link.setAnchor("Cute Poem");
		link.setUrl("walrus5.html");
		assertEquals(40, crawler.score(link, page, query), 0);
		String content2 = "walrus word walrus word3 word4 <a href=\"dummy.html\">Cute Poem</a> w1 w2 w3 w4 w5 bread dude bread";
		page.setContent(content2);
		link.setUrl("dummy.html");
		assertEquals(5, crawler.score(link, page, query), 0);
		
	}
	private InetAddress[] dnslookup(String url) {
		try {
			InetAddress inetAddress = InetAddress.getLocalHost();
			displayStuff("local host", inetAddress);
//			System.out.print("--------------------------");
			inetAddress = InetAddress.getByName(url);
			displayStuff(url, inetAddress);
//			System.out.print("--------------------------");
			return InetAddress.getAllByName(url);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void displayStuff(String whichHost, InetAddress inetAddress) {
//		System.out.println("--------------------------");
//		System.out.println("Which Host:" + whichHost);
//		System.out.println("Canonical Host Name:" + inetAddress.getCanonicalHostName());
//		System.out.println("Host Name:" + inetAddress.getHostName());
//		System.out.println("Host Address:" + inetAddress.getHostAddress());
	}
	
	@Test
	public void testUrlRequest() throws IOException {
		String urlString = "https://www.reddit.com/";
		Document stuff = Jsoup.connect(urlString).get();
		System.out.println(stuff.toString());
//		Connection cn = Jsoup.connect(urlString);
//		Request rq = cn.request();
//		URL url = rq.url();
//		System.out.println(url.getHost());
//		System.out.println(url.getFile());
//		InetAddress[] addresses = InetAddress.getAllByName(url.getHost());
//		for (InetAddress address : addresses) {
//			System.out.println(address.getHostAddress());
//		}
//		String req = Jsoup.connect(urlString).get().toString();
//		System.out.println(req);
	}
	
//	@Test
	public void testTitle() throws IOException {
		Document html = Jsoup.connect("http://cs.nyu.edu/~drs414/courses.html").get();
		Element title = html.select("abstsr").first();
		if (title == null) {
			System.out.println("errror!");
			return;
		}
		String originalTitle = title.text();
		System.out.println(originalTitle);
	}
	
//	@Test
	public void testMapSerialization() throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		IndexerMap indMap = new IndexerMap();
		indMap.map.put("test1", new Page(1L,new Link("url", "anchor", "absURL"), new HashSet<Link>(), "content", "title"));
		indMap.map.put("test2", new Page(2L,new Link("url2", "anchor2", "absURL2"), new HashSet<Link>(), "content2", "title2"));
		mapper.writeValue(new File("map"), indMap);
//		mapper.writeValueAsString(map);
		
		IndexerMap indMap2 = mapper.readValue(new File("map"), IndexerMap.class);
		System.out.println(indMap2.map);
		
	}
	
//	@Test
	public void urlNormalization() throws MalformedURLException, URISyntaxException, UnknownHostException {
		URL url = new URL("http://www.EXAMPLE.com:80/index.html/../HelloGOodbye.html");
		System.out.println(url.getHost());
//		String str = url.getProtocol().toLowerCase() + "://"
//						+ url.getHost().toLowerCase()
//						+ (url.getPort() != -1 && url.getPort() != 80 ? ":"
//						+ url.getPort() : "") + url.getFile();
//		str = new URI(str).normalize().toString();
		System.out.println(Crawler.normalizeURL("http://www.EXAMPLE.com:80/index.html/../HelloGOodbye.html"));
	}

}
