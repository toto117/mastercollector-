import java.util.Timer;
import java.util.TimerTask;

import java.util.Date;
import java.util.Calendar;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;
import org.json.JSONArray;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
//import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class Checker{
  public static void main(String[] args) {
    System.out.println("Welcome\nStarting Checker\n");

    // And From your main() method or any other method
    // Calls the run method in the Check object every x milliseconds
    Timer timer = new Timer();
    timer.schedule(new Check(), 0, 30000);
    //timer.scheduleAtFixedRate(new Check(), 0, 10000);
  }
}

class Check extends TimerTask {
  // Static variables because we want to preserve the values every time the run function is called
  static Timestamp time1;     // The begining of the time interval
  static Timestamp time2;     // The end of the time interval

  @Override
  public void run() {
    Date date = new Date();
    long time = date.getTime();
    //System.out.println("Time in Milliseconds: " + time);
    Timestamp ts = new Timestamp(time);
    //System.out.println("Current Time Stamp: " + ts);

    if(time1 == null && time2 == null){
      // This condition enters the first time the run function is called
      time2 = ts;
      System.out.println("Waiting for Info to be Generated");
    } else {
      time1 = time2;    // Previous end time becomes the new start time
      time2 = ts;       // Endtime is the current timestamo

      String time1Str = new SimpleDateFormat("HH:mm:ss").format(time1);
      String time2Str = new SimpleDateFormat("HH:mm:ss").format(time2);

      System.out.println("\n\n\nChecking for new Info - " + ts + " |||   " + time1Str + " | " + time2Str);

      HttpURLConnection http = new HttpURLConnection(time1, time2);
  		//System.out.println("\nTesting 1 - Send Http POST request");
      try {
        http.sendPost();
      } catch (Exception e) {
        System.out.println("\nError sending POST request 1\n" + e);
      }
    }
  }
}

class HttpURLConnection {

  String accessToken = "08E37F75-F06B-4C41-B827-E218438B901F";
  Timestamp t1, t2;
  String from = null;   // From date
  String to = null;     // To date

	private final String USER_AGENT = "Mozilla/5.0";

  public HttpURLConnection(Timestamp argT1, Timestamp argT2){
    this.t1 = argT1;
    this.t2 = argT2;
  }

	// HTTP POST request
	public void sendPost() throws Exception {

		String url = "https://api.visitrack.com/api/Surveys/Activities";
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		//add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

    String time1Str = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(t1);
    String time2Str = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(t2);

    // This code adds 1 day to the timestamp
    // String dt = "2008-01-01";  // Start date
    //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    //Calendar c = Calendar.getInstance();
    //c.setTime(sdf.parse(time2Str));
    //c.add(Calendar.DATE, 1);  // number of days to add
    //time2Str = sdf.format(c.getTime());  // dt is now the new date

		//String urlParameters = "AccessToken=08E37F75-F06B-4C41-B827-E218438B901F&From=2019-09-03&To=2019-09-04";
    String urlParameters = "AccessToken="+accessToken +
                           "&From="+time1Str +
                           "&To="+time2Str;
    System.out.println(urlParameters);

		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
		//System.out.println("\nSending 'POST' request to URL : " + url);
		//System.out.println("Post parameters : " + urlParameters);
		//System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		//print result
		//System.out.println(response.toString());

    JSONArray myJsonArray = new JSONArray(response.toString());

    for (int i=0; i < myJsonArray.length(); i++) {
      //System.out.println(myJsonArray.getJSONObject(i).getString("GUID"));
      //arr.getJSONObject(i);
      HttpURLConnection2 http2 = new HttpURLConnection2(t1, t2, myJsonArray.getJSONObject(i).getString("GUID"));

  		//System.out.println("\nTesting 1 - Send Http POST request");
      try {
        http2.sendPost();
      } catch (Exception e) {
        System.out.println("\nError sending POST request 2\n" + e);
      }
    }

	}

}

class HttpURLConnection2 {

  String accessToken = "08E37F75-F06B-4C41-B827-E218438B901F";
  Timestamp t1, t2;
  String guid = null;
  String listValues = "false";

	private final String USER_AGENT = "Mozilla/5.0";

  public HttpURLConnection2(Timestamp argT1, Timestamp argT2, String argGuid){
    this.t1 = argT1;
    this.t2 = argT2;
    this.guid = argGuid;
  }

