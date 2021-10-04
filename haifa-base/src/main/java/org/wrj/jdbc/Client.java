package org.wrj.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

public class Client {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Connection con = JDBCUtil.getConnection();
		System.out.println(con.toString());
		try {
			con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
