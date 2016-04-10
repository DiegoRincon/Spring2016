import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;

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

public class WeightedPageRank {
	private int nextPageId;
	private Options options;
	private String path;
	private double f;
	private double epsilon;
	private Set<Page> pageCollection;
	private Map<Integer, Page> idToPageMap;
	private Map<String, Page> absUrlToPageMap;
	private static final String[] IMPORTANT_SCOPES = {"H1", "H2", "H3", "H4", "em", "b"};
	private static final String DEFAULT_PROTOCOL = "http://";
	private static final String SECURE_PROTOCOL = "https://";
	private static final double EPSILON_PARAM = 0.01;
	private boolean isLocal = false;
	
	public WeightedPageRank() {
		initOptions();
		this.nextPageId = 0;
		this.idToPageMap = new HashMap<Integer, Page>();
		this.absUrlToPageMap = new HashMap<String, Page>();
		this.pageCollection = new HashSet<Page>();
	}
	
	public void go(String[] args){
		if (!checkArgs(args)) {
			System.exit(1);
		} else {
			try {
				if (this.isLocal)
					runPageRankLocal();
				else
					runPageRank();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException er) {
				if (er.getCause() instanceof MalformedURLException) {
					System.err.println("A url was malformed!");
					MalformedURLException err = (MalformedURLException) er.getCause();
					System.err.println("Problem was: " + err);
					
				}
			}
		}
	}
	
	private void runPageRank() throws IOException {
		String content = request(this.path);
		
//		String content2 = new Scanner(new File("test.txt")).useDelimiter("\\Z").next();
		//This should be a set to discount duplicates
		List<Link> collection = getLinks(content, getBaseFromURL(this.path));
		confirmAllLinksThatExist(collection);
		int numPages = collection.size();
		this.epsilon = EPSILON_PARAM/numPages;
		for (Link link : collection) {
			processLinks(link);
		}
		double sum = 0;
		for (Page page : this.pageCollection) {
			double base = phiPage(page);
			page.setBase(base);
			sum += base;
		}
		for (Page page : this.pageCollection) {
			double normalizedScore = page.getBase()/sum;
			page.setScore(normalizedScore);
			page.setBase(normalizedScore);
		}
		double[][] weights = getWeights(numPages);
//		System.out.println(Arrays.deepToString(weights));
		runWeightedPageRank(weights);
		SortedSet<Page> sortedPages = new TreeSet<Page>(new Comparator<Page>() {
			@Override
			public int compare(Page o1, Page o2) {
				if (o1.getScore() < o2.getScore())
					return 1;
				if (o1.getScore() > o2.getScore())
					return -1;
				return 0;
			}			
		});
		sortedPages.addAll(this.pageCollection);
		for (Page page : sortedPages) {
			System.out.println(page.getLink().getUrl() + ": " + page.getScore());
		}
	}
	
	private void runPageRankLocal() throws IOException {
		File dir = new File(this.path);
		if (!dir.exists() || !dir.isDirectory()) {
			System.err.println(this.path + " is not a valid directory!");
			System.exit(1);
		}
		List<Link> collection = getLinksDirectoryLocal(new File(this.path));
		int numPages = collection.size();
		this.epsilon = EPSILON_PARAM/numPages;
		for (Link link : collection) {
			processLinksLocal(link);
		}
		double sum = 0;
		for (Page page : this.pageCollection) {
			double base = phiPage(page);
			page.setBase(base);
			sum += base;
		}
		for (Page page : this.pageCollection) {
			double normalizedScore = page.getBase()/sum;
			page.setScore(normalizedScore);
			page.setBase(normalizedScore);
		}
		double[][] weights = getWeights(numPages);
		runWeightedPageRank(weights);
		SortedSet<Page> sortedPages = new TreeSet<Page>(new Comparator<Page>() {
			@Override
			public int compare(Page o1, Page o2) {
				if (o1.getScore() < o2.getScore())
					return 1;
				if (o1.getScore() > o2.getScore())
					return -1;
				return 0;
			}			
		});
		sortedPages.addAll(this.pageCollection);
		for (Page page : sortedPages) {
			System.out.println(page.getLink().getUrl() + ": " + page.getScore());
		}
	}
	
