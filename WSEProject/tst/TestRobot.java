import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.StringTokenizer;

import nyu.crawler.crawler.Crawler;

public class TestRobot {

	public static void main(String[] args) {
		new TestRobot().testUrlRequest();
	}

	public void testUrlRequest() {
		String urlString = "http://stackoverflow.com/robots.txt";
		URL url = null;
		try {
			url = new URL(urlString);
		} catch (Exception e) {
			System.out.println("exception 1");
		}
		try {
			URLConnection uc = url.openConnection();
			uc.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:19.0) Gecko/20100101 Firefox/19.0");
			InputStream is = uc.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			String content = "";
			while ((line = br.readLine()) != null) {
				content += line + "\n";
			}
//			System.out.println(url.getFile());
//			System.out.println(content.toLowerCase(Locale.US));
			//We are only interested in the Disallow's for user-agent: *
			int indexOfUserAgentStar = content.toLowerCase(Locale.US).indexOf(Crawler.USER_AGENT_STAR.toLowerCase(Locale.US));
			int endOfUserAgentStar = content.toLowerCase(Locale.US).indexOf(Crawler.USER_AGENT.toLowerCase(Locale.US), indexOfUserAgentStar+Crawler.USER_AGENT_STAR.length());
			if (endOfUserAgentStar == -1)
				endOfUserAgentStar = content.length();
			robotSafeHelper(content.substring(indexOfUserAgentStar, endOfUserAgentStar));
			is.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}


	private boolean robotSafeHelper(String robotFile) {
		int lastDisallowIndex = 0;
		while ((lastDisallowIndex = robotFile.indexOf("Disallow:", lastDisallowIndex)) != -1) {
			lastDisallowIndex += new String("Disallow:").length();
			String badPaths = robotFile.substring(lastDisallowIndex);
			StringTokenizer st = new StringTokenizer(badPaths);

			if (!st.hasMoreTokens())
				break;
			System.out.println(st.nextToken());
		}
		return true;
	}

}
