package nyu.crawler.servlet;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.queryparser.classic.ParseException;

import lombok.extern.log4j.Log4j2;
import nyu.crawler.crawler.Crawler;
import nyu.crawler.indexer.Indexer;
import nyu.crawler.indexer.IndexerMap;
import nyu.crawler.retriever.Retriever;

@Log4j2
public class Servlet extends HttpServlet {
	private static final long serialVersionUID = 8778174121968337225L;
	private static final String SEARCH_FORM =
			  "<form method=\"get\" action=\"crawl\">"
			+ "		<input type=\"text\" name=\"url\" required placeholder=\"Starting URL\" />"
			+ "		<input type=\"text\" name=\"query\" required placeholder=\"Query\" />"
			+ "		<input type=\"number\" min=\"1\" name=\"maxPages\" required placeholder=\"MaxNumPages\" />"
			+ "		<input type=\"submit\" name=\"crawl\" value=\"crawl\" />"
			+ "</form>"
			+ "<form method=\"get\" action=\"search\">"
			+ "		<input type=\"text\" name=\"query\" required placeholder=\"Query\" />"
			+ "     <input type=\"number\" name=\"numDocs\" min=\"1\" required placeholder=\"Max # Of Pages to Return\" />"
			+ "		<input type=\"submit\" name=\"search\" value=\"search\" />"
			+ "</form>";

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		//TODO: try to implement this asynchronously
//		final AsyncContext context = request.startAsync();
		
		PrintWriter out = response.getWriter();
		String result = null;
		if (request.getParameter("crawl") != null) {
			String query = request.getParameter("query");
			String startingUrl = request.getParameter("url");
			String maxNumPages = request.getParameter("maxPages");
			result = processCrawl(query, startingUrl, maxNumPages);
		}
		
		if (request.getParameter("search") != null) {
			String query = request.getParameter("query");
			String maxNumPagesString = request.getParameter("numDocs");
			int maxNumPages = Crawler.DEFAULT_NUM_OF_DOCS;
			try {
				maxNumPages = Integer.parseInt(maxNumPagesString);
			} catch (NumberFormatException e) {
				log.error("entered wrong number format for num of docs. Defaulting to " + maxNumPages);
			}
			result = processSearch(query, maxNumPages);
		}
		
		response.setContentType("text/html;charset=UTF-8");

		out.println("<!DOCTYPE html>");
		out.println("<html><head>");
		out.println("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
		out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"css/general.css\">");
		out.println("<title>Crawl results</title></head>");
		out.println("<body>");
		out.println(SEARCH_FORM);
		out.println("<div id=\"content\">");
		out.println(result);
		out.println("</div>");
		out.println("</body>");
		out.println("</html>");
		out.close();
	}
	

	public String processSearch(String query, int maxNumDocs) throws IOException {

		// Write the response message, in an HTML page
		log.info("Searching for query: " + query);
		StringBuilder sb = new StringBuilder();
		
		// Echo client's request information
		sb.append("<p>Your query was: " + query + "</p>");
		try {
			String indexerPathString = getServletContext().getRealPath("/WEB-INF/Indexer/" + Indexer.INDEXER_DEFAULT_NAME);
			Retriever retriever = new Retriever(indexerPathString);
			String indexerMapPath = indexerPathString + "/" + Indexer.INDEXER_MAP_FILENAME;
			IndexerMap indexerMap = Indexer.getIndexerMapFromFile(indexerMapPath);
			String results = null;
			try {
				results = retriever.getResultsPageRank(indexerMap,
						Crawler.DEFAULT_F,
						maxNumDocs,
						query.split(" "));
//				results = retriever.getResultsAsHtml(indexerPathString, query);
			} catch (IOException e) {
				log.error("There was a problem searching for " + query + " returned error message. Cause " + e.getCause());
			}
			if (results != null) {
				sb.append(results);
			} else {
				sb.append("Oops! There was a problem fetching the results!");
			}
		} catch (ParseException e) {
			sb.append("Oops! There was a problem parsing the query!");
		}
		return sb.toString();
	}

	public String processCrawl(String query, String startingUrl, String maxNumPages) throws IOException {

		StringBuilder sb = new StringBuilder();
		// Echo client's request information
		int maxPages = 50;
		if (maxNumPages != null) {
			try {
				maxPages = Integer.parseInt(maxNumPages);
			} catch (NumberFormatException e) {
				maxPages = 50;
			}
		}
		
		String indexerPathString = getServletContext().getRealPath("/WEB-INF/Indexer/" + Indexer.INDEXER_DEFAULT_NAME);
		String parameters = String.format("StartingUrl: %s, Query: %s, MaxNumPages: %d, IndexerPath: %s",
				startingUrl, query, maxPages, indexerPathString);
		log.info("Crawling with parameters: " + parameters);
		Crawler crawler = new Crawler(startingUrl, query, maxPages, indexerPathString, true);
		double time = crawler.runCrawler();

		sb.append("<p>Your crawler took: " + time + " sec. to finish</p>");
		
		return sb.toString();
	}

}
