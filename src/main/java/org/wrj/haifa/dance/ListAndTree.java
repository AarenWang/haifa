package org.wrj.haifa.dance;

public class ListAndTree {

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int x) {
            val = x;
        }

        public String toString(){
            return val+"->"+(next == null ? "null" :this.next.toString());
        }

        @Override
        public boolean equals(Object obj) {
            return  this == obj;
        }
    }

    public ListNode reverseList(ListNode head) {

        return null;
    }

    /**
     * 检测链表是否有环
     * @param head
     * @return
     */
    public ListNode detectCycle(ListNode head) {
        ListNode fast = head;
        ListNode slow = head;
        ListNode current = head;
        while (current.next != null) {
            slow = current.next;
            fast = current.next.next;
            //到尾部了
            if(fast == null ){
                return null;
            }
            if(slow.val == fast.val) {
                return current;
            }
            current = current.next;
        }
        return null;

    }


}
