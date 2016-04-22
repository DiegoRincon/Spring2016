import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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
import org.jsoup.Jsoup;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@AllArgsConstructor
public class Retriever {
	public static final int DEFAULT_NUM_OF_RESULTS = 50;
	public static final int DEFAULT_SNIPPET_SIZE = 40;
	private static final int DEFAULT_WORDS_BEFORE_HIT = 5;
	private static final String STOPWORDS_FILE_PATH = "stopwordslist.txt";
	private String indexerPath;
	
	private String getQuery(String... queryArgs) {
		String queryString = "";
		for (String str : queryArgs) {
			queryString += str + " ";
		}
		queryString.trim();
		return queryString;
	}
	
	public String getResultsAsHtml(int maxNumDocs, String... queryArgs) throws IOException, ParseException {
		//TODO: Add a text snippet to the results from the retriever
		Set<Document> docs = getResultsFromQuery(maxNumDocs, queryArgs);
		StringBuffer result = new StringBuffer();
		if (docs.size() == 0) {
			result.append("<h3>No results were found</h3>");
			return result.toString();
		}
		int resNum = 0;
		result.append("<h3>Results:</h3>");
		for (Document doc : docs) {
			resNum++;
			String title = doc.get(Indexer.TITLE);
			String absURL =  doc.get(Indexer.ABSURL);
			result.append(String.format("<div><b>%d: <a href=\"%s\">%s</a></b><br> <a href=\"%s\">%s</a></div>", resNum, absURL, title, absURL, absURL));
			String snippet = getSnippet(doc.get(Indexer.CONTENTS), getQuery(queryArgs));
			result.append(String.format("<div>%s</div>", snippet));
			System.out.println(String.format("<div><b>%d: <a href=\"%s\">%s</a></b><br> <a href=\"%s\">%s</a></div>", resNum, absURL, title, absURL, absURL));
			System.out.println(String.format("<div>%s</div>", snippet));
		}
		return result.toString();
	}

	public String getResultsAsHtml(String... queryArgs) throws IOException, ParseException {
		return getResultsAsHtml(DEFAULT_NUM_OF_RESULTS, queryArgs);
	}
	
	public Set<String> getResultsAsAbsUrls(String... queryArgs) throws IOException, ParseException {
		return getResultsAsAbsUrls(DEFAULT_NUM_OF_RESULTS, queryArgs);
	}
	
	public Set<String> getResultsAsAbsUrls(int maxNumDocs, String... queryArgs) throws IOException, ParseException {
		Set<Document> docs = getResultsFromQuery(maxNumDocs, queryArgs);
		Set<String> docsUrls = new HashSet<String>(docs.size());
		for (Document doc : docs) {
			docsUrls.add(doc.get(Indexer.ABSURL));
		}
		return docsUrls;
	}
	
	public Set<Document> getResultsFromQuery(String... queryArgs) throws IOException, ParseException {
		return getResultsFromQuery(DEFAULT_NUM_OF_RESULTS, queryArgs);
	}
	
	public Set<Document> getResultsFromQuery(int maxNumDocs, String... queryArgs)  throws IOException, ParseException {
		String queryString = getQuery(queryArgs);
		if (queryString.length() == 0) {
			return null;
		}
		log.info("Searching for " + queryString);
		Path indexPath = Paths.get(this.indexerPath);
		Directory indexDir = FSDirectory.open(indexPath);
		IndexReader reader = DirectoryReader.open(indexDir);
		IndexSearcher indexSearcher = new IndexSearcher(reader);
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser contentsParser = new QueryParser(Indexer.CONTENTS, analyzer);
		Query contentsQuery = contentsParser.parse(queryString);
		TopDocs docs = indexSearcher.search(contentsQuery, maxNumDocs);
		Set<Document> setOfDocs = new HashSet<Document>();
		for (ScoreDoc doc : docs.scoreDocs) {
			int docId = doc.doc;
			Document document = indexSearcher.doc(docId);
			setOfDocs.add(document);
		}
		reader.close();
		return setOfDocs;
	}
	
