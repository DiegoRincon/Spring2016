
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


public class Indexer {
	
	private HTMLParser htmlParser;
	public static final String CONTENTS = "contents";
	public static final String FILENAME = "filename";
	public static final String TITLE = "title";
	public static final String INDEXER_DEFAULT_NAME = "indexer";
	public static final String DATA_DEFAULT = "data";
	
	public Indexer() {
		this.htmlParser = new HTMLParser();
	}
	
	public void go(String indexDir, String dataDir) throws IOException {
		
		long start = System.nanoTime();
		int numIndexed = index(indexDir, dataDir);
		System.out.println("Indexing " + numIndexed + " files took " + (System.nanoTime() - start) + " milliseconds");
	}
	
	public int index(String indexPath, String dataPath) throws IOException {
				
		//Create the index
		Path indPath = Paths.get(indexPath);
		Directory indDir = FSDirectory.open(indPath);
				
		IndexWriter writer = new IndexWriter(indDir, new IndexWriterConfig(new StandardAnalyzer()));
		File dataDir = new File(dataPath);
		indexDirectory(writer, dataDir);
		int numIndex = writer.numDocs();
		writer.close();
		return numIndex;
	}
	
	public void indexDirectory(IndexWriter writer, File dir) throws IOException {
		if (!dir.isDirectory()) {
			System.err.println("Input not a directory");
			System.exit(1);
		}
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				indexDirectory(writer, file);
			} else if (file.getName().endsWith(".html") || (file.getName().endsWith(".htm"))) {
				indexFile(writer, file);
			}
		}
	}
	
	public void indexFile(IndexWriter writer, File file) throws IOException {
		if (file.isHidden() || !file.exists() || !file.canRead()) {
			return;
		}
		
		System.out.println("Indexing " + file.getCanonicalPath());
		Document doc = new Document();
		String body = this.htmlParser.getBody(file);
		if (body != null) {
			doc.add(new TextField(CONTENTS, this.htmlParser.getBody(file), Field.Store.YES));
		}
		String title = this.htmlParser.getTitle(file);
		if (title != null) {
			doc.add(new TextField(TITLE, this.htmlParser.getTitle(file), Field.Store.YES));
		}
		doc.add(new StringField(FILENAME, file.getPath(), Field.Store.YES));
		writer.addDocument(doc);
	}
	
	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("Usage...");
			System.exit(1);
		}
		
		try {
			new Indexer().go(args[0], args[1]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
