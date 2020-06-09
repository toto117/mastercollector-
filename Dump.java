import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class Dump{
  public static void main(String[] args) {
    Connection conn = null;
    Statement s = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    String connectionUrl =
                //"jdbc:sqlserver://192.168.100.205.database.windows.net:1433;"
                "jdbc:sqlserver://192.168.100.205:1433;"
                + "database=GKING;"
                + "user=sa;"
                + "password=Limac00;"
                //+ "encrypt=true;"
                + "trustServerCertificate=false;"
                + "loginTimeout=30;";

    try {
      // Do stuff
      conn = DriverManager.getConnection(connectionUrl);
      System.out.println("Connected to DB");
      s = conn.createStatement();
      String queryString = "select * from LC_vwLBSDocCustomerServiceEventList WHERE DateDocDelivery='2019-09-03 16:35:31'";
      rs = s.executeQuery(queryString);
      while (rs.next()) {
         System.out.println(rs.getString(3) +" - "+ rs.getString(5) +" - "+ rs.getString(7) +" - "+ rs.getString(9));
      }
    } catch (SQLException ex) {
      // Exception handling stuff
      System.out.println("Error connecting to the DB");
      ex.printStackTrace();
    } finally {
      // Needs to import more libraries
      /*DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(ps);
      DbUtils.closeQuietly(conn);*/

      try { rs.close(); } catch (Exception e) { /* ignored */ }
      try { ps.close(); } catch (Exception e) { /* ignored */ }
      try { conn.close(); } catch (Exception e) { /* ignored */ }
    }
    /*String connectionUrl =
                //"jdbc:sqlserver://192.168.100.205.database.windows.net:1433;"
                "jdbc:sqlserver://192.168.100.205:1433;"
                        + "database=GKING;"
                        + "user=sa;"
                        + "password=Limac00;"
                        //+ "encrypt=true;"
                        + "trustServerCertificate=false;"
                        + "loginTimeout=30;";

        try (Connection connection = DriverManager.getConnection(connectionUrl);) {
            // Code here.
            System.out.println("connected");
            Statement statement = connection.createStatement();
            String queryString = "select * from LC_vwLBSDocCustomerServiceEventList WHERE DateDocDelivery='2019-09-03 16:35:31'";
            ResultSet rs = statement.executeQuery(queryString);
            while (rs.next()) {
               System.out.println(rs.getString(3) +" - "+ rs.getString(5) +" - "+ rs.getString(7) +" - "+ rs.getString(9));
            }
        }
        // Handle any errors that may have occurred.
        catch (SQLException e) {
            e.printStackTrace();
        }*/


    // Check JRE Version
    /*System.out.println(System.getProperty("java.vendor"));
    System.out.println(System.getProperty("java.vendor.url"));
    System.out.println(System.getProperty("java.version"));*/

    //try {
       //Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
       /*Connection conn = DriverManager.getConnection(db_connect_string,
                db_userid, db_password);*/
       /*Connection conn = DriverManager.getConnection("GKING","sa","limac00");
       System.out.println("connected");
       Statement statement = conn.createStatement();
       String queryString = "select * from sysobjects where type='u'";
       ResultSet rs = statement.executeQuery(queryString);
       while (rs.next()) {
          System.out.println(rs.getString(1));
       }*/
    //} catch (Exception e) {
      //System.out.println("Error on DB Connection");
      // e.printStackTrace();
    //}
  }
}
