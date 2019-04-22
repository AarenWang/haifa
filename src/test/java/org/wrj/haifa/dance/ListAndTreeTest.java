package org.wrj.haifa.dance;

import org.junit.Before;
import org.junit.Test;

public class ListAndTreeTest {

    private ListAndTree listAndTree;
    @Before
    public void before(){
        listAndTree = new ListAndTree();
    }

    @Test
    public void testDetectCycle(){
        ListAndTree.ListNode first = new ListAndTree.ListNode(1);
        ListAndTree.ListNode two = new ListAndTree.ListNode(2);
        ListAndTree.ListNode three = new ListAndTree.ListNode(3);
        ListAndTree.ListNode four = new ListAndTree.ListNode(4);
        ListAndTree.ListNode five = new ListAndTree.ListNode(5);

        first.next = two;
        two.next = three;
        three.next = four;
        four.next = five;
        five.next = two;

        ListAndTree.ListNode findNode = listAndTree.detectCycle(first);
        System.out.println(findNode);



    }
}
