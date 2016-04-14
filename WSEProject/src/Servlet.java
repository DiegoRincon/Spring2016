import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.queryparser.classic.ParseException;

public class Servlet extends HttpServlet {
	private static final long serialVersionUID = 8778174121968337225L;
	private static final String SEARCH_FORM =
			  "<form method=\"get\" action=\"crawl\">"
			+ "		<input type=\"text\" name=\"url\" required placeholder=\"Starting URL\" />"
			+ "		<input type=\"text\" name=\"query\" required placeholder=\"Query\" />"
			+ "		<input type=\"number\" name=\"maxPages\" required placeholder=\"MaxNumPages\" />"
			+ "		<input type=\"submit\" name=\"crawl\" value=\"crawl\" />"
			+ "</form>"
			+ "<form method=\"get\" action=\"search\">"
			+ "		<input type=\"text\" name=\"query\" required placeholder=\"Query\" />"
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
			result = processSearch(query);
		}
		
		response.setContentType("text/html;charset=UTF-8");

		out.println("<!DOCTYPE html>");
		out.println("<html><head>");
		out.println("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
		out.println("<title>Crawl results</title></head>");
		out.println("<body>");
		out.println(SEARCH_FORM);
		out.println(result);
		out.println("</body>");
		out.println("</html>");
		out.close();
	}
	
	//TODO: Add a class field IndexWriter to pass to the Crawler and Indexer (for searching)

	public String processSearch(String query) throws IOException {
		// Set the response message's MIME type
		// Allocate a output writer to write the response message into the network socket

		// Write the response message, in an HTML page
		StringBuffer sb = new StringBuffer();
		
		// Echo client's request information
		sb.append("<p>Your query was: " + query + "</p>");
		try {
			String indexerPathString = getServletContext().getRealPath("/WEB-INF/Indexer/" + Indexer.INDEXER_DEFAULT_NAME);
			Retriever retriever = new Retriever();
			String results = null;
			try {
				results = retriever.go(indexerPathString, query);
			} catch (IOException e) {
				e.printStackTrace();
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

		StringBuffer sb = new StringBuffer();
		// Echo client's request information
		int maxPages = 50;
		if (maxNumPages == null) {
			try {
				maxPages = Integer.parseInt(maxNumPages);
			} catch (NumberFormatException e) {
				maxPages = 50;
			}
		}
		String indexerPathString = getServletContext().getRealPath("/WEB-INF/Indexer/" + Indexer.INDEXER_DEFAULT_NAME);
		Crawler crawler = new Crawler(startingUrl, query, maxPages, indexerPathString, true);
		double time = crawler.crawl();

		sb.append("<p>The indexer can be found at: " + indexerPathString + "</p>");
		sb.append("<p>Your crawler took: " + time + " sec. to finish</p>");
		
		return sb.toString();
	}

}
