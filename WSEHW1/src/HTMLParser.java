import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.tidy.Tidy;


public class HTMLParser {
	
	private Tidy tidy;
	
	public HTMLParser() {		
		this.tidy = new Tidy();
		this.tidy.setQuiet(true);
		this.tidy.setXHTML(true);
		this.tidy.setQuiet(true);
		this.tidy.setShowWarnings(false);
	}
	
	public String getBody(File file){
		try (InputStream is = new FileInputStream(file)) {
		org.w3c.dom.Document doc = this.tidy.parseDOM(is, null);
		Element rawDoc = doc.getDocumentElement();
		if (rawDoc == null) {
			return null;
		}
		NodeList body = rawDoc.getElementsByTagName("body");
		return getText(body.item(0));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String getTitle(File file) {
		try (InputStream is = new FileInputStream(file)) {
			org.w3c.dom.Document doc = this.tidy.parseDOM(is, null);
			Element rawDoc = doc.getDocumentElement();
			if (rawDoc == null) {
				return null;
			}
			String title = "";
			NodeList children = rawDoc.getElementsByTagName("title");
			if (children.getLength() == 0) {
				return null;
			}
			Node titleElem = children.item(0);
			Text text = (Text)titleElem.getFirstChild();
			if (text == null) {
				return null;
			}
			title = text.getData();
			return title;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private String getText(Node node) {
		NodeList children = node.getChildNodes();
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				sb.append(getText(child));
				sb.append(" ");
			} else if (child.getNodeType() == Node.TEXT_NODE) {
				sb.append(((Text) child).getData());				
			}
		}
		return sb.toString();
	}
}
