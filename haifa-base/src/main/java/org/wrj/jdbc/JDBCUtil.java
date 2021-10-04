package org.wrj.jdbc;



import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Properties;

public class JDBCUtil {
    static{
        try{
            Class.forName("oracle.jdbc.driver.OracleDriver");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static Connection getConnection(){
        Connection con=null;
        try{
            Properties prop = new Properties();
            prop.load(new FileInputStream("src/tool/jdbc.properties"));
            String host = prop.getProperty("host", "");
            String port = prop.getProperty("port", "");
            String servername = prop.getProperty("servername", "");
            String username = prop.getProperty("username", "");
            String password = prop.getProperty("password", "");
            prop.clone();
            StringBuffer sb = new StringBuffer();
            sb.append("jdbc:oracle:thin:@");
            sb.append(host);
            sb.append(":");
            sb.append(port);
            sb.append(":");
            sb.append(servername);
            return DriverManager.getConnection(sb.toString(),username,password);
        }catch(Exception e){
            e.printStackTrace();
        }
        return con;
    }


    public static void close(ResultSet rs,Statement stm,PreparedStatement ps,Connection con){
        try{
            rs.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        try{
            stm.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        try{
            ps.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        try{
            con.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void close(ResultSet rs,PreparedStatement ps,Connection con){
        try{
            if(rs!=null){
                rs.close();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        try{
            if(ps!=null){
                ps.close();
            }

        }catch(Exception e){
            e.printStackTrace();
        }
        try{
            if(con!=null){
                con.close();
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }


    public static void close(Object o){
        try {
            if (o instanceof ResultSet) {
                ((ResultSet)o).close();
            }
            if (o instanceof Statement) {
                ((Statement)o).close();
            }
            if (o instanceof PreparedStatement) {
                ((PreparedStatement)o).close();
            }
            if (o instanceof Connection) {
                ((Connection)o).close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printResultSet(ResultSet rs){
        try {
            if (rs != null) {
                //�õ�Ԫ���ݶ���
                ResultSetMetaData md = rs.getMetaData();
                //�õ��ֶθ���
                int cols=md.getColumnCount();
                //�����ֶθ��������ʹ�ӡ�����
                StringBuffer sb=new StringBuffer();
                for(int i=0;i<cols;i++){
                    sb.append(md.getColumnName(i+1)+" ");
                }
                while(rs.next()){
                    for(int i=0;i<cols;i++){
                        sb.append(rs.getString(i+1)+" ");
                    }
                }
                sb.append("\n");
                System.out.println(sb.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