	public String getResultsPageRank(Map<String, Page> map, Set<Page> pageCollection, double f, int numOfDocs, String... queryArgs) throws IOException, ParseException {
		Set<String> absUrls = getResultsAsAbsUrls(queryArgs);
		WeightedPageRank pageRank = new WeightedPageRank(pageCollection, f);
		Set<Page> selectedPages = getPagesFromAbsUrls(absUrls, pageRank.getAbsUrlToPageMap());
		SortedSet<Page> sortedSet = pageRank.runPageRank();
		reduceSetOfPages(sortedSet, selectedPages);
		return getSortedSetOfPagesAsHtml(getFirstKElementsFromSet(sortedSet, numOfDocs), queryArgs);
	}
	
	public String getResultsPageRank(IndexerMap indexerMap, double f, int numOfDocs, String... queryArgs) throws IOException, ParseException {
		Set<Page> pageCollection = new HashSet<Page>(indexerMap.map.values());
		return getResultsPageRank(indexerMap.map, pageCollection, f, numOfDocs, queryArgs);
	}
	
	private SortedSet<Page> getFirstKElementsFromSet(SortedSet<Page> set, int numOfDocs) {
		SortedSet<Page> sortedSet = new TreeSet<Page>(new Comparator<Page>() {
			@Override
			public int compare(Page o1, Page o2) {
				if (o1.getScore() < o2.getScore())
					return 1;
				if (o1.getScore() > o2.getScore())
					return -1;
				return 0;
			}			
		});
		int count = 0;
		for (Page page : set) {
			if (count >= numOfDocs)
				break;
			sortedSet.add(page);
			count++;
		}
		return sortedSet;
	}
	
	private String getSortedSetOfPagesAsHtml(SortedSet<Page> sortedSet, String... queryArgs) {
		int resNum = 0;
		StringBuffer sb = new StringBuffer();
		if (sortedSet.isEmpty()) {
			sb.append("<h3>No results were found</h3>");
			return sb.toString();
		}
		sb.append("<h3>Results:</h3>");
		for (Page page : sortedSet) {
			resNum++;
			String title = page.getTitle();
			String absURL = page.getLink().getAbsUrl();
			sb.append(String.format("<div><b>%d: <a href=\"%s\">%s</a></b><br> <a href=\"%s\">%s</a></div>", resNum, absURL, title, absURL, absURL));
			org.jsoup.nodes.Document doc = Jsoup.parse(page.getContent());
			String snippet = getSnippet(doc.text(), getQuery(queryArgs));
			sb.append(String.format("<div>%s</div>", snippet));
		}
		return sb.toString();
	}
	
	public void reduceSetOfPages(Set<Page> setToBeReduced, Set<Page> set) {
		Iterator<Page> it = setToBeReduced.iterator();
		while (it.hasNext()) {
			Page page = it.next();
			if (!set.contains(page))
				it.remove();
		}
	}
	
	public String addHtmlTagsToSnippet(String snippet, String queryString) {
		StringBuffer sb = new StringBuffer(snippet);
		//TODO: Assumes query will be well formed. Enforce this assumption!! (e.g. no punctuation)
		int indexOfQuery = sb.indexOf(queryString);
		if (indexOfQuery != -1) {
			while (indexOfQuery != -1) {
				String replacement = "<b>" + queryString + "</b>";
				sb.replace(indexOfQuery, indexOfQuery+queryString.length(), replacement);
				indexOfQuery = sb.indexOf(queryString, sb.indexOf(replacement, indexOfQuery) + replacement.length());
			}
			return sb.toString();
		}
		Set<String> queries = new HashSet<String>(Arrays.asList(queryString.split(" ")));
		for (String q : queries) {
			int index = sb.indexOf(q);
			while (index != -1) {
				String replacement = "<b>" + q + "</b>";
				sb.replace(index, index+q.length(), replacement);
				index = sb.indexOf(q, sb.indexOf(replacement, index) + replacement.length());
			}
		}
		return sb.toString();
	}
	
