import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Crawler {
	public static final int distanceFromQueryWords = 5;
    public static final String DISALLOW = "Disallow:";
    public static final String USER_AGENT_STAR = "user-agent: *";
    public static final String USER_AGENT = "user-agent:";
	public Options options;
	public String url;
	public String query;
	public String path;
	public int maxNumOfPages;
	public boolean trace;
	private Map<String, List<String>> robotSafeHostsMap;
	
	public Crawler() {
		initOptions();
		this.trace = false;
		this.maxNumOfPages = 50;
		this.robotSafeHostsMap = new HashMap<String, List<String>>();
	}
	
	private void initOptions() {
		this.options = new Options();
		this.options.addOption("u", true, "URL argument");
		this.options.addOption("q", true, "Query");
		this.options.addOption("docs", true, "Path");
		this.options.addOption("m", true, "Max number of pages");
		this.options.addOption("t", false, "Trace");
	}
	
	public void run(String[] args) throws ParseException {		
		if (!checkArgs(args)) {
			System.exit(1);
		} else {
			startCrawler();
		}
		
	}
	
	private void startCrawler() {
		URLScore originalURLScore = new URLScore(this.url, 0);
		
		
		PriorityQueue<URLScore> queue = new PriorityQueue<URLScore>(100, Collections.reverseOrder());
		queue.add(originalURLScore);
		
		Map<String, URLScore> urlToURLScoreMap = new HashMap<String, URLScore>();		
		urlToURLScoreMap.put(this.url, originalURLScore);	
		
		Set<String> seen = new HashSet<String>();
		
		while (!queue.isEmpty() && seen.size() <= this.maxNumOfPages ) {
			URLScore bestURL = queue.poll();
			if (urlToURLScoreMap.remove(bestURL.url) == null) {
				throw new RuntimeException("The URL to Score mapping should exist!");
			}
			if (this.trace)
				System.out.println("\nDownloading: " + bestURL.url + "; Score = " + bestURL.score);
			Page page = null;
			URL url = null;
			try {
				url = new URL(bestURL.url);
			} catch (MalformedURLException e) {
				if (this.trace)
					System.out.println("URL " + bestURL.url + " was malformed!");
				continue;
			}
			if (!robotSafe(url)) {
				System.out.println("Told not to go there by the robot.txt file");
				continue;
			}
			try {
				page = request(url);
			} catch (IOException e) {
				//page doesn't exist!
				if (this.trace)
					System.out.println("There was a problem fetching " + bestURL.url + " (it probably doesn't exist!)");
				continue;
			}
			if (page != null) {
				seen.add(bestURL.url);
				writeToFile(page.filename, page.content);
				if (seen.size() >= this.maxNumOfPages)
					return;
				if (this.trace)
					System.out.println("Received: " + bestURL.url);
				String base = getBaseFromURL(url.toString());
				List<Link> links = getLinks(page.content, base);
				for (Link link : links) {
					double score = score(link, page, this.query);
					URLScore urlScore = new URLScore(link.url, score);
					if (!seen.contains(link.url)) {
						if (!urlToURLScoreMap.containsKey(urlScore.url)) {
							if (this.trace)
								System.out.println("Adding to queue: " + urlScore);
							queue.add(urlScore);
							urlToURLScoreMap.put(urlScore.url, urlScore);
						} else {
							URLScore existingUrl = urlToURLScoreMap.get(urlScore.url);
							existingUrl.score += score;
							if (this.trace)
								System.out.println("Adding " + score + " to score of " + existingUrl.url + " (total of: " + existingUrl.score + ")");
							if (!queue.remove(existingUrl))
								throw new RuntimeException("This urlScore should be in the queue!");
							queue.add(existingUrl);
						}
					}
				}
			}
		}
	}
	
	private String getBaseFromURL(String url) {
		int lastIndexOfForwardSlash = url.lastIndexOf('/');
		return url.substring(0, lastIndexOfForwardSlash+1);
	}
		
	private void processRobot(URL url) {
		String host = url.getHost();
		if (this.robotSafeHostsMap.containsKey(host))
			return;
		String strRobot = "http://" + host + "/robots.txt";
		Page robot = null;
		try {
			robot = request(new URL(strRobot));
			if (robot == null) {
				this.robotSafeHostsMap.put(host, new ArrayList<String>(Arrays.asList("/")));
				return;
			}
			
			processRobotHelper(robot.content, host);
			return;

		} catch (IOException e) {
			if (e instanceof MalformedURLException) {
				if (this.trace)
					System.out.println("URL " + strRobot + " was malformed!");
				this.robotSafeHostsMap.put(host, new ArrayList<String>(Arrays.asList("/")));
				return;
			}
			//page doesn't exit, so all good
//			if (this.trace)
//				System.out.println("No robots.txt file found");
			this.robotSafeHostsMap.put(host, new ArrayList<String>());
			return;
		}
		
	}
	
	private boolean processRobotHelper(String robotContent, String host) {
		//We are only interested in the Disallow's for user-agent: *
		int indexOfUserAgentStar = robotContent.toLowerCase(Locale.US).indexOf(USER_AGENT_STAR.toLowerCase(Locale.US));
		int endOfUserAgentStar = robotContent.toLowerCase(Locale.US).indexOf(USER_AGENT.toLowerCase(Locale.US), indexOfUserAgentStar+USER_AGENT_STAR.length());
		if (endOfUserAgentStar == -1)
			endOfUserAgentStar = robotContent.length();
		
		robotContent = robotContent.substring(indexOfUserAgentStar, endOfUserAgentStar);
		
		int lastDisallowIndex = 0;
		while ((lastDisallowIndex = robotContent.indexOf(DISALLOW, lastDisallowIndex)) != -1) {
			lastDisallowIndex += DISALLOW.length();
			String badPaths = robotContent.substring(lastDisallowIndex);
			StringTokenizer st = new StringTokenizer(badPaths);
			
			if (!st.hasMoreTokens())
				break;
			
			String badPath = st.nextToken();
			if (this.robotSafeHostsMap.containsKey(host)) {
				this.robotSafeHostsMap.get(host).add(badPath);
			} else {
				this.robotSafeHostsMap.put(host, new ArrayList<String>(Arrays.asList(badPath)));
			}
		}
		return true;
	}
	
	public boolean robotSafe(URL url) {
		processRobot(url);
		String host = url.getHost();
		if (!this.robotSafeHostsMap.containsKey(host) || this.robotSafeHostsMap.get(host) == null) {
			if (this.trace)
				System.out.println("Something went wrong with robot.txt file");
			return false;
		}
		List<String> badPaths = this.robotSafeHostsMap.get(host);
		for (String badPath : badPaths) {
			if (url.getPath().startsWith(badPath)) {
				return false;
			}
		}
		return true;		
	}
	
	public List<Link> getLinks(String content, String base) {
		Document document = Jsoup.parse(content, base);
		Elements links = document.select("a");
		List<Link> linkList = new ArrayList<Link>();
		for (Element link : links) {
			String url = link.absUrl("href");
//			if (!url.endsWith("html")) {
//				continue;
//			}
			String anchor = link.text();
			linkList.add(new Link(url, anchor));
		}
		return linkList;
	}
		
	private Page request(URL url) throws IOException {
		URLConnection uc = url.openConnection();
		uc.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:19.0) Gecko/20100101 Firefox/19.0");
		uc.setRequestProperty("Accept", "text/html");
		InputStream is = uc.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		String content = "";
		while ((line = br.readLine()) != null) {
			content += line + "\n";
		}
		String newUrlString = url.toString().replace("/", "_").replace(":", "");
		return new Page(newUrlString, content);
	}
		
	private void writeToFile(String name, String content) {
		String fileName = this.path + name;
		try {
			Writer writer = new BufferedWriter(new FileWriter(fileName));
			writer.write(content);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
		
	public  int findWordInArrayNotCaseSensitive(String[] array, String elem) {
		for (int i=0; i<array.length; i++) {
			String string = array[i];
			if (string.toLowerCase(Locale.US).equals(elem.toLowerCase(Locale.US))) {
				return i;
			}
		}		
		return -1;
	}
		
	public int findStringsInArray(String[] array, String words) {
		String[] wordsArray = words.split(" ");
		for (int i=0; i<array.length; i++) {
			if (!array[i].equals(wordsArray[0])) {
				continue;
			}
			for (int j=1; j<=wordsArray.length; j++) {
				if (j==wordsArray.length)
					return i;
				if (!array[i+j].equals(wordsArray[j])) {
					break;
				}
			}
		}
		return -1;
	}
		
	public double score(Link link, Page page, String query) {
		if (query == null)
			return 0;
		String[] words = query.split(" ");
		int k = 0;
		for (String word : words) {
			if (link.anchor.toLowerCase(Locale.US).contains(word.toLowerCase(Locale.US)))
				k++;
		}
		if (k > 0)
			return k*50;
		for (String word : words) {
			if (link.url.toLowerCase(Locale.US).contains(word.toLowerCase(Locale.US)))
				k++;
		}
		if (k > 0)
			return 40;
		Document doc = Jsoup.parse(page.content);
		String docTextLowerCaseWithoutSpecialChars = doc.text().replaceAll("[^a-zA-Z0-9 ]", "").toLowerCase(Locale.US);
		String[] wordsInDocText = docTextLowerCaseWithoutSpecialChars.split(" ");
		Set<String> setOfGoodQueryWords = new HashSet<String>();
		for (String word : words) {
			String anchorLowerCaseWithoutSpecialChars = link.anchor.replaceAll("[^a-zA-Z0-9 ]", "").toLowerCase(Locale.US);
			int indexOfStartAnchor = findStringsInArray(wordsInDocText, anchorLowerCaseWithoutSpecialChars);
			int indexOfEndAnchor = indexOfStartAnchor + link.anchor.split(" ").length-1;
			for (int i=1; i<=distanceFromQueryWords; i++) {
				int less = Math.max(0,indexOfStartAnchor-i);
				int more = Math.min(wordsInDocText.length-1, indexOfEndAnchor+i);
				String lessLC = wordsInDocText[less].toLowerCase(Locale.US);
				String moreLC = wordsInDocText[more].toLowerCase(Locale.US);
				if (word.toLowerCase(Locale.US).equals(lessLC) || word.toLowerCase(Locale.US).equals(moreLC))
					setOfGoodQueryWords.add(word);
			}
		}
		Set<String> setOfQueryWords = new HashSet<String>();
		for (String word : words) {
			if (findWordInArrayNotCaseSensitive(wordsInDocText, word) != -1) {
				setOfQueryWords.add(word);
			}
		}
		return 4*setOfGoodQueryWords.size() + (setOfQueryWords.size() - setOfGoodQueryWords.size());
				
	}
		
	private boolean checkArgs(String[] args) throws ParseException {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(this.options, args);
		HelpFormatter formatter = new HelpFormatter();
		if (cmd.hasOption("u") && cmd.hasOption("q")
				&& cmd.hasOption("q") && cmd.hasOption("docs")) {
			this.url = cmd.getOptionValue("u");
			this.query = cmd.getOptionValue("q");
			this.path = cmd.getOptionValue("docs");
			if (this.path.charAt(this.path.length()-1) != '/') {
				this.path += "/";
			}
			if (cmd.hasOption("m")) {
				try {
					this.maxNumOfPages = Integer.parseInt(cmd.getOptionValue("m"));
				} catch (NumberFormatException e) {
					//print usage
					formatter.printHelp("Crawler", this.options);
					return false;
				}
			}
			if (cmd.hasOption("t")) {
				this.trace = true;
			}
		} else {
			formatter.printHelp("Crawler", this.options);
			return false;
		}
		return true;
	}
		
	public static void main(String[] args) {
		try {
			new Crawler().run(args);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

}
