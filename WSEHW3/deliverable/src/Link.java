import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class Link {
	private String url;
	private String anchor;
	private String absUrl;
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Link) {
			Link other = (Link)o;
			return this.absUrl.equals(other.getAbsUrl());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return this.absUrl.length()*17;
	}
	
}
