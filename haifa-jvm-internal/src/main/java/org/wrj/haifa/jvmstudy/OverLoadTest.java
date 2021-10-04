package org.wrj.haifa.jvmstudy;

public class OverLoadTest {

    public static void main(String[] args) {
        OverLoadClass overLoadClass = new OverLoadClass();

        Object str1 = "Str1";
        String str2 = "Str2";

        overLoadClass.mock(str1);
        overLoadClass.mock(str2);

        overLoadClass.mockVarArgu(null,null);


    }

}

class  OverLoadClass{

    public void mock(String str){
        System.out.println("mock String");
    }

    public void mock(Object obj){
        System.out.println("mock object");
    }

    public void mockInteger(Integer i){
        System.out.println("mock Integer");

    }

    public void mockInteger(int i){
        System.out.println("mock Integer");

    }

    public  void mockVarArgu(String str,Object... objects){
        System.out.println("mock mockVarArgu String... objects");
    }

    public  void mockVarArgu(String str,String... objects){
        System.out.println("mock mockVarArgu String... objects");
    }


}