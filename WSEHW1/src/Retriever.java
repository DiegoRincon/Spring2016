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
			return null;
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
			String filename =  d.get(Indexer.FILENAME);
			result.append(String.format("<p><b>%d: %s</b><br \\> %s<p>", resNum, title, filename));
			System.out.println(String.format("<p><b>%d: %s</b><br \\> %s<p>", resNum, title, filename));			
		}
		return result.toString();
	}
	
	public static void main(String[] args) {
		try {
			if (args.length < 2) {
				System.err.println("Usage: <indexerDir> <query>");
			}
			String indexerDir = args[0];
			String[] queryArgs = Arrays.copyOfRange(args, 1, args.length);
			new Retriever().go(indexerDir, queryArgs);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

}
