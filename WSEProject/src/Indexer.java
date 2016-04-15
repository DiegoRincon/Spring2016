
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Log4j2
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
		if (indexPath.charAt(indexPath.length()-1) != '/')
			indexPath += '/';
		INDEXER_MAP_PATH = indexPath + INDEXER_MAP_FILENAME;
		File indexerMapFile = new File(INDEXER_MAP_PATH);
		this.mapper = new ObjectMapper();
		if (indexerMapFile.exists() && !indexerMapFile.isDirectory()) {
			try {
				log.info("recovering map");
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
	
	public void closeWriter() {
		try {
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String search(String... queryArgs) throws IOException, ParseException {
		//TODO: Add a text snippet to the results from the retriever
		String queryString = "";
		for (String str : queryArgs) {
			queryString += str + " ";
		}
		queryString.trim();
		if (queryString.length() == 0) {
			return null;
		}
		log.info("Searching for " + queryString);
		IndexReader reader = DirectoryReader.open(this.writer, false);
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

	public void serializeIndexerMap() {
		try {
			this.mapper.writerWithDefaultPrettyPrinter().writeValue(new File(INDEXER_MAP_PATH), this.indexerMap);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean pageNeedsUpdate(Page newPage) {
		Page oldPage = this.indexerMap.map.get(newPage.getLink().getAbsUrl());
		if (oldPage == null)
			return true;
		Duplicate duplicate = new Duplicate();
		boolean pagesAreNearDuplicate = duplicate.arePagesNearDuplicates(newPage, oldPage);
		return !pagesAreNearDuplicate;
	}
	
	private boolean isPageInIndex(Page page) {
		return this.indexerMap.map.containsKey(page.getLink().getAbsUrl());
	}
	
	//Don't index if page is in map AND the content is not similar.
	public void indexPage(Page page) {
		if (isPageInIndex(page)) {
			if (pageNeedsUpdate(page)) {
				try {
					updateDocument(page.getContent(), page.getLink().getAbsUrl(), page.getTitle());
					this.indexerMap.addToMap(page.getLink().getAbsUrl(), page);
					log.info("Update page with url: " + page.getLink().getAbsUrl());
					return;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			log.info("Avoided indexing page with url: " + page.getLink().getAbsUrl());
			return;
		}
		try {
			log.info("Indexing Page: " + page.getLink().getAbsUrl());
			indexBody(page.getContent(), page.getLink().getAbsUrl(), page.getTitle());
		} catch (IOException e) {
			log.error("Error indexing Page: " + page.getLink().getAbsUrl() + " cause " + e.getCause());
		}
		this.indexerMap.addToMap(page.getLink().getAbsUrl(), page);
	}
		
	private IndexWriter getBasicIndexWriter(String indexPath) throws IOException {
		Path indPath = Paths.get(indexPath);
		Directory indexDir = FSDirectory.open(indPath);
		IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
		return new IndexWriter(indexDir, config);
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
//		this.writer.commit();
	}
		
	public void updateDocument(String body, String absUrl, String title) throws IOException {
		Document doc = new Document();
		if (body != null) {
			doc.add(new TextField(CONTENTS, body, Field.Store.YES));
		}
		if (title != null) {
			doc.add(new TextField(TITLE, title, Field.Store.YES));
		}
		doc.add(new StringField(ABSURL, absUrl, Field.Store.YES));
		this.writer.updateDocument(new Term(ABSURL, absUrl), doc);
//		this.writer.commit();
	}

}
