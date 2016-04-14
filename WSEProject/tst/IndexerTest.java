import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Test;

public class IndexerTest {

	@Test
	public void test() throws IOException, ParseException {
		Indexer indexer = new Indexer(System.getProperty("user.dir")+"/indexerTest/");
		String absUrl = "absUrl";
		indexer.indexBody("body", absUrl, "title");
		String results = indexer.search("body");
		System.out.println(results);
		indexer.updateDocument("body", absUrl, "titleModified");
		results = indexer.search("body");
		System.out.println(results);
		indexer.writer.close();
		
	}

}
