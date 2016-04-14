import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@AllArgsConstructor
@Data
public class Shingle {
	@NonNull
	private String text;
	private int count;
	
	public void incrementCount() {
		count++;
	}

}