	private void runWeightedPageRank(double[][] weights) {
		boolean changed = true;
		while (changed) {
			changed = false;
			for (Page page : this.pageCollection) {
				double newScore = (1-this.f)*page.getBase() + this.f*getSumWeightsFromPage(weights, page);
				page.setNewScore(newScore);
				if (Math.abs(page.getScore() - page.getNewScore()) > this.epsilon) {
					changed = true;
				}
			}
			for (Page page : this.pageCollection) {
				page.setScore(page.getNewScore());
			}
		}
	}
	
	private double getSumWeightsFromPage(double[][] weights, Page page) {
		double sum = 0;
		for (Page q : this.pageCollection) {
			sum += q.getScore()*weights[page.getId()][q.getId()];
		}
		return sum;
	}
	
	private double[][] getWeights(int numPages) {
		double[][] weights = new double[numPages][numPages];
		for (Page page : this.pageCollection) {
			Set<Link> outLinks = page.getOutLinks();
			if (outLinks.isEmpty()) {
				for (Page page2 : this.pageCollection) {
					weights[page2.getId()][page.getId()] = 1.0/numPages;
				}
			} else {
				double sum = 0;
				for (Link link : outLinks) {
					Page q = this.absUrlToPageMap.get(link.getAbsUrl());
					if (q == null) {
						throw new NoSuchElementException();
					}
					double weightPages = thetaPage(page, q);
//					System.out.println(page.getLink().getUrl() + " --> " + q.getLink().getUrl() + "  = " + weightPages);
					weights[q.getId()][page.getId()] = weightPages;
					sum += weightPages;
				}
				for (Link link : outLinks) {
					Page q = this.absUrlToPageMap.get(link.getAbsUrl());
					double newWeight = weights[q.getId()][page.getId()]/sum;
					weights[q.getId()][page.getId()] = newWeight;
				}
			}
		}
		return weights;
	}
	
	private double thetaPage(Page p, Page q) {
		double score = 0;
		Set<Link> visited = new HashSet<Link>();
		for (Link link : p.getOutLinks()) {
			if (link.getUrl().equals(q.getLink().getUrl())) {
				score+=getNumOccurencesOfSubstringInString(link.getUrl(), p.getContent());
				for (String scope : IMPORTANT_SCOPES) {
					score += isStringInsideScope(link.getAnchor(), p.getContent(), scope);
				}
				visited.add(link);
			}
		}
		return score;
	}
	
	private int getNumOccurencesOfSubstringInString(String substring, String string) {
		int lastIndex = 0;
		int count = 0;
		while (lastIndex != -1) {
			lastIndex = string.indexOf(substring, lastIndex);
			if (lastIndex != -1) {
				count++;
				lastIndex += substring.length();
			}
		}
		return count;
	}
	
	private int isStringInsideScope(String link, String text, String scope) {
		Pattern pattern = Pattern.compile("<"+scope+">(.*>?)</"+scope+">");
		Matcher matcher = pattern.matcher(text);
		int count = 0;
		while (matcher.find()) {
			if (matcher.group(1).contains(link)) {
				count++;
			}
		}
		return count;
	}
	
	private double phiPage(Page page) {
		return Math.log(getNumberOfWordsInPage(page))/Math.log(2);
	}
	
	private void processLinks(Link link) throws IOException {
		String content = request(link.getAbsUrl());
		List<Link> links = getLinks(content, getBaseFromURL(link.getAbsUrl()));
		Page page = new Page(getNewId(), link, new HashSet<Link>(links), content);
		this.pageCollection.add(page);
		this.idToPageMap.put(page.getId(), page);
		this.absUrlToPageMap.put(page.getLink().getAbsUrl(), page);
	}
	
	private void processLinksLocal(Link link) throws IOException {
		Scanner sc = new Scanner(new File(link.getAbsUrl()));
		String content = sc.useDelimiter("\\Z").next();
		sc.close();
//		String content = request(link.getAbsUrl());
		List<Link> links = getLinksLocal(content, this.path);
		Page page = new Page(getNewId(), link, new HashSet<Link>(links), content);
		this.pageCollection.add(page);
		this.idToPageMap.put(page.getId(), page);
		this.absUrlToPageMap.put(page.getLink().getAbsUrl(), page);
	}
	
