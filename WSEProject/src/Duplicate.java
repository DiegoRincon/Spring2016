import java.util.HashMap;
import java.util.HashSet;
//import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class Duplicate {
	
	private static final int NUMBER_OF_RANDOM_PERMUTATIONS = 200;
	private static final int DEFAULT_SHINGLE_SIZE = 4;
	private static final double NEAR_DUPLICATE_THRESHOLD = 0.9;
	//TODO do something with this. Map the host (e.g. www.nyu.edu) to the list of ip addresses so to detect duplicates faster!
//	private Map<String, List<String>> hostToIpAddr;
	
	public Set<Shingle> getKShinglesSet(int k, String docText) {
		docText = docText.replaceAll("[^a-zA-Z0-9 ]", "");
		Map<String, Integer> shinglesMap = new HashMap<String, Integer>();
		String[] words = docText.split("[ \n\r]+");
		for (int i=0; i<=words.length-k; i++) {
			String str = getStringFromArray(words, i, i+k);
			Integer count = shinglesMap.get(str);
			if (count == null) {
				shinglesMap.put(str, 1);
			} else {
				shinglesMap.put(str, count+1);
			}
		}
		Set<Shingle> shinglesSet = new HashSet<Shingle>();
		for (Entry<String, Integer> entry : shinglesMap.entrySet()) {
			shinglesSet.add(new Shingle(entry.getKey(), entry.getValue()));
		}
		return shinglesSet;
	}
	
	private Long get64BitHash(Shingle shingle) {
		String text = shingle.getText();
		long h = 1125899906842597L; // prime
		int len = text.length();

		for (int i = 0; i < len; i++) {
			h = 31*h + text.charAt(i);
		}
		return h + shingle.getCount();
	}
	
	private Set<Long> get64BitHashSet(Set<Shingle> set) {
		Set<Long> hashSet = new HashSet<Long>();
		for (Shingle shingle : set) {
			hashSet.add(get64BitHash(shingle));
		}
		return hashSet;
	}
	
	/**
	 * NOT WORKING!!
	 * @param doc1
	 * @param doc2
	 * @return
	 */
	public double getApproxJaccard(String doc1, String doc2) {
		Set<Shingle> doc1Shingles = getKShinglesSet(DEFAULT_SHINGLE_SIZE, doc1);
		Set<Shingle> doc2Shingles = getKShinglesSet(DEFAULT_SHINGLE_SIZE, doc2);
		return approxJaccard(doc1Shingles, doc2Shingles);
	}
	
	public boolean arePagesNearDuplicates(Page newPage, Page oldPage) {
		double jaccard = getJaccardFromDocs(newPage.getContent(), oldPage.getContent());
		return jaccard >= NEAR_DUPLICATE_THRESHOLD;
	}
	
	public double getJaccardFromDocs(String doc1, String doc2) {
		Set<Shingle> doc1Shingles = getKShinglesSet(DEFAULT_SHINGLE_SIZE, doc1);
		Set<Shingle> doc2Shingles = getKShinglesSet(DEFAULT_SHINGLE_SIZE, doc2);
		return getJaccard(doc1Shingles, doc2Shingles);
	}
	
	private double approxJaccard(Set<Shingle> set1, Set<Shingle> set2) {
		Set<Long> set1HashSet = get64BitHashSet(set1);
		Set<Long> set2HashSet = get64BitHashSet(set2);
		Set<String> minNumbSet1 = new HashSet<String>();
		Set<String> minNumbSet2 = new HashSet<String>();
		Random random = new Random();
		for (int i=0; i<NUMBER_OF_RANDOM_PERMUTATIONS; i++) {
			RandomPermutation permutation = new RandomPermutation(random.nextLong());
			SortedSet<String> permutedSet1HashValues = getRandomPermutationOfSet(set1HashSet, permutation);
			SortedSet<String> permutedSet2HashValues = getRandomPermutationOfSet(set2HashSet, permutation);
			minNumbSet1.add(permutedSet1HashValues.first());
			minNumbSet2.add(permutedSet2HashValues.first());
		}
		Set<String> intersection = getIntersection(minNumbSet1, minNumbSet2);
		return (double)intersection.size()/NUMBER_OF_RANDOM_PERMUTATIONS;
	}
	
	private SortedSet<String> getRandomPermutationOfSet(Set<Long> set, RandomPermutation permutation) {
		SortedSet<String> permSet = new TreeSet<String>();
		for (Long number : set) {
			String permuted = getRandomPermutationOfLong(number, permutation);
			permSet.add(permuted);
		}
		permutation.reset();
		return permSet;
	}
	
	public String getRandomPermutationOfLong(Long number, RandomPermutation permutation) {
		String binaryString = Long.toBinaryString(number);
		char[] arr = binaryString.toCharArray();
		for (int i=arr.length;i>1; i--) {
			swapCharArray(arr, i-1, Math.abs(permutation.getNextNumber())%arr.length);
		}
		String permutedString = new String(arr);
		return permutedString;
	}
	
	private static void swapCharArray(char[] x, int a, int b) {
		char t = x[a];
		x[a] = x[b];
		x[b] = t;
	}
		
	public <E> double getJaccard(Set<E> set1, Set<E> set2) {
		if (set1.size() == 0 || set2.size() == 0) {
			return 0;
		}
		Set<E> union = getUnion(set1, set2);
		Set<E> intersection = getIntersection(set1, set2);
		return (double)intersection.size()/(double)union.size();
	}	
	
	public <E> Set<E> getUnion(Set<E> set1, Set<E> set2) {
		Set<E> union = new HashSet<E>(set1);
		union.addAll(set2);
		return union;
	}

	public <E> Set<E> getIntersection(Set<E> set1, Set<E> set2) {
		Set<E> interesection = new HashSet<E>(set1);
		interesection.retainAll(set2);
		return interesection;
	}

	private String getStringFromArray(String[] words, int start, int end) {
		StringBuffer sb = new StringBuffer();
		for(int i=start; i<end; i++) {
			sb.append(words[i] + " ");
		}
		return sb.toString().trim();
	}

}
