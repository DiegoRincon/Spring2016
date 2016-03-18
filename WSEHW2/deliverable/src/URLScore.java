
public class URLScore implements Comparable<URLScore>{
	public double score;
	public String url;
	
	public URLScore(String url, double score) {
		this.score = score;
		this.url = url;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof URLScore) {
			URLScore other = (URLScore)o;
			return this.score == other.score && this.url.equals(other.url);
		}
		return false;
	}
	
	@Override
	public String toString() {
		return this.url + " has score: " + this.score;
	}

	@Override
	public int compareTo(URLScore arg0) {
		if (this.score > arg0.score)
			return 1;
		else if (this.score < arg0.score)
			return -1;
		return 0;
	}
	
}
