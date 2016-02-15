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

public class Retriever {
	
	public void go(String[] args) throws IOException, ParseException {;
		String queryString = "";
		for (int i=1; i<args.length; i++) {
			String str = args[i];
			queryString += str + " ";
		}
		queryString.trim();
		if (queryString.length() == 0) {
			System.out.println("Query is empty");
			return;
		}
		System.out.println("Searching for " + queryString);
		String indexerName = args[0];
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
		for (ScoreDoc doc : docs.scoreDocs) {
			in++;
			int docId = doc.doc;
			Document d = indexSearcher.doc(docId);
			String title = d.get(Indexer.TITLE);
			String filename =  d.get(Indexer.FILENAME);
			System.out.println(String.format("<p><b>%d: %s</b><br \\> %s<p>", in, title, filename));			
		}
	}
	
	public static void main(String[] args) {
		try {
			new Retriever().go(args);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

}