	public String queryIsFoundEntirely(StringBuffer sb, String[] contentArray, String[] queryArray) {
		int minus = DEFAULT_WORDS_BEFORE_HIT;
		int it = 0;
		while (true) {
			it++;
			int indexFirstWordOfQueryInContent = Math.max(0, findInArray(contentArray, queryArray[0], 0));
			int begin = Math.max(0, indexFirstWordOfQueryInContent - minus);
			String joinedBeginString = joinStringsWithSpace(contentArray, begin, indexFirstWordOfQueryInContent);
			int end = Math.min(contentArray.length, indexFirstWordOfQueryInContent + minus);
			String joinedEndString = joinStringsWithSpace(contentArray, indexFirstWordOfQueryInContent, end);
			String tempResult = sb.subSequence(sb.indexOf(joinedBeginString), sb.indexOf(joinedEndString)+joinedEndString.length()).toString();
			String[] lengthOfTempResult = tempResult.split(" ");
			if (lengthOfTempResult.length > DEFAULT_SNIPPET_SIZE || it > 15) {
				return tempResult;
			} else {
				minus++;
			}
		}
	}
	
	public String queryNotFoundEntirely(StringBuffer sb, String[] contentArray, String[] queryArray) {
		String[] queryNoStopWords = removeStopWords(queryArray);
		Set<String> queries = new HashSet<String>(Arrays.asList(queryNoStopWords));
		List<Integer> indices = new ArrayList<Integer>(); 
		for (String q : queries) {
			int start = 0;
			int index = findInArray(contentArray, q, start);
			while (index != -1) {
				indices.add(index);
				start = index+1;
				index = findInArray(contentArray, q, start);
			}
		}
		if (indices.isEmpty()) {
			return joinStringsWithSpace(contentArray, 0, DEFAULT_SNIPPET_SIZE);
		}
		Collections.sort(indices);
		int min = indices.get(0);
		int max = indices.get(indices.size()-1);
		if (max - min < DEFAULT_SNIPPET_SIZE) {
			while (true) {
				String result = joinStringsWithSpace(contentArray, Math.max(0, min), Math.min(max, contentArray.length-1));
				if (result.split(" ").length > DEFAULT_SNIPPET_SIZE) {
					return result;
				} else {
					min--;
					max++;
				}
			}
		} else {
			int oddThreshold = 7;
			List<Interval> intervals = new ArrayList<Interval>();
//			while (buffer.toString().split(" ").length < DEFAULT_SNIPPET_SIZE) {
				intervals.clear();
				for (int i=0; i<indices.size()-1; i++) {
					int index1 = indices.get(i);
					int index2 = indices.get(i+1);
					if (index2 - index1 > oddThreshold) {
						intervals.add(new Interval(Math.max(0, index1-oddThreshold/4),
								Math.min(contentArray.length,index1+oddThreshold/4)));
						intervals.add(new Interval(Math.max(0, index2-oddThreshold/4),
								Math.min(contentArray.length,index2+oddThreshold/4)));
					} else {
						intervals.add(new Interval(Math.max(0, index1-oddThreshold/4),
								Math.min(contentArray.length,index2+oddThreshold/4)));
					}
				}
				oddThreshold += 2;
//			}
			intervals = reduceIntervals(intervals, 3);
			String snippet = getStringFromIntervals(intervals, contentArray);
			return snippet;
		}
	}
	
