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
		this.crawler = new Crawler(this.url, this.query, this.maxNumOfPages, this.indexPath, this.trace);
	}
	
	public void go() {
		String parameters = String.format("StartingURL: %s, Query: %s, IndexerPath: %s, MaxNumPages: %d",
				this.url, this.query, this.indexPath, this.maxNumOfPages);
		log.info("Initializing Update with parameters: " + parameters);
		this.crawler.runCrawler();
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

	public static void main(String[] args) {
		Update update = new Update(args);
		update.go();
	}

}
