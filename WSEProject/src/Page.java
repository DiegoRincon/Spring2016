import java.util.Set;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@NoArgsConstructor
public class Page {
	@NonNull
	private String id;
	@NonNull
	private Link link;
	@NonNull
	private Set<Link> outLinks;
	@NonNull
	private String content;
	@NonNull
	private String title;
	private double base = 0;
	private double score = 0;
	private double newScore = 0;
	
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Page) {
			Page other = (Page)o;
			return this.id == other.getId() && this.link.equals(other.getLink());
		}
		return false;
	}
	
	
	@Override
	public int hashCode() {
		return 17*(this.id.hashCode() + this.link.hashCode()) + 31*17;
	}
}
