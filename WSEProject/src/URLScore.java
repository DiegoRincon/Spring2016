import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class URLScore implements Comparable<URLScore>{
	private Link link;
	private double score;

	@Override
	public int compareTo(URLScore arg0) {
		if (this.score > arg0.score)
			return 1;
		else if (this.score < arg0.score)
			return -1;
		return 0;
	}
	
}
