import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class Retriever {
	
	public String go(String indexerPath, String... queryArgs) throws IOException, ParseException {

		//TODO: Add a text snippet to the results from the retriever
		if (indexerPath == null) {
			indexerPath = Indexer.INDEXER_DEFAULT_NAME;
		}
		String queryString = "";
		for (String str : queryArgs) {
			queryString += str + " ";
		}
		queryString.trim();
		if (queryString.length() == 0) {
			return null;
		}
		log.info("Searching for " + queryString);
		Path indexPath = Paths.get(indexerPath);
		Directory indexDir = FSDirectory.open(indexPath);
		IndexReader reader = DirectoryReader.open(indexDir);
		IndexSearcher indexSearcher = new IndexSearcher(reader);
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser contentsParser = new QueryParser(Indexer.CONTENTS, analyzer);
		Query contentsQuery = contentsParser.parse(queryString);
		TopDocs docs = indexSearcher.search(contentsQuery, 10);
		int resNum = 0;
		StringBuffer result = new StringBuffer();
		if (docs.scoreDocs.length == 0) {
			result.append("<h3>No results were found</h3>");
			return result.toString();
		}
		result.append("<h3>Results:</h3>");
		for (ScoreDoc doc : docs.scoreDocs) {
			resNum++;
			int docId = doc.doc;
			Document d = indexSearcher.doc(docId);
			String title = d.get(Indexer.TITLE);
			String absURL =  d.get(Indexer.ABSURL);
			result.append(String.format("<div><b>%d: <a href=\"%s\">%s</a></b><br> <a href=\"%s\">%s</a></div>", resNum, absURL, title, absURL, absURL));
			System.out.println(String.format("<div><b>%d: <a href=\"%s\">%s</a></b><br> <a href=\"%s\">%s</a></div>", resNum, absURL, title, absURL, absURL));			
		}
		reader.close();
		return result.toString();
	}
	
	public static void main(String[] args) {
		try {
			if (args.length < 1) {
				System.err.println("Usage: <query>");
				System.exit(1);
			}
			String indexerDir = System.getProperty("user.dir") + "/indexer/";
//			String indexerDir = args[0];
			String[] queryArgs = args;
			new Retriever().go(indexerDir, queryArgs);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

}
