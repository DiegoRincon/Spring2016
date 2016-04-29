package nyu.crawler.retriever;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import lombok.Getter;
import nyu.crawler.data.Link;
import nyu.crawler.data.Page;

public class WeightedPageRank {
	private double f;
	private double epsilon;
	private Set<Page> pageCollection;
	private Map<String, Integer> pageIdToIndexMap;
	@Getter
	private Map<String, Page> absUrlToPageMap;
	private static final String[] IMPORTANT_SCOPES = {"H1", "H2", "H3", "H4", "em", "b"};
	private static final double EPSILON_PARAM = 0.01;
	
	public WeightedPageRank(Set<Page> pageCollection, double f) {
		this.pageCollection = pageCollection;
		this.f = f;
		this.pageIdToIndexMap = new HashMap<String, Integer>();
		this.absUrlToPageMap = new HashMap<String, Page>();
		initPageIdToIndexMap();
	}
	
	private void initPageIdToIndexMap() {
		int index = 0;
		for (Page page : this.pageCollection) {
			this.pageIdToIndexMap.put(page.getId(), index++);
			this.absUrlToPageMap.put(page.getLink().getAbsUrl(), page);
//			System.out.println(page.getLink().getAbsUrl());
		}
	}
	
	public SortedSet<Page> runPageRank() {
		int numPages = this.pageCollection.size();
		this.epsilon = EPSILON_PARAM/numPages;
		double sum = 0;
		for (Page page : this.pageCollection) {
			double base = phiPage(page);
			page.setBase(base);
			sum += base;
		}
		for (Page page : this.pageCollection) {
			double normalizedScore = page.getBase()/sum;
			page.setScore(normalizedScore);
			page.setBase(normalizedScore);
		}
		double[][] weights = getWeights(numPages);
//		System.out.println(Arrays.deepToString(weights));
		runWeightedPageRank(weights);
		SortedSet<Page> sortedPages = new TreeSet<Page>(new Comparator<Page>() {
			@Override
			public int compare(Page o1, Page o2) {
				if (o1.getScore() < o2.getScore())
					return 1;
				if (o1.getScore() > o2.getScore())
					return -1;
				return 0;
			}			
		});
		sortedPages.addAll(this.pageCollection);
		
//		for (Page page : sortedPages) {
//			System.out.println(page.getLink().getUrl() + ": " + page.getScore());
//		}
		
		return sortedPages;
	}

	private void runWeightedPageRank(double[][] weights) {
		boolean changed = true;
		while (changed) {
			changed = false;
			for (Page page : this.pageCollection) {
				double newScore = (1-this.f)*page.getBase() + this.f*getSumWeightsFromPage(weights, page);
				page.setNewScore(newScore);
				if (Math.abs(page.getScore() - page.getNewScore()) > this.epsilon) {
					changed = true;
				}
			}
			for (Page page : this.pageCollection) {
				page.setScore(page.getNewScore());
			}
		}
	}
	
	private double getSumWeightsFromPage(double[][] weights, Page page) {
		double sum = 0;
		int pageIndex = getIndexFromPage(page);
		for (Page q : this.pageCollection) {
			int qIndex = getIndexFromPage(q);
			sum += q.getScore()*weights[pageIndex][qIndex];
		}
		return sum;
	}
	
	private int getIndexFromPage(Page page) {
		Integer pageIndex = this.pageIdToIndexMap.get(page.getId());
		if (pageIndex == null)
			throw new RuntimeException("Missing pageIndex!!" + page.getId());
		return pageIndex;
	}
	
	private double[][] getWeights(int numPages) {
		double[][] weights = new double[numPages][numPages];
		for (Page page : this.pageCollection) {
			Set<Link> outLinks = page.getOutLinks();
			int pageIndex = getIndexFromPage(page);
			boolean hasExistingOutlink = false;
			for (Link link : outLinks) {
				if (this.absUrlToPageMap.containsKey(link.getAbsUrl())) {
					hasExistingOutlink = true;
					break;
				}
			}
			if (hasExistingOutlink) {
				for (Page otherPage : this.pageCollection) {
					int otherPageIndex = getIndexFromPage(otherPage);
					weights[otherPageIndex][pageIndex] = 1.0/numPages;
				}
			} else {
				double sum = 0;
				for (Link link : outLinks) {
					Page q = this.absUrlToPageMap.get(link.getAbsUrl());
					if (q == null) {
						continue;
					}
					int qIndex = getIndexFromPage(q);
					double weightPages = thetaPage(page, q);
//					System.out.println(page.getLink().getUrl() + " --> " + q.getLink().getUrl() + "  = " + weightPages);
					weights[qIndex][pageIndex] = weightPages;
					sum += weightPages;
				}
				for (Link link : outLinks) {
					Page q = this.absUrlToPageMap.get(link.getAbsUrl());
					if (q == null) {
						continue;
					}
					int qIndex = getIndexFromPage(q);
					double newWeight = weights[qIndex][pageIndex]/sum;
					weights[qIndex][pageIndex] = newWeight;
				}
			}
		}
		return weights;
	}
	
	private double thetaPage(Page p, Page q) {
		double score = 0;
		Set<Link> visited = new HashSet<Link>();
		for (Link link : p.getOutLinks()) {
			if (link.getUrl().equals(q.getLink().getUrl())) {
				score+=getNumOccurencesOfSubstringInString(link.getUrl(), p.getContent());
				for (String scope : IMPORTANT_SCOPES) {
					score += isStringInsideScope(link.getAnchor(), p.getContent(), scope);
				}
				visited.add(link);
			}
		}
		return score;
	}
	
	private int getNumOccurencesOfSubstringInString(String substring, String string) {
		int lastIndex = 0;
		int count = 0;
		while (lastIndex != -1) {
			lastIndex = string.indexOf(substring, lastIndex);
			if (lastIndex != -1) {
				count++;
				lastIndex += substring.length();
			}
		}
		return count;
	}
	
	private int isStringInsideScope(String link, String text, String scope) {
		Pattern pattern = Pattern.compile("<"+scope+">(.*?)</"+scope+">");
		Matcher matcher = pattern.matcher(text);
		int count = 0;
		while (matcher.find()) {
			if (matcher.group(1).contains(link)) {
				count++;
			}
		}
		return count;
	}
	
	private double phiPage(Page page) {
		return Math.log(getNumberOfWordsInPage(page))/Math.log(2);
	}

	private int getNumberOfWordsInPage(Page page) {
//		return page.getContent().split("[ \n\r]+").length;
		return getTextFromPage(page).split("[ \n\r]+").length;
	}
	
	private String getTextFromPage(Page page) {
		String content = page.getContent();
		Document doc = Jsoup.parse(content);
		return doc.text();
	}
	
	public static void main(String[] args) {
//		new WeightedPageRank().go(args);
	}
}
