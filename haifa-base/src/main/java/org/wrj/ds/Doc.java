package org.wrj.ds;

import java.util.ArrayList;
import java.util.List;

public class Doc {

	private Long docId;
	private String docName;
	private List<Doc> docs = new ArrayList<Doc>();

	public Doc(Long docId, String docName) {
		super();
		this.docId = docId;
		this.docName = docName;
	}

	public Doc(Long docId, String docName, List<Doc> docs) {
		super();
		this.docId = docId;
		this.docName = docName;
		this.docs = docs;
	}

	public Doc() {
		super();
	}

	public Long getDocId() {
		return docId;
	}

	public void setDocId(Long docId) {
		this.docId = docId;
	}

	public String getDocName() {
		return docName;
	}

	public void setDocName(String docName) {
		this.docName = docName;
	}

	public List<Doc> getDocs() {
		return docs;
	}

	public void setDocs(List<Doc> docs) {
		this.docs = docs;
	}
	
	


}
