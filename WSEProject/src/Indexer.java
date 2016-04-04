
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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class Indexer {
	
	public static final String CONTENTS = "contents";
	public static final String ABSURL = "absURL";
	public static final String TITLE = "title";
	public static final String INDEXER_DEFAULT_NAME = "indexer";
	public static final String DATA_DEFAULT = "data";
	public static final String INDEXER_MAP_FILENAME = "indexer_map";
	public final String INDEXER_MAP_PATH;
	public IndexWriter writer;
	private IndexerMap indexerMap;
	private ObjectMapper mapper;
	
	public Indexer(String indexPath) {
		INDEXER_MAP_PATH = indexPath + INDEXER_MAP_FILENAME;
//		System.out.println(INDEXER_MAP_PATH);
		File indexerMapFile = new File(INDEXER_MAP_PATH);
		this.mapper = new ObjectMapper();
		if (indexerMapFile.exists() && !indexerMapFile.isDirectory()) {
			try {
				System.out.println("recovering map");
				this.indexerMap = this.mapper.readValue(indexerMapFile, IndexerMap.class);
			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			this.indexerMap = new IndexerMap();
		}
		try {
			this.writer = getBasicIndexWriter(indexPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void serializeIndexerMap() {
		try {
			this.mapper.writeValue(new File(INDEXER_MAP_PATH), this.indexerMap);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * TODO: work on this. if it contains, check if the page has been updated. If it has, then see by how much.
	 * Also consider adding a 
	 * 
	 */
	private boolean isPageInIndex(Page page) {
		return this.indexerMap.map.containsKey(page.getLink().getAbsUrl());
	}
	
	public void indexPage(Page page) {
		if (isPageInIndex(page)) {
			System.out.println("Avoided indexing page with url: " + page.getLink().getAbsUrl());
			return;
		}
		try {
			indexBody(page.getContent(), page.getLink().getAbsUrl(), page.getTitle());
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.indexerMap.addToMap(page.getLink().getAbsUrl(), page);
	}
		
	private IndexWriter getBasicIndexWriter(String indexPath) throws IOException {
		Path indPath = Paths.get(indexPath);
		Directory indexDir = FSDirectory.open(indPath);
		return new IndexWriter(indexDir, new IndexWriterConfig(new StandardAnalyzer()));
	}

	public void indexBody(String body, String absUrl, String title) throws IOException {
		Document doc = new Document();
		if (body != null) {
			doc.add(new TextField(CONTENTS, body, Field.Store.YES));
		}
		if (title != null) {
			doc.add(new TextField(TITLE, title, Field.Store.YES));
		}
		doc.add(new StringField(ABSURL, absUrl, Field.Store.YES));
		this.writer.addDocument(doc);		
	}

}
