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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import nyu.crawler.crawler.Crawler;
import nyu.crawler.data.Interval;
import nyu.crawler.data.Link;
import nyu.crawler.data.Page;
import nyu.crawler.indexer.IndexerMap;
import nyu.crawler.retriever.Retriever;

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
		Link link = new Link("file.html", "WalrusAndCarpenter", "dummy abs url", "uniqueURL");
		String content = "<a href=\"file1.html\">WalrusAndCarpenter</a>";
		String query = "walrus carpenter bread";
		Page page = new Page("id1", link, content, "title");
		assertEquals(100, crawler.score(link, page.getContent().split(" "), query), 0);
		content = "<a href=\"walrus5.html\">Cute Poem</a>";
		page.setContent(content);
		link.setAnchor("Cute Poem");
		link.setUrl("walrus5.html");
		assertEquals(40, crawler.score(link, page.getContent().split(" "), query), 0);
		String content2 = "walrus word walrus word3 word4 <a href=\"dummy.html\">Cute Poem</a> w1 w2 w3 w4 w5 bread dude bread";
		page.setContent(content2);
		link.setUrl("dummy.html");
		assertEquals(5, crawler.score(link, page.getContent().split(" "), query), 0);
		
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
	
//	@Test
	public void testUrlRequest() throws IOException {
		String urlString = "http://cs.nyu.edu/courses/spring16/CSCI-GA.2580-001/MarineMammal/BrydesWhale.html";
		String stuff = Jsoup.connect(urlString).get().baseUri();
		System.out.println(stuff);
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
		indMap.map.put("test1", new Page("id1",new Link("url", "anchor", "absURL", "uniqueURL"), "content", "title"));
		indMap.map.put("test2", new Page("id2",new Link("url2", "anchor2", "absURL2", "uniqueURL2"), "content2", "title2"));
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

//	@Test
	public void testUrlMatching() {
		String url = "";
		String[] urls = {"https://en.wikipedia.org/wiki/Vladimir_Nabokov", "http://www.abc.com/index?hello", "http://www.nyu.edu", "http://www.wikipedia.org/something#blabla", "http://www.nyu.edu/index.html"};
		Pattern pattern = Pattern.compile("https?://[^#\\?]+");
//		Pattern pattern = Pattern.compile("http.*/[a-z\\.A-Z0-9_-]+$");
		for (String string : urls) {
			Matcher m = pattern.matcher(string);
			System.out.println(string + " " + m.matches());
		}
	}
	
//	@Test
	public void sortedSetTest() {
		SortedSet<Integer> sortedSet = new TreeSet<Integer>();
		sortedSet.add(10);
		sortedSet.add(19);
		sortedSet.add(12);
		sortedSet.add(1);
		sortedSet.add(100);
		sortedSet.add(99);
		System.out.println(sortedSet);
		List<Integer> list = new ArrayList<Integer>(sortedSet);
		System.out.println(list);
	}
	
//	@Test
	public void testSnippet() {
		String fullContent = "It was the best of times, it was the worst of times, it was the age of wisdom,"
				+ "it was the age of foolishness, it was the epoch of belief, it was the epoch of incredulity,"
				+ "it was the season of Light, it was the season of Darkness, it was the spring of hope, it was the winter of despair,"
				+ "we had everything before us, we had nothing before us, we were all going direct to Heaven,"
				+ "we were all going direct the other way – in short, the period was so far like the present period,"
				+ "that some of its noisiest authorities insisted on its being received, for good or for evil, in the superlative degree of comparison only";
		String content = "It was the best of times, it was the worst of times, it was the age of ";
		Retriever retriever = new Retriever("indexPath");
		System.out.println(retriever.getSnippet(fullContent, "best of times season of authorities"));
//		System.out.println(Retriever.getSnippetFromStringWithSubstring(content, 0, "best of times"));
//		System.out.println(Retriever.getSnippetFromStringWithSubstring(content, 0, "it was"));
//		System.out.println(Retriever.getSnippetFromStringWithSubstring(content, 0, "best times worst times"));
	}
	
	@Test
	public void testSnippet2() throws IOException {
		Document doc = Jsoup.connect("http://www.nytimes.com/").get();
		Retriever retriever = new Retriever("indexPath");
		System.out.println(retriever.getSnippet(doc.body().text(), "tom brady"));
	}
	
//	@Test
	public void testIntervals() {
		Retriever retriever = new Retriever("indexPath");
		List<Interval> intervals = new ArrayList<Interval>();
		intervals.add(new Interval(0,3));
		intervals.add(new Interval(2,4));
		intervals.add(new Interval(7,10));
		intervals.add(new Interval(6,13));
		intervals.add(new Interval(18,20));
		List<Interval> reduced = retriever.reduceIntervals(intervals,0);
		System.out.println(reduced);
	}
	
//	@Test
	public void doctypeTest() {
		String url = "https://en.wikipedia.org/wiki/bla-Bla";
		System.out.println(isUrlValid(url));
	}
	
//	@Test
	public void testingRegex() {
		String queryString = "science, fiction!!";
		String reducedQuery = queryString.replaceAll("[^-a-zA-Z0-9_ ]", "");
		System.out.println(reducedQuery);
	}
	
	private boolean isUrlValid(String absUrl) {
		Pattern pattern = Pattern.compile("https?://[-a-zA-Z0-9 \\._/]+");
		Matcher matcher = pattern.matcher(absUrl);
		if (matcher.matches())
			return true;
		return false;
		
//		return absUrl.endsWith("html");
	}
}
