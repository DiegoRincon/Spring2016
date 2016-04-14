import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class Crawler {
	public static final int distanceFromQueryWords = 5;
    public static final String DISALLOW = "Disallow:";
    public static final String USER_AGENT_STAR = "user-agent: *";
    public static final String USER_AGENT = "user-agent:";
	public static final int DEFAULT_MAX_NUM_PAGES = 50;
	public static final String DEFAULT_STARTING_URL = "http://cs.nyu.edu/";
	public static final String DEFAULT_PROTOCOL = "http://";
	public static final String SECURE_PROTOCOL = "https://";
	public Options options;
	public String url;
	public String query;
	public String indexPath;
	public int maxNumOfPages;
	public boolean trace;
	private Object robotSafeLock;
	private Map<String, List<String>> robotSafeHostsMap;
	private Object seenLock;
	private Set<String> seen;
	private Object mapHeapLock;
	private PriorityQueue<URLScore> urlScoreQueue;
	private Map<String, URLScore> urlToURLScoreMap;
	private Object numThreadsLock;
	private Object pageCollectionLock;
	private Set<Page> pageCollection;
	private int numThreads;
	private Object pageIdLock;
	private Object indexerLock;
	private Indexer indexer;
	private Random random;
	
	public Crawler(String[] args) {
		this.trace = false;
		this.maxNumOfPages = DEFAULT_MAX_NUM_PAGES;
		initOptions();
		if (!checkArgs(args)) {
			System.exit(1);
		}
//		this.pageId = 0;
		this.robotSafeHostsMap = new HashMap<String, List<String>>();
		this.urlScoreQueue = new PriorityQueue<URLScore>(1000, Collections.reverseOrder());
		this.urlToURLScoreMap = new HashMap<String, URLScore>();
		this.pageCollection = new HashSet<Page>();
		this.indexer = new Indexer(this.indexPath);
		this.indexerLock = new Object();
		this.seen = new HashSet<String>();
		this.robotSafeLock = new Object();
		this.mapHeapLock = new Object();
		this.seenLock = new Object();
		this.numThreadsLock = new Object();
		this.pageIdLock = new Object();
		this.pageCollectionLock = new Object();
		this.random = new Random();
	}
	
	public Crawler(String url, String query, int maxNumPages, String indexerPath, boolean trace) {
		this.trace = trace;
		this.indexPath = indexerPath;
		this.maxNumOfPages = maxNumPages;
		this.url = url;
		this.query = query;
//		this.pageId = 0;
		this.robotSafeHostsMap = new HashMap<String, List<String>>();
		this.urlScoreQueue = new PriorityQueue<URLScore>(1000, Collections.reverseOrder());
		this.urlToURLScoreMap = new HashMap<String, URLScore>();
		this.pageCollection = new HashSet<Page>();
		this.indexer = new Indexer(this.indexPath);
		this.indexerLock = new Object();
		this.seen = new HashSet<String>();
		this.robotSafeLock = new Object();
		this.mapHeapLock = new Object();
		this.seenLock = new Object();
		this.numThreadsLock = new Object();
		this.pageIdLock = new Object();
		this.pageCollectionLock = new Object();
		this.random = new Random();
	}

	private void initOptions() {
		this.options = new Options();
		this.options.addOption(Option.builder("u").required().hasArg().desc("URL argument").build());
		this.options.addOption(Option.builder("q").required().hasArg().desc("Query").build());
		this.options.addOption(Option.builder("i").required(false).hasArg().desc("IndexerPath").build());
		this.options.addOption(Option.builder("m").required(false).hasArg().desc("Max number of pages").build());
		this.options.addOption(Option.builder("t").required(false).hasArg(false).desc("Trace").build());
	}

	private boolean checkArgs(String[] args) {
		HelpFormatter formatter = new HelpFormatter();
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(this.options, args);
			this.url = cmd.getOptionValue("u");
			this.query = cmd.getOptionValue("q");
			if (cmd.hasOption("i")) {
				this.indexPath = cmd.getOptionValue("i");
				if (this.indexPath.charAt(this.indexPath.length()-1) != '/')
					this.indexPath += '/';
			} else {
				this.indexPath = System.getProperty("user.dir") + "/indexer/";
			}
			if (cmd.hasOption("m")) {
				try {
					this.maxNumOfPages = Integer.parseInt(cmd.getOptionValue("m"));
				} catch (NumberFormatException e) {
					formatter.printHelp("Crawler", this.options);
					return false;
				}
			}
			if (cmd.hasOption("t")) {
				this.trace = true;
			}			
		} catch (ParseException e) {
			formatter.printHelp("Crawler", this.options);
			return false;
		}
		return true;
	}
	
	public void run() {
		String parameters = String.format("StartingURL: %s, Query: %s, IndexerPath: %s, MaxNumPages: %d",
				this.url, this.query, this.indexPath, this.maxNumOfPages);
		log.info("Starting Crawler with parameters: " + parameters);
		long start = System.nanoTime();
		startCrawlerConcurrent();
		try {
			this.indexer.search(this.query);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (org.apache.lucene.queryparser.classic.ParseException e) {
			e.printStackTrace();
		}
		this.indexer.serializeIndexerMap();
		log.info("Total time elapsed: " + (System.nanoTime() - start)/1000000000.0 + " seconds");
	}
	
	public double runCrawler() {
		String parameters = String.format("StartingURL: %s, Query: %s, IndexerPath: %s, MaxNumPages: %d",
				this.url, this.query, this.indexPath, this.maxNumOfPages);
		log.info("Starting Crawler with parameters: " + parameters);
		long start = System.nanoTime();
		startCrawlerConcurrent();
		this.indexer.serializeIndexerMap();
		double time = (System.nanoTime() - start)/1000000000.0;
		log.info("Total time elapsed: " + time + " seconds");
		return time;
	}
	
	private boolean hasReachedMaxCapacitySynchronized() {
		synchronized (this.seenLock) {
			return this.seen.size() > this.maxNumOfPages;
		}
	}
	
	private URLScore getBestUrlScoreAndRemoveFromMapSynchronized() {
		synchronized(this.mapHeapLock) {
			URLScore urlScoreQueue =  this.urlScoreQueue.poll();
			URLScore urlScoreMap = this.urlToURLScoreMap.remove(urlScoreQueue.getLink().getAbsUrl());
			if (!urlScoreQueue.equals(urlScoreMap)) {
				log.error(urlScoreQueue);
				log.error(urlScoreMap);
				throw new RuntimeException("URLScore's should be equal!");
			}
			return urlScoreQueue;
		}
	}
	
	private boolean hasBeenVisitedSynchronized(String url) {
		synchronized(this.seenLock) {
			return this.seen.contains(url);
		}
	}
	
	private boolean isInURLScoreMapSynchronized(String url) {
		synchronized (this.mapHeapLock) {
			return this.urlToURLScoreMap.containsKey(url);
		}
	}
	
	private boolean isQueueEmptySynchronized() {
		synchronized (this.mapHeapLock) {
			return this.urlScoreQueue.isEmpty();
		}
	}
	
	private void addNumThreadsSynchronized() {
		synchronized (this.numThreadsLock) {
			this.numThreads++;
		}
	}
	
	private void subNumThreadsSynchronized() {
		synchronized (this.numThreadsLock) {
			this.numThreads--;
		}
	}
	
	private boolean areThereThreadsOngoing() {
		synchronized (this.numThreadsLock) {
			return this.numThreads > 0;
		}
	}
	
	public void startCrawlerConcurrent() {
		URLScore originalURLScore = new URLScore(new Link(this.url, "", this.url, getUniqueURL(this.url)), 0);

		this.urlScoreQueue.add(originalURLScore);

		this.urlToURLScoreMap.put(this.url, originalURLScore);
		
		ExecutorService executor = Executors.newCachedThreadPool();
		CompletionService<Boolean> ecs = new ExecutorCompletionService<Boolean>(executor);
		
		List<Future<Boolean>> listOfResults = new ArrayList<Future<Boolean>>();
		boolean started = true;
		boolean keepRunning = true;
		while (keepRunning) {
			while (isQueueEmptySynchronized() && areThereThreadsOngoing()) {
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (!areThereThreadsOngoing() && isQueueEmptySynchronized()) {
				keepRunning = false;
				continue;
			}
			
			if (hasReachedMaxCapacitySynchronized()) {
				keepRunning = false;
				continue;
			}
			
			URLScore bestURL = getBestUrlScoreAndRemoveFromMapSynchronized();
			
			if (hasBeenVisitedSynchronized(bestURL.getLink().getUniqueUrl()))
				continue;
			
			Future<Boolean> future = ecs.submit(new CrawlerCallable(bestURL));
			listOfResults.add(future);
			//Needed so that the crawler doesn't stop at the start
			//Need to add a delay perhaps
			if (started) {
				started = false;
				try {
					future.get();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		}
		for (int i = 0; i < listOfResults.size(); i++) {
			try {
				ecs.take().get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}		
		executor.shutdown();
	}
	
	public void closeWriter() {
		this.indexer.closeWriter();
	}
	
	private boolean addToPageCollectionSynchronized(Page page) {
		synchronized (this.pageCollectionLock) {
			return this.pageCollection.add(page);
		}
	}
	
	private String getNewIdSynchronized() {
		synchronized (this.pageIdLock) {
			return new UUID(random.nextLong(), random.nextLong()).toString();
		}
	}
	
	private Page requestAndProcessLinks(Link link) throws IOException {
		Document html = request(link.getAbsUrl());
		link.setAbsUrl(html.baseUri());
		String content = html.toString();
		Element titleElement = html.select("title").first();
		String title = (titleElement == null) ? link.getUrl() : titleElement.text();		
		List<Link> links = getLinks(content, getBaseFromURL(link.getAbsUrl()));
		Page page = new Page(getNewIdSynchronized(), link, new HashSet<Link>(links), content, title);
		return page;
	}
	
	private class CrawlerCallable implements Callable<Boolean> {
		private URLScore bestURL;
		public CrawlerCallable(URLScore bestURL) {
			this.bestURL = bestURL;
		}

		@Override
		public Boolean call() throws Exception {
			addNumThreadsSynchronized();
			Page page = null;
			try {
				page = requestAndProcessLinks(this.bestURL.getLink());
			} catch (IOException e) {
				//perhaps page doesn't exist!
				if (Crawler.this.trace)
					log.debug("There was a problem fetching " + bestURL.getLink().getAbsUrl() + " (it probably doesn't exist!)");
				synchronized(Crawler.this.seenLock) {
					Crawler.this.seen.add(this.bestURL.getLink().getAbsUrl());					
				}
				subNumThreadsSynchronized();
				return false;
			}
			//If the page is null, we are still interested in marking it as visited
			synchronized(Crawler.this.seenLock) {
				if (Crawler.this.seen.contains(this.bestURL.getLink().getAbsUrl())) {
					return false;
				} else {
					Crawler.this.seen.add(this.bestURL.getLink().getAbsUrl());
				}
			}
			if (page == null)
				return false;
			if (!robotSafe(this.bestURL.getLink().getAbsUrl())) {
				log.info("Told not to go there by the robot.txt file");
				subNumThreadsSynchronized();
				return false;
			}
			addToPageCollectionSynchronized(page);
			if (Crawler.this.trace) {
				log.info("logging: " + "Received: " + this.bestURL.getLink().getAbsUrl());
			}
			synchronized (Crawler.this.indexerLock) {
				Crawler.this.indexer.indexPage(page);
			}
			Set<Link> links = page.getOutLinks();
			for (Link link : links) {
				double score = score(link, page, Crawler.this.query);
				if (!hasBeenVisitedSynchronized(link.getUniqueUrl())) {
					if (!isInURLScoreMapSynchronized(link.getAbsUrl())) {
						URLScore urlScore = new URLScore(link, score);
						handleNewURL(urlScore);
					} else {
						handleExistingURL(link.getAbsUrl(), score);
					}
				}
			}
			subNumThreadsSynchronized();
			return true;
		}

		
		private void handleNewURL(URLScore urlScore) {
			synchronized (Crawler.this.mapHeapLock) {
				Crawler.this.urlScoreQueue.add(urlScore);
				Crawler.this.urlToURLScoreMap.put(urlScore.getLink().getAbsUrl(), urlScore);
			}
		}
		
		private void handleExistingURL(String urlString, double score) {
			synchronized (Crawler.this.mapHeapLock) {
				URLScore existingUrl = Crawler.this.urlToURLScoreMap.get(urlString);
				existingUrl.setScore(existingUrl.getScore() + score);
				if (!Crawler.this.urlScoreQueue.remove(existingUrl)) {
					log.error(String.format("url %s should be in the queue", existingUrl.getLink().toString()));
				}
				Crawler.this.urlScoreQueue.add(existingUrl);
			}
		}

	}
	
	private String getBaseFromURL(String url) {
		String urlMinusProtocol = url;
		if (url.startsWith(DEFAULT_PROTOCOL)) {
			urlMinusProtocol = url.substring(DEFAULT_PROTOCOL.length());
		} else if (url.startsWith(SECURE_PROTOCOL)) {
			urlMinusProtocol = url.substring(SECURE_PROTOCOL.length());
		}
		
		//for cases such as "http://www.nyu.edu"
		int lastIndexOfForwardSlash = (urlMinusProtocol.lastIndexOf('/') == -1) ? urlMinusProtocol.length() - 1 : urlMinusProtocol.lastIndexOf('/');
		
		return DEFAULT_PROTOCOL + urlMinusProtocol.substring(0, lastIndexOfForwardSlash+1);
	}
		
	private void processRobot(String urlString) {
		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		String host = url.getHost();
		synchronized (this.robotSafeLock) {
			if (this.robotSafeHostsMap.containsKey(host))
				return;
		}
		String strRobot = "http://" + host + "/robots.txt";
		if (this.trace)
			log.info("Processing robot for: " + strRobot);
		String robotContent = null;
		try {
			Document robotDoc = request(strRobot);
			robotContent = robotDoc.toString();
			if (robotContent == null) {
				synchronized (this.robotSafeLock) {
					this.robotSafeHostsMap.put(host, new ArrayList<String>(Arrays.asList("/")));
					return;
				}
			}
			
			processRobotHelper(robotContent, host);
			return;

		} catch (IOException e) {
			if (e instanceof MalformedURLException) {
				if (this.trace)
					log.debug("URL " + strRobot + " was malformed!");
				synchronized (this.robotSafeLock) {
					this.robotSafeHostsMap.put(host, new ArrayList<String>(Arrays.asList("/")));
					return;
				}
			}
			//page doesn't exit, so all good
			synchronized (this.robotSafeLock) {
				this.robotSafeHostsMap.put(host, new ArrayList<String>());
				return;
			}
		}
		
	}
	
	private boolean processRobotHelper(String robotContent, String host) {
		//We are only interested in the Disallow's for user-agent: *
		int indexOfUserAgentStar = robotContent.toLowerCase(Locale.US).indexOf(USER_AGENT_STAR.toLowerCase(Locale.US));
		int endOfUserAgentStar = robotContent.toLowerCase(Locale.US).indexOf(USER_AGENT.toLowerCase(Locale.US), indexOfUserAgentStar+USER_AGENT_STAR.length());
		if (endOfUserAgentStar == -1)
			endOfUserAgentStar = robotContent.length();
		
		if (indexOfUserAgentStar == -1)
			indexOfUserAgentStar = 0;
		
		robotContent = robotContent.substring(indexOfUserAgentStar, endOfUserAgentStar);
		
		int lastDisallowIndex = 0;
		while ((lastDisallowIndex = robotContent.indexOf(DISALLOW, lastDisallowIndex)) != -1) {
			lastDisallowIndex += DISALLOW.length();
			String badPaths = robotContent.substring(lastDisallowIndex);
			StringTokenizer st = new StringTokenizer(badPaths);
			
			if (!st.hasMoreTokens())
				break;
			
			String badPath = st.nextToken();
			synchronized (this.robotSafeLock) {
				if (this.robotSafeHostsMap.containsKey(host)) {
					this.robotSafeHostsMap.get(host).add(badPath);
				} else {
					this.robotSafeHostsMap.put(host, new ArrayList<String>(Arrays.asList(badPath)));
				}
			}
		}
		return true;
	}
	
	public boolean robotSafe(String urlString) {
		processRobot(urlString);
		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		String host = url.getHost();
		List<String> badPaths;
		synchronized (this.robotSafeLock) {
			if ((badPaths = this.robotSafeHostsMap.get(host)) == null) {
				if (this.trace)
					log.error("Something went wrong with robot.txt file");
				return false;
			}
			for (String badPath : badPaths) {
				if (url.getPath().startsWith(badPath)) {
					return false;
				}
			}
		}

		return true;		
	}
	
	public List<Link> getLinks(String content, String base) {
		Document document = Jsoup.parse(content, base);
		Elements links = document.select("a[href]");
		List<Link> linkList = new ArrayList<Link>();
		for (Element link : links) {
			String url = link.attr("href");
			String absUrl = link.absUrl("href");
			if (absUrl.isEmpty())
				continue;
			if (!isUrlValid(absUrl)) {
				continue;
			}
			String anchor = link.text();
			String uniqueUrl = getUniqueURL(absUrl);
			linkList.add(new Link(url, anchor, absUrl, uniqueUrl));
		}
		return linkList;
	}
	
	private boolean isUrlValid(String absUrl) {
		Pattern hashPatern = Pattern.compile("http.*#[a-zA-Z0-9%_-]+$");
		Matcher matcher = hashPatern.matcher(absUrl);
		if (matcher.matches())
			return false;
		return true;
//		return absUrl.endsWith("html");
	}
	
	private String getUniqueURL(String absUrl) {
		String url = normalizeURL(absUrl);
		if (url==null)
			return absUrl;
		if (url.startsWith(DEFAULT_PROTOCOL)) {
			return url.substring(DEFAULT_PROTOCOL.length());
		} else if (url.startsWith(SECURE_PROTOCOL)) {
			return url.substring(SECURE_PROTOCOL.length());
		}
		
		return url;
	}
	
	public static String normalizeURL(String urlString) {
		try {
			URL url = new URI(urlString).normalize().toURL();
			final String path = url.getPath().replace("/$", "");
			String semiNormalizedString = url.getProtocol().toLowerCase()
					+ "://" + url.getHost().toLowerCase()
					+ (url.getPort() != -1 && url.getPort() != 80 ? ":"
					+ url.getPort() : "")
					+ path
					+ (url.getQuery() == null ? "" : url.getQuery());
			return semiNormalizedString;
		} catch (MalformedURLException e) {
			return null;
		} catch (URISyntaxException e) {
			return null;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
		
	private Document request(String url) throws IOException {
		Document doc = Jsoup.connect(url).followRedirects(true).get();
		String redirect = hasRedirect(doc);
		if (redirect != null) {
			if (this.trace)
				log.info("Redirecting to: " + redirect + " from: " + url);
			doc = Jsoup.connect(redirect).followRedirects(true).get();
		}
		return doc;
	}
	
	private String hasRedirect(Document doc) throws MalformedURLException {
		//looking for stuff like: <meta http-equiv="Refresh" content="0; URL=home/index.html">
		Elements meta = doc.select("html head meta");
		if (meta == null)
			return null;
		String httpEquiv = meta.attr("http-equiv");
		if (httpEquiv != null && httpEquiv.toLowerCase().contains("refresh")) {
			String content = meta.attr("content");
			if (content != null) {
				String[] contentArray = content.split("=");
				if (contentArray.length > 1) {
					URL base = new URL(doc.baseUri());
					return new URL(base, contentArray[1]).toString();
				}
			}
		}
		return null;
	}
		
	public void writeToFile(String name, String content) {
		String fileName = this.indexPath + name;
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
		if (wordsArray.length == 0) {
			return -1;
		}
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
			if (link.getAnchor().toLowerCase(Locale.US).contains(word.toLowerCase(Locale.US)))
				k++;
		}
		if (k > 0)
			return k*50;
		for (String word : words) {
			if (link.getUrl().toLowerCase(Locale.US).contains(word.toLowerCase(Locale.US)))
				k++;
		}
		if (k > 0)
			return 40;
		Document doc = Jsoup.parse(page.getContent());
		String docTextLowerCaseWithoutSpecialChars = doc.text().replaceAll("[^a-zA-Z0-9 ]", "").toLowerCase(Locale.US);
		String[] wordsInDocText = docTextLowerCaseWithoutSpecialChars.split(" ");
		Set<String> setOfGoodQueryWords = new HashSet<String>();
		for (String word : words) {
			String anchorLowerCaseWithoutSpecialChars = link.getAnchor().replaceAll("[^a-zA-Z0-9 ]", "").toLowerCase(Locale.US);
			int indexOfStartAnchor = findStringsInArray(wordsInDocText, anchorLowerCaseWithoutSpecialChars);
			int indexOfEndAnchor = indexOfStartAnchor + link.getAnchor().split(" ").length-1;
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

		
	public static void main(String[] args) {
		Crawler crawler = new Crawler(args);
		crawler.run();
		crawler.closeWriter();
	}

	
}