	public String getStringFromIntervals(List<Interval> intervals, String[] contentArray) {
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<intervals.size()-1 ; i++) {
			Interval interval = intervals.get(i);
			sb.append(joinStringsWithSpace(contentArray, interval.start, interval.end));
			sb.append(" ... ");
		}
		Interval interval = intervals.get(intervals.size()-1);
		sb.append(joinStringsWithSpace(contentArray, interval.start, interval.end));
		return sb.toString();
	}
	
	public List<Interval> reduceIntervals(List<Interval> intervals, int limit) {
		List<Interval> reducedIntervals = new ArrayList<Interval>();
		Collections.sort(intervals, new Comparator<Interval>(){

			@Override
			public int compare(Interval o1, Interval o2) {
				if (o1.start > o2.start)
					return 1;
				if (o1.start < o2.start)
					return -1;
				return 0;
			}
			
		});
		for (int i=0; i<intervals.size(); i++) {
			Interval interval1 = intervals.get(i);
			Interval interval2 = intervals.get(Math.min(i+1, intervals.size()-1));
			if (!intervalsOverlap(interval1, interval2, limit)) {
				reducedIntervals.add(interval1);
				continue;
			}
			while (intervalsOverlap(interval1, intervals.get(Math.min(i+1, intervals.size()-1)), limit)) {
				interval1 = new Interval(Math.min(interval1.start, interval2.start), Math.max(interval1.end, interval2.end));
				i++;
				if (i >= intervals.size())
					break;
			}
			reducedIntervals.add(interval1);
		}
		return reducedIntervals;
	}
	
	private boolean intervalsOverlap(Interval interval1, Interval interval2, int limit) {
		if (interval1.end + limit < interval2.start || interval2.end + limit < interval1.start) {
			return false;
		}
		return true;					
	}
	
	public String getSnippet(String content, String queryString) {
		String reducedSnippet = reduceToSnippetSize(content, queryString);
		return addHtmlTagsToSnippet(reducedSnippet, queryString);
	}
	
	public String reduceToSnippetSize(String content, String queryString) {
		StringBuffer sb = new StringBuffer(content);
		int indexQueryString = sb.indexOf(queryString);
		String[] contentArray = sb.toString().split(" ");
		String[] queryArray = queryString.split(" ");
		if (indexQueryString != -1) {
			return queryIsFoundEntirely(sb, contentArray, queryArray);
		} else {
			return queryNotFoundEntirely(sb, contentArray, queryArray);
		}
	}
	
	private int findInArray(String[] array, String word, int start) {
		for (int i=start; i<array.length; i++) {
			if (array[i].replaceAll("[^a-zA-Z0-9 ]", "").equals(word)) {
				return i;
			}
		}
		return -1;
	}
	
	private Set<String> getStopWords() throws IOException {
		Set<String> stopWords = new HashSet<String>();
		BufferedReader br = new BufferedReader(new FileReader(STOPWORDS_FILE_PATH));
		String line;
		while ((line = br.readLine()) != null) {
			stopWords.add(line);
		}
		br.close();
		return stopWords;
	}
	
	public String[] removeStopWords(String[] query) {
		try {
			Set<String> stopWords = getStopWords();
			List<String> list = new ArrayList<String>();
			for (String q : query) {
				if (!stopWords.contains(q))
//					list.add(q);
					list.add(q.replaceAll("[^a-zA-Z0-9]", ""));
			}
			return list.toArray(new String[0]);
		} catch (IOException e) {
			log.error("Couldn't retrieve stop words");
			return query;
		}
	}
	
	private String joinStringsWithSpace(String[] array, int begin, int end) {
		StringBuffer sb = new StringBuffer();
		for (int i=begin; i<=end; i++) {
			sb.append(array[i] + " ");
		}
		return sb.toString();
	}
	
	private Set<Page> getPagesFromAbsUrls(Set<String> absUrls, Map<String, Page> map) {
		Set<Page> pages = new HashSet<Page>(absUrls.size());
		for (String absUrl : absUrls) {
			Page page = map.get(absUrl);
			if (page == null)
				throw new RuntimeException("There is no such page!!");
			pages.add(page);
		}
		return pages;
	}
	
	public static void main(String[] args) {
		

		
		try {
			if (args.length < 1) {
				System.err.println("Usage: <query>");
				System.exit(1);
			}
			String indexerDir = System.getProperty("user.dir") + "/indexer/";
			String indexerMapPath = indexerDir + Indexer.INDEXER_MAP_FILENAME;
			IndexerMap indexerMap = Indexer.getIndexerMapFromFile(indexerMapPath);
			String[] queryArgs = args;
			Retriever retriever = new Retriever(indexerDir);
			String result = retriever.getResultsPageRank(indexerMap,
					Crawler.DEFAULT_F,
					Crawler.DEFAULT_NUM_OF_DOCS,
					queryArgs);
			System.out.println(result.replaceAll("(</div>)", "$1\n"));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

}
