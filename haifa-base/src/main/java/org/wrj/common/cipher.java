package org.wrj.common;

public class cipher {
  public String srst="";
  public cipher() {
  }
  public int c10to34(int n)
  {
      int nIdx;
      int n34[]=new int[4];
      char Char34[] =new char[35];
      String stmp="";

      int tmp;
      n34[0] = 1;
      for(nIdx = 1;nIdx < 4;nIdx++)
       { n34[nIdx] = n34[nIdx - 1] * 34;
       }
        int tmpi;
        char tmpc;
      for(nIdx = 0;nIdx < 10;nIdx++)
      {
        tmpi='0';
        tmpi=tmpi+nIdx;
        Char34[nIdx]=(char)tmpi;
      }
      for(nIdx  = 10;nIdx < 34;nIdx++)
      {
        tmpi='A';
        tmpi=tmpi+nIdx-10;
        Char34[nIdx] =(char)tmpi;
      }
      for(nIdx = 3;nIdx >= 0;nIdx--){
        tmp=(int)n/n34[nIdx];
        srst+=Char34[tmp];
        n %= n34[nIdx];
     }

      return 0;
  }

  public int c34to10(String Str)
 {
     int n34[]=new int[4];
     int i,j,n;
     char ch;

     if (Str.length()!=4)
     {
         n=0;
         return n;
     }
     n34[0]=1;
     for (i = 2;i <= 4;i++)
     {
         n34[i-1]=n34[i-2]*34;
     }

     n=0;
     for (i=1;i <= 4;i++)
     {
         ch=Str.charAt(i-1);
         j=ch-'0';
         if ((ch>='0')&&(ch<='9'))
         {
             j=ch-'0';
         }
         else
         {
             j=ch-'A'+10;
         }
         n=n+j*n34[5-i-1];
     }
     return n;
 }
    /***************************************************************************
    �������:EnCode
    ������:���ܺ������ݿ��û�����ļ���
    �������:
            sUnCoded :����ǰ������
    �������:
            sEnCoded :���ܺ������
    �� �� ֵ:
            0���ɹ�������ʧ��
    ***************************************************************************/

    public String EnCode(String sUnCoded)    /*���ܣ��������󳤶�Ϊ12��*/
    {
        String sEnCoded="";
        int i,n,Len1,PwdLen,SumAscii=0,PlusNum=0,ChgNum=0;
        int ExChange[][]={{23,25,29,0,0,36,28,37,0,35,39,0,32,31,0,0,26,27,34},
                             {27,33,30,0,0,21,24,29,0,38,22,0,39,0,32,36,0,37,0}};
        String Str13;
        String First;
        String Str39="";
        String FillStr;
        String PlusStr[]={"200022012120110102201101211011112102112",
                            "021102010210101210220001112010211020100",
                            "120002101012010220221101202011021211010",
                            "210201012210202011111212011020112011020",
                            "202112211012101100222120121201012121021",
                            "102121210021020221211010112020110202011",
                            "022102110121212201211221002201100012100",
                            "221120101220211202012101210210110010221",
                            "012020201101021022112112112111102101101",
                            "101212012211021101011221021022022100110",
                            "012202110210011202101220121100101211002",
                            "220102021101202012101102010201202011012",
                            "121111212010121021120101111021021202112"};
        char cTmp;
        String sTmp;
        FillStr="[1E<$&@&>n2]";
        PwdLen=sUnCoded.length();
        Str13=PwdLen+sUnCoded;
        Str13=Str13+FillStr;
        Str13=Str13.substring(0,13);
        Len1=Str13.length();
        for (i=1;i<=Len1;i++)
        {
            SumAscii=SumAscii+Str13.charAt(i-1);
        }
        PlusNum = SumAscii%13;
        ChgNum  = SumAscii%2;
        int ascii=ChgNum*13+PlusNum+'A';
        char asciichar=(char)ascii;
        First=""+asciichar;
        for (i=1;i<=Len1;i++)
        {
            n=(255*255-Str13.charAt(i-1)*Str13.charAt(i-1));

            cipher ci=new cipher();
            ci.c10to34(n);
            sTmp=ci.srst;

            sTmp=sTmp.substring(1,4);
            Str39+=sTmp;
        }
        char CStr39[]=Str39.toCharArray();
        for (i=1;i<=39;i++)
        {
          int tmpstr=CStr39[i-1]+PlusStr[PlusNum].charAt(i-1)-'0';
            CStr39[i-1]=(char)tmpstr;
        }
        for (i=1;i<=19;i++)
        {
            if (ExChange[ChgNum][i-1]>0)
            {
                cTmp=CStr39[i-1];
                CStr39[i-1]=CStr39[ExChange[ChgNum][i-1]-1];
                CStr39[ExChange[ChgNum][i-1]-1]=cTmp;
            }
        }
        Str39=new String(CStr39);
        sEnCoded=First+Str39;
        return sEnCoded;
    }
    /***************************************************************************
    �������:DeCode
    ������:���ܺ������ݿ��û��������ĵĽ���
    �������:
            sEnCoded :����ǰ������
    �������:
            sUnCoded :���ܺ������
    �� �� ֵ:
            0���ɹ���99:ԭ��δ���ܣ�����ʧ��
    ***************************************************************************/


