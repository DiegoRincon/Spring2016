import java.util.Set;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Page {
	@NonNull
	private int id;
	@NonNull
	private Link link;
	@NonNull
	private Set<Link> outLinks;
	@NonNull
	private String content;
	private double base = 0;
	private double score = 0;
	private double newScore = 0;
}