	public boolean checkIfExists(String urlString) throws IOException {
		final URL url = new URL(urlString);
		HttpURLConnection huc = (HttpURLConnection)url.openConnection();
		huc.setRequestMethod("HEAD");
		try {
			int responseCode = huc.getResponseCode();
			if (responseCode != 200) {
				return false;
			}
		} catch (UnknownHostException e) {
			return false;
		}
		return true;		
	}
	
	public void confirmAllLinksThatExist(List<Link> links) {
		try {
			Iterator<Link> iterator = links.iterator();
			while (iterator.hasNext()) {
				Link link = iterator.next();
				if (!checkIfExists(link.getAbsUrl())) {
					iterator.remove();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private int getNewId() {
		return this.nextPageId++;
	}
	
	private int getNumberOfWordsInPage(Page page) {
//		return page.getContent().split("[ \n\r]+").length;
		return getTextFromPage(page).split("[ \n\r]+").length;
	}
	
	private String getTextFromPage(Page page) {
		String content = page.getContent();
		Document doc = Jsoup.parse(content);
		return doc.text();
	}
	
	private void initOptions() {
		this.options = new Options();
		this.options.addOption(Option.builder("docs").required().hasArg().desc("url to collection").build());
		this.options.addOption(Option.builder("f").required().hasArg().desc("F value in (0,1)").build());
		this.options.addOption(Option.builder("l").required(false).hasArg(false).desc("Add if the docs is a loca dir").build());
//		this.options.addOption("docs", true, "Path");
//		this.options.addOption("f", true, "F value in (0,1)");
	}
	
	private boolean checkArgs(String[] args) {
		HelpFormatter formatter = new HelpFormatter();
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(this.options, args);
			this.path = cmd.getOptionValue("docs");
			if (this.path.charAt(this.path.length()-1) != '/') {
				this.path += "/";
			}
			this.f = Double.parseDouble(cmd.getOptionValue("f"));
			if (cmd.hasOption("l")) {
				this.isLocal = true;
			}
		} catch (ParseException e) {
			formatter.printHelp("Weighted PageRank", this.options);
			return false;
		} catch (NumberFormatException e) {
			formatter.printHelp("Weighted PageRank", this.options);
			System.err.println("f must be a double");
			return false;
		}
		if (this.f >= 1 || this.f <= 0) {
			formatter.printHelp("Weighted PageRank", this.options);
			System.err.println("f must be in (0,1)");
			return false;
		}
		return true;
	}
	
	private List<Link> getLinks(String content, String base) {
		Document document = Jsoup.parse(content, base);
		Elements links = document.select("a");
		List<Link> linkList = new ArrayList<Link>();
		for (Element link : links) {
			String url = link.attr("href");
			String absUrl = link.absUrl("href");
			if (!absUrl.endsWith("html")) {
				continue;
			}
			String anchor = link.text();
			linkList.add(new Link(url, anchor, absUrl));
		}
		return linkList;
	}
	
	private List<Link> getLinksLocal(String content, String base) {
		Document document = Jsoup.parse(content, base);
		Elements links = document.select("a");
		List<Link> linkList = new ArrayList<Link>();
		for (Element link : links) {
			String url = link.attr("href");
			String absUrl = this.path + url;
//			String absUrl = link.absUrl("href");
			if (!absUrl.endsWith("html")) {
				continue;
			}
			String anchor = link.text();
			linkList.add(new Link(url, anchor, absUrl));
		}
		return linkList;
	}
	
	private List<Link> getLinksDirectoryLocal(File directory) {
		List<Link> linkList = new ArrayList<Link>();
		File[] listOfFiles = directory.listFiles();
		for (File file : listOfFiles) {
			linkList.add(new Link(file.getName(), null, file.getParent()+'/'+file.getName()));
		}
		
		return linkList;
	}
	
	public static String request(String urlString) throws IOException {
		Document doc = Jsoup.connect(urlString).get();
		String str = doc.toString();
		return str;
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
	
	public static void main(String[] args) {
		new WeightedPageRank().go(args);
	}
}
