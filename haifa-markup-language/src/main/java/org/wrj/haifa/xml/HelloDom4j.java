package org.wrj.haifa.xml;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.net.URL;

public class HelloDom4j {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// URL url = new
		// URL("F:\\dev\\opensource\\dom4j-1.6.1\\xml\\cookbook.xml");
		HelloDom4j dom4j = new HelloDom4j();
		Document document = dom4j.parse("F:\\dev\\opensource\\dom4j-1.6.1\\xml\\cookbook.xml");
		/*String xpath = "book/bookinfo/author/firstname";
		List list = document.selectNodes(xpath);
		for (Object object : list) {
			Element e = (Element) object;
			dom4j.walkman(e);
		}

		Element element = document.getRootElement();
		System.out.println(element.getName());
		Element bookinfo = element.element("bookinfo");
		Element title = bookinfo.element("title");
		dom4j.walkman(bookinfo);
		dom4j.walkman(title);*/
		dom4j.treeWalk(document);
	}

	public Document parse(URL url) throws DocumentException {
		SAXReader reader = new SAXReader();
		Document document = reader.read(url);
		return document;
	}

	public Document parse(String path) throws DocumentException {
		SAXReader reader = new SAXReader();
		Document document = reader.read(new File(path));
		return document;
	}

	public void walkman(Element element) throws Exception {
		System.out.println("name=" + element.getName() + ",text="
				+ element.getText() + ",NodeType=" + element.getNodeType()
				+ ",NodeTypeName=" + element.getNodeTypeName());
	}

	public void treeWalk(Document document) {
		treeWalk(document.getRootElement());
	}

	public void treeWalk(Element element) {
		for (int i = 0, size = element.nodeCount(); i < size; i++) {
			Node node = element.node(i);
			if (node instanceof Element) {
				treeWalk((Element) node);
			} else {
				System.out.println("name=" + node.getName() + ",text="
						+ node.getText() + ",NodeType=" + node.getNodeType()
						+ ",NodeTypeName=" + node.getNodeTypeName());
			}
		}
	}
}
