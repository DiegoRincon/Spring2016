import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class DuplicateTest {

//	@Test
	public void test() {
		Set<Integer> set1 = new HashSet<Integer>(Arrays.asList(1,2,3,4,5));
		Set<Integer> set2 = new HashSet<Integer>(Arrays.asList(3,4,5,6,7,8));
		Duplicate dup = new Duplicate();
		Set<Integer> union = new HashSet<Integer>(Arrays.asList(1,2,3,4,5,6,7,8));
		Set<Integer> interesction = new HashSet<Integer>(Arrays.asList(3,4,5));
		assertEquals(interesction, dup.getIntersection(set1, set2));
		assertEquals(union, dup.getUnion(set1, set2));
	}
	
//	@Test
	public void test2() throws FileNotFoundException {
		String str1 = new Scanner(new File("original.txt")).useDelimiter("\\Z").next();
		String str2 = new Scanner(new File("modified.txt")).useDelimiter("\\Z").next();
		Duplicate dup = new Duplicate();
		Long start = System.nanoTime();
		System.out.println(dup.getApproxJaccard(str1, str2));
		System.out.println("time: " + (System.nanoTime()-start)/1000000000.0);
		System.out.println("---");
		start = System.nanoTime();
		System.out.println(dup.getJaccardFromDocs(str1, str2));
		System.out.println("time: " + (System.nanoTime()-start)/1000000000.0);
	}

//	@Test
	public void longStringTest() {
		Long numb = 123453212L;
		Long numb2 = 123453212L;
		String string = Long.toBinaryString(numb);
		String string2 = Long.toBinaryString(numb2);
		System.out.println(string2.compareTo(string));
//		System.out.println(string);
//		Duplicate dup = new Duplicate();
//		System.out.println(dup.getRandomPermutationOfLong(numb, new RandomPermutation(1L)));
	}
	
	@Test
	public void dnsTest() throws IOException {
		Document nytDoc = Jsoup.connect("http://www.nyt.com").followRedirects(true).get();
		Document nytimesDoc = Jsoup.connect("http://www.nytimes.com").followRedirects(true).get();
		Document newyorktimesDoc = Jsoup.connect("http://www.newyorktimes.com").followRedirects(true).get();
		System.out.println(nytDoc.baseUri());
		System.out.println(nytimesDoc.baseUri());
		System.out.println(newyorktimesDoc.baseUri());
	}
	
}
