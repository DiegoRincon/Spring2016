package nyu.crawler.data;
import lombok.Data;
import nyu.crawler.indexer.Indexer;

import org.apache.lucene.document.Document;

@Data
public class DocScore {
	private Document document;
	private float score;
	//-1 if not set
	private double realScore;
	private String absUrl;
	
	public DocScore(Document document, float score) {
		this.document = document;
		this.score = score;
		this.absUrl = document.get(Indexer.ABSURL);
		this.realScore = -1;
	}
}
