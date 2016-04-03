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
	private Long id;
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
	private volatile int hashCode = 0;
	
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Page) {
			Page other = (Page)o;
			return this.id == other.getId() || this.link.equals(other.getLink());
		}
		return false;
	}
	
	
	@Override
	public int hashCode() {
		if (hashCode == 0) {
			return 17*this.id.intValue() + 31*17;
		}
		return this.hashCode;
	}
}
