package nyu.crawler.update;
import java.util.Random;

public class RandomPermutation {
	private static final int NUMBER_OF_RANDOM_NUMBERS = 100;
	private int[] randomNumbers;
	private int next;
	
	public RandomPermutation(long seed) {
		Random random = new Random(seed);
		this.randomNumbers = new int[NUMBER_OF_RANDOM_NUMBERS];
		for (int i=0; i<NUMBER_OF_RANDOM_NUMBERS; i++) {
			this.randomNumbers[i] = random.nextInt();
		}
		this.next = 0;
	}
	
	public int getNextNumber() {
		if (this.next >= NUMBER_OF_RANDOM_NUMBERS)
			this.next = 0;
		return this.randomNumbers[this.next++];
	}
	
	
	public void reset() {
		this.next = 0;
	}

}
