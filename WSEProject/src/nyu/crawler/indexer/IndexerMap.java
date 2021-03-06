package nyu.crawler.indexer;
import java.util.HashMap;
import java.util.Map;

import nyu.crawler.data.Page;

public class IndexerMap {
	//AbsUrl to Page
	public Map<String, Page> map;
	
	public IndexerMap() {
		this.map = new HashMap<String, Page>();
	}
	
	public void addToMap(String key, Page value) {
		this.map.put(key, value);
	}
	
	public boolean removeFromMap(String key) {
		return this.map.remove(key) != null;
	}
	
}
