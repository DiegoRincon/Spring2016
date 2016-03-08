import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

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
	public Options options;
	public String url;
	public String query;
	public String path;
	public int maxNumOfPages;
	public boolean trace;
	
	public Crawler() {
		this.options = new Options();
		this.options.addOption("u", true, "URL argument");
		this.options.addOption("q", true, "Query");
		this.options.addOption("docs", true, "Path");
		this.options.addOption("m", true, "Max number of pages");
		this.options.addOption("t", false, "Trace");
		this.trace = false;
		this.maxNumOfPages = 50;
	}
	
	public void run(String[] args) throws ParseException {
		
		if (!checkArgs(args)) {
			System.exit(1);
		} else {
			System.out.println(this.query);
			System.out.println(this.url);
			System.out.println(this.maxNumOfPages);
			System.out.println(this.path);
			System.out.println(this.trace);
		}
		
	}
	
	public void startCrawler() throws IOException {
		PriorityQueue<URLScore> queue = new PriorityQueue<URLScore>(100, new Comparator<URLScore>() {
			@Override
			public int compare(URLScore arg0, URLScore arg1) {
				if (arg0.score > arg1.score)
					return -1;
				if (arg0.score < arg1.score)
					return 1;
				return 0;
			}			
		});
		queue.add(new URLScore(this.url, 0));
		Set<URLScore> seen = new HashSet<URLScore>();
		while (true) {
			URLScore bestURL = queue.poll();
			if (this.trace)
				System.out.println("Requested: " + bestURL.url);
			String htmlPage = request(bestURL.url);
			if (htmlPage != null) {
				seen.add(bestURL);
				//htmlPage is auto added to directory
				if (this.trace)
					System.out.println("Received: " + bestURL.url);
				List<Link> links = getLinks(htmlPage);
				for (Link link : links) {
					double score = score(link, htmlPage, this.query);
				}
			}
		}
		
	}
	
	public List<Link> getLinks(String filename) throws IOException {
		File doc = new File(filename);
		Document document = Jsoup.parse(doc, "UTF-8");
		Elements links = document.select("a[href]");
		List<Link> linkList = new ArrayList<Link>();
		for (Element link : links) {
			String url = link.attr("abs:href");
			String anchor = link.text();
			linkList.add(new Link(url, anchor));
		}
		return linkList;
	}
	
	public String request(String urlString) {
		URL url = null;
		try {
			url = new URL(urlString);
		} catch (Exception e) {
			return null;
		}
		try (InputStream is = url.openStream()) {
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			String content = "";
			while ((line = br.readLine()) != null) {
				content += line;
			}
			String newUrlString = urlString.replace("/", "_");
			writeToFile(newUrlString, content);
			return newUrlString;
		} catch(Exception e) {
			return null;
		}
	}
	
	public void writeToFile(String name, String content) {
		String fileName = this.path + name;
		try {
			Writer writer = new BufferedWriter(new FileWriter(fileName));
			writer.write(content);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public double score(Link link, String page, String query) {
		if (query == null)
			return 0;
		String[] words = query.split(" ");
		int k = 0;
		for (String word : words) {
			if (link.anchor.contains(word))
				k++;
		}
		if (k > 0)
			return k*50;
		for (String word : words) {
			if (link.url.contains(word))
				k++;
		}
		if (k > 0)
			return 40;
				
	}
	
	public boolean checkArgs(String[] args) throws ParseException {
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
