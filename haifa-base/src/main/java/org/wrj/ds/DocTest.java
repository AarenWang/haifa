package org.wrj.ds;

import java.util.List;

public class DocTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Doc root = new Doc(0L,"我的资料");
		
		Doc d1 = new Doc(1L,"我的文档");
		Doc d2 = new Doc(2L,"我的音乐");
		
		Doc d1Java = new Doc(3L,"我的java");
		Doc d1Python = new Doc(4L,"我的python");
		
		Doc d2HK = new Doc(5L,"香港流行");
		Doc d2mainLand = new Doc(6L,"大陆流行");
		
		Doc zhang = new Doc(7L,"张靓颖");
		Doc wang = new Doc(8L,"王菲");
		
		root.getDocs().add(d1);
		root.getDocs().add(d2);
		
		d1.getDocs().add(d1Java);
		d1.getDocs().add(d1Python);
		
		d2.getDocs().add(d2HK);
		d2.getDocs().add(d2mainLand);
		
		updateDocTree(root, 6L, zhang);
		updateDocTree(root, 6L, wang);

	}
	
	
   public static void  updateDocTree(Doc tree,Long parentDocId, Doc doc){
		if(tree.getDocId().equals(parentDocId)){
			System.out.println("before:"+tree.getDocs().size());
			tree.getDocs().add(doc);
			System.out.println("after:"+tree.getDocs().size());
		}
		else{
			List<Doc> docs = tree.getDocs();
			for(Doc d: docs){
				updateDocTree(d, parentDocId, doc);
			}
		}
	}

}