	// HTTP POST request
	public void sendPost() throws Exception {

		String url = "https://api.visitrack.com/api/Surveys/Activity";
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		//add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		//String urlParameters = "AccessToken=08E37F75-F06B-4C41-B827-E218438B901F&From=2019-09-03&To=2019-09-04";
    String urlParameters = "AccessToken="+accessToken +
                           "&GUID="+guid +
                           "&ListValues="+listValues;
    //System.out.println(urlParameters);

		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
		//System.out.println("\nSending 'POST' request to URL : " + url);
		//System.out.println("Post parameters : " + urlParameters);
		//System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		//print result
		//System.out.println(response.toString());

    JSONObject myJsonObject = new JSONObject(response.toString());

    //try {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
      Date parsedDate = dateFormat.parse(myJsonObject.getString("CreatedOn"));
      Timestamp timestamp = new Timestamp(parsedDate.getTime());
      //System.out.println(timestamp);
    /*} catch (Exception e) {
      System.out.println("Exception: Date Format\n" + e);
    }*/

    if ( timestamp.getTime() >= t1.getTime() && timestamp.getTime() <= t2.getTime() ) {
      //
      //System.out.println(myJsonObject.getString("GUID") + " - " + myJsonObject.getString("CreatedOn"));
      System.out.println("Nueva Entrada - " + myJsonObject.getString("GUID") + " - " + myJsonObject.getString("CreatedOn"));

      JSONArray ja_values = myJsonObject.getJSONArray("Values");

      System.out.println(ja_values.getJSONObject(0).getString("apiId") + ": " + ja_values.getJSONObject(0).getString("Value"));
      System.out.println(ja_values.getJSONObject(1).getString("apiId") + ": " + ja_values.getJSONObject(1).getString("Value"));
      System.out.println(ja_values.getJSONObject(2).getString("apiId") + ": " + ja_values.getJSONObject(2).getString("Value"));
      System.out.println(ja_values.getJSONObject(3).getString("apiId") + ": " + ja_values.getJSONObject(3).getString("Value"));
      System.out.println(ja_values.getJSONObject(4).getString("apiId") + ": " + ja_values.getJSONObject(4).getString("Value"));
      System.out.println(ja_values.getJSONObject(5).getString("apiId") + ": " + ja_values.getJSONObject(5).getString("Value"));
      //System.out.println(ja_values.getJSONObject(6).getString("apiId") + ": " + ja_values.getJSONObject(6).getString("Value"));
      System.out.println(ja_values.getJSONObject(7).getString("apiId") + ": " + ja_values.getJSONObject(7).getString("Value"));
      System.out.println(ja_values.getJSONObject(8).getString("apiId") + ": " + ja_values.getJSONObject(8).getString("Value"));
      System.out.println(ja_values.getJSONObject(9).getString("apiId") + ": " + ja_values.getJSONObject(9).getString("Value"));
      System.out.println(ja_values.getJSONObject(10).getString("apiId") + ": " + ja_values.getJSONObject(10).getString("Value"));
      System.out.println(ja_values.getJSONObject(11).getString("apiId") + ": " + ja_values.getJSONObject(11).getString("Value"));
      System.out.println(ja_values.getJSONObject(12).getString("apiId") + ": " + ja_values.getJSONObject(12).getString("Value"));
      System.out.println(ja_values.getJSONObject(13).getString("apiId") + ": " + ja_values.getJSONObject(13).getString("Value"));
      System.out.println(ja_values.getJSONObject(14).getString("apiId") + ": " + ja_values.getJSONObject(14).getString("Value"));

      //System.out.println("Area de Equipo: " + ja_values.getJSONObject(2).getString("Value"));
        //System.out.println("Tecnico: " + ja_values.getJSONObject(3).getString("Value"));
      //System.out.println("Tipo de Mantenimiento: " + ja_values.getJSONObject(4).getString("Value"));
      //System.out.println("Prioridad: " + ja_values.getJSONObject(5).getString("Value") + "\n");

      // DB Connection Starts here
      Connection conn = null;
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
        //String queryString = "select * from LC_vwLBSDocCustomerServiceEventList WHERE DateDocDelivery=?";
        String queryString = "INSERT INTO dbo.MyTestTable VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        conn = DriverManager.getConnection(connectionUrl);
        System.out.println("Connected to DB");
        ps = conn.prepareStatement(queryString);
        ps.setString(1, myJsonObject.getString("GUID"));
        ps.setString(2, ja_values.getJSONObject(0).getString("Value"));

        String createdOnStr = myJsonObject.getString("CreatedOn");
        System.out.println(createdOnStr);
        ps.setString(3, createdOnStr.substring(createdOnStr.length() - 4));
        //ps.setString(3, ja_values.getJSONObject(1).getString("Value"));

        ps.setString(4, ja_values.getJSONObject(2).getString("Value"));
        ps.setString(5, ja_values.getJSONObject(3).getString("Value"));
        ps.setString(6, ja_values.getJSONObject(4).getString("Value"));
        ps.setString(7, ja_values.getJSONObject(5).getString("Value"));
        ps.setString(8, ja_values.getJSONObject(7).getString("Value"));
        ps.setString(9, ja_values.getJSONObject(8).getString("Value"));
        ps.setString(10, ja_values.getJSONObject(9).getString("Value"));
        ps.setString(11, ja_values.getJSONObject(10).getString("Value"));
        ps.setString(12, ja_values.getJSONObject(11).getString("Value"));
        ps.setString(13, ja_values.getJSONObject(12).getString("Value"));
        ps.setString(14, ja_values.getJSONObject(13).getString("Value"));
        ps.setString(15, ja_values.getJSONObject(14).getString("Value"));
        ps.setString(16, ja_values.getJSONObject(15).getString("Value"));
        ps.execute();
        //rs = ps.executeQuery();
        //while (rs.next()) {
           //System.out.println(rs.getString(3) +" - "+ rs.getString(5) +" - "+ rs.getString(7) +" - "+ rs.getString(9));
        //}
      } catch (SQLException ex) {
        // Exception handling stuff
        System.out.println("Error connecting to the DB");
        ex.printStackTrace();
      } finally {
        try { rs.close(); } catch (Exception e) { /* ignored */ }
        try { ps.close(); } catch (Exception e) { /* ignored */ }
        try { conn.close(); } catch (Exception e) { /* ignored */ }
      }
    }

	}

}
