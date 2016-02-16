import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

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

public class Retriever {
	
	public String go(String indexerName, String... queryArgs) throws IOException, ParseException {
		if (indexerName == null) {
			indexerName = Indexer.INDEXER_DEFAULT_NAME;
		}
		String queryString = "";
		for (String str : queryArgs) {
			queryString += str + " ";
		}
		queryString.trim();
		if (queryString.length() == 0) {
			System.out.println("Query is empty");
			return "<h3>No results were found</h3>";
		}
		System.out.println("Searching for " + queryString);
		Path indexPath = Paths.get(indexerName);
		Directory indexDir = FSDirectory.open(indexPath);
		IndexReader reader = DirectoryReader.open(indexDir);
		IndexSearcher indexSearcher = new IndexSearcher(reader);
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser contentsParser = new QueryParser(Indexer.CONTENTS, analyzer);
		QueryParser titleParser = new QueryParser(Indexer.TITLE, analyzer);
		Query contentsQuery = contentsParser.parse(queryString);
		Query titleQuery = titleParser.parse(queryString);
		TopDocs contentsDocs = indexSearcher.search(contentsQuery, 10);
		TopDocs titleDocs = indexSearcher.search(titleQuery, 10);
		TopDocs[] docsArray = {contentsDocs, titleDocs};
		TopDocs docs = TopDocs.merge(10, docsArray);
		int in = 0;
		StringBuffer result = new StringBuffer();
		result.append("<h3>Results:</h3>");
		for (ScoreDoc doc : docs.scoreDocs) {
			in++;
			int docId = doc.doc;
			Document d = indexSearcher.doc(docId);
			String title = d.get(Indexer.TITLE);
			String filename =  d.get(Indexer.FILENAME);
			result.append(String.format("<p><b>%d: %s</b><br \\> %s<p>", in, title, filename));
			System.out.println(String.format("<p><b>%d: %s</b><br \\> %s<p>", in, title, filename));			
		}
		return result.toString();
	}
	
	public static void main(String[] args) {
		try {
			String indexerName = args[0];
			String[] queryArgs = Arrays.copyOfRange(args, 1, args.length);
			new Retriever().go(indexerName, queryArgs);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

}
