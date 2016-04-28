import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class Update {
	private Options options;
	private String url;
	private String query;
	private String indexPath;
	private int maxNumOfPages;
	private boolean trace;
	private Crawler crawler;
	
	public Update(String[] args) {
		initOptions();
		if (!checkArgs(args))
			System.exit(1);
	}
	
	public void go() throws FileNotFoundException {
		File lastPageFile = new File(this.indexPath + Crawler.LAST_PAGE_FILENAME);
		if (lastPageFile.exists() && lastPageFile.isFile()) {
			Scanner scanner = new Scanner(lastPageFile);
			this.url = scanner.useDelimiter("\\Z").next();
			log.info("Recovered last Page File. Last Page was: " + this.url);
			scanner.close();
		}
		String parameters = String.format("StartingURL: %s, Query: %s, IndexerPath: %s, MaxNumPages: %d",
				this.url, this.query, this.indexPath, this.maxNumOfPages);
		log.info("Initializing Update with parameters: " + parameters);
		this.crawler = new Crawler(this.url, this.query, this.maxNumOfPages, this.indexPath, this.trace);
		this.crawler.runCrawler();
	}
	
	private void initOptions() {
		this.options = new Options();
		this.options.addOption(Option.builder("u").required(false).hasArg().desc("URL argument").build());
		this.options.addOption(Option.builder("q").required(false).hasArg().desc("Query").build());
		this.options.addOption(Option.builder("i").required(false).hasArg().desc("IndexerPath").build());
		this.options.addOption(Option.builder("m").required(false).hasArg().desc("Max number of pages").build());
		this.options.addOption(Option.builder("t").required(false).hasArg(false).desc("Trace").build());
	}
	
	private boolean checkArgs(String[] args) {
		HelpFormatter formatter = new HelpFormatter();
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(this.options, args);
			if (cmd.hasOption("u")) {
				this.url = cmd.getOptionValue("u");
			} else {
				this.url = Crawler.DEFAULT_STARTING_WEBPAGE;
			}
			if (cmd.hasOption("q")) {
				this.query = cmd.getOptionValue("q");
			} else {
				this.query = Crawler.DEFAULT_QUERY;
			}
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
			formatter.printHelp("Update", this.options);
			return false;
		}
		return true;
	}

	public static void main(String[] args) {
		Update update = new Update(args);
		try {
			update.go();
		} catch (FileNotFoundException e) {
			log.error(e.getCause());
		}
	}

}
