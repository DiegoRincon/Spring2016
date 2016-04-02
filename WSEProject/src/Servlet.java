import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.queryparser.classic.ParseException;

public class Servlet extends HttpServlet {
	private static final long serialVersionUID = 8778174121968337225L;
	private static final String SEARCH_FORM = "<form method=\"get\" action=\"search\">"
			+ "<input type=\"text\" name=\"query\" required />"
			+ "<input type=\"submit\" value=\"search\" />"
			+ "</form>";

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		//TODO: try to implement this asynchronously
//		final AsyncContext context = request.startAsync();
		
		// Set the response message's MIME type
		response.setContentType("text/html;charset=UTF-8");
		// Allocate a output writer to write the response message into the network socket
		PrintWriter out = response.getWriter();

		// Write the response message, in an HTML page
		try {
			out.println("<!DOCTYPE html>");
			out.println("<html><head>");
			out.println("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
			out.println("<title>Crawl results</title></head>");
			out.println("<body>");
			out.println(SEARCH_FORM);
			// Echo client's request information
			String query = request.getParameter("query");
			String startingUrl = request.getParameter("url");
			String maxNumPages = request.getParameter("maxPages");
			int maxPages = 50;
			if (maxNumPages == null) {
				try {
					maxPages = Integer.parseInt(maxNumPages);
				} catch (NumberFormatException e) {
					maxPages = 50;
				}
			}
			String indexerPathString = getServletContext().getRealPath("/WEB-INF/Indexer/" + Indexer.INDEXER_DEFAULT_NAME);
			Crawler crawler = new Crawler(startingUrl, query, maxPages, indexerPathString);
			double time = crawler.crawl();
			
			out.println("<p>The indexer can be found at: " + indexerPathString + "</p>");
			out.println("<p>Your crawler took: " + time + " sec. to finish</p>");
			out.println("</body>");
			out.println("</html>");
		} finally {
			out.close();  // Always close the output writer
		}
	}

//	@Override
//	public void doGet(HttpServletRequest request, HttpServletResponse response)
//		   throws IOException, ServletException {
//	   // Set the response message's MIME type
//	   response.setContentType("text/html;charset=UTF-8");
//	   // Allocate a output writer to write the response message into the network socket
//	   PrintWriter out = response.getWriter();
//
//	   // Write the response message, in an HTML page
//	   try {
//		   out.println("<!DOCTYPE html>");
//		   out.println("<html><head>");
//		   out.println("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
//		   out.println("<title>Search Results</title></head>");
//		   out.println("<body>");
//		   out.println(SEARCH_FORM);
//		   // Echo client's request information
//		   String query = request.getParameter("query");
//		   String startingUrl = request.getParameter("url");
//		   String maxNumPages = request.getParameter("maxPages");
//		   out.println("<p>Your query was: " + query + "</p>");
//		   try {
//			   String indexerPathString = getServletContext().getRealPath("/WEB-INF/Indexer/" + Indexer.INDEXER_DEFAULT_NAME);
//			   Path indexerPath = Paths.get(indexerPathString);
//			   if (Files.notExists(indexerPath)) {
//				   String dataPathString = getServletContext().getRealPath("/WEB-INF/Indexer/" + Indexer.DATA_DEFAULT);
//				   new Indexer().go(indexerPathString, dataPathString);
//			   }
//			   String results = new Retriever().go(indexerPathString, query);
//			   if (results != null) {
//				   out.println(results);
//			   }
//		   } catch (ParseException e) {
//			   e.printStackTrace();
//		   }
//		   out.println("</body>");
//		   out.println("</html>");
//	   } finally {
//		   out.close();  // Always close the output writer
//	   }
//   }
}