    public String DeCode(String sEnCoded)
    {
      String sUnCoded="";
        int i,n,SumAscii=0,PlusNum=0,ChgNum=0;
        int ExChange[][]={{23,25,29,0,0,36,28,37,0,35,39,0,32,31,0,0,26,27,34},
                             {27,33,30,0,0,21,24,29,0,38,22,0,39,0,32,36,0,37,0}};
       String Str="";
       String Str1="";
       String Str2="";
       String FillStr="";
       String Frist="";
       String PlusStr[]={"200022012120110102201101211011112102112",
                            "021102010210101210220001112010211020100",
                            "120002101012010220221101202011021211010",
                            "210201012210202011111212011020112011020",
                            "202112211012101100222120121201012121021",
                            "102121210021020221211010112020110202011",
                            "022102110121212201211221002201100012100",
                            "221120101220211202012101210210110010221",
                            "012020201101021022112112112111102101101",
                            "101212012211021101011221021022022100110",
                            "012202110210011202101220121100101211002",
                            "220102021101202012101102010201202011012",
                            "121111212010121021120101111021021202112"};
        char cTmp;
        String Str39="";
        String sTmp= "",sTmp1= "";
        String sStrTmp1 = "",sStrTmp2 = "";
        int n1=0;
        int LenMatch=0;
        FillStr="[1E<$&@&>n2]";
        if (sEnCoded.length()!=40)
        {
           sUnCoded=sEnCoded;
            return sUnCoded;
        }
        Frist=sEnCoded.substring(0,1);
        SumAscii=Frist.charAt(0)-'A';

        ChgNum  = (int)(SumAscii/13);
        PlusNum = SumAscii%13;
        Str39=sEnCoded.substring(1,40);
        char CStr39[]=Str39.toCharArray();
        for (i=1;i<=19;i++)
        {
            if (ExChange[ChgNum][i-1]>0)
            {
              cTmp=CStr39[i-1];
              CStr39[i-1]=CStr39[ExChange[ChgNum][i-1]-1];
              CStr39[ExChange[ChgNum][i-1]-1]=cTmp;
            }
        }
        for (i=1;i<=39;i++)
        {

          int tmpstr=CStr39[i-1]-(PlusStr[PlusNum].charAt(i-1)-'0');
            CStr39[i-1]=(char)tmpstr;
        }
        Str39=new String(CStr39);
        Str1=Str39;
        for (i=1;i<=13;i++)
        {
            sTmp1="1";
            sTmp=Str1.substring((i-1)*3,(i-1)*3+3);
            sTmp1+=sTmp;
            cipher ci=new cipher();
            n=ci.c34to10(sTmp1);
            n=(int)(Math.sqrt(255*255-n));
            Str2=Str2+(char)n;
        }
        sTmp=""+Str2.charAt(0);
        n1= Integer.parseInt(sTmp);
        sUnCoded=Str2.substring(1,1+n1);
        return sUnCoded;
    }





  public static void main(String[] args)
{
   cipher ci=new cipher();
   System.out.println("����----------------"+ci.EnCode("wangrenjun@123"));
   System.out.println("����----------------"+ci.DeCode("I3L5JGEJMV42V4KQJ6WM3ZGJYPK;MRH:KNLKW50R"));
}




}
