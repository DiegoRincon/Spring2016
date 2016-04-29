package nyu.crawler.data;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Link {
	@NonNull
	private String url;
	@NonNull
	private String anchor;
	@NonNull
	private String absUrl;
	@NonNull
	private String uniqueUrl;
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Link) {
			Link other = (Link)o;
			return this.uniqueUrl.equals(other.getUniqueUrl());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return this.absUrl.length()*17;
	}
	
}
