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
import java.sql.Time;

class Collector{
  public static void main(String[] args) {
    System.out.println("Welcome\nStarting Collector\n");

    // Calls the run method in the Check object every x milliseconds
    Timer timer = new Timer();
    // Checar cada 60,000 ms
    timer.schedule(new Check(), 0, 30000);
  }
}

class Check extends TimerTask{
  Timestamp ts, tsPast;

  String url0 = "https://api.visitrack.com/api/Users/Authentication";// Authentication Token
  String url1 = "https://api.visitrack.com/api/Surveys/Activities";  // All activities in general (can be filtered between 2 timestamps)
  String url2 = "https://api.visitrack.com/api/Surveys/Activity";    // Detail of an Activity by its guid
  String url3 = "https://api.visitrack.com/api/Locations/Get";       // Detal of a Location by its guid
  String urlParameters;
  String response;          // Here we we will store the response from the requests

  String usr = "Limac01";
  String pswd = "limac01";
  // (BE CAREFUL, this token expires)
  String accessToken = "";//"1DD8F449-6A3F-4748-BB32-2B5967722538";      // Access Token for the API
  String from = null;   // From date
  String to = null;     // To date
  String guid;
  String listValues = "false";
  String centroCostos = "";
  int salesrepContactid = 0;
  float precioLista = 0;

  JSONArray myJsonArray;
  JSONArray ja_values;
  JSONObject myJsonObject;

  String connectionUrl;
  Connection conn;
  PreparedStatement ps, ps2;
  ResultSet rs;
  String queryString, queryString2, queryString3, queryString4;

  int idProd;

  @Override
  public void run() {
    // Get the current Timestamp
    Date date = new Date();
    long time = date.getTime();
    ts = new Timestamp(time);

    try {
      // Get the Access Toekn
      urlParameters = "Login="+usr +
                      "&Password="+pswd;
      //System.out.println(urlParameters);

      // New URL connection to get AccessToken
      HttpURLConnection http = new HttpURLConnection(url0, urlParameters);

      try {
        response = http.sendPost();
        accessToken = new JSONObject(response).getString("AccessToken");  // Saving the AccessToken from the response
        //System.out.println(response);
        //System.out.println("AT = " + accessToken);
      } catch (Exception e) {
        System.out.println("Exception Error: ");
        e.printStackTrace();
      }

      // This code substracts 10 minutes to the timestamp
      Calendar c = Calendar.getInstance();
      c.setTime(date);
      c.add(Calendar.MINUTE, -10);  // number of minutes to add
      date = c.getTime();
      time = date.getTime();
      tsPast =  new Timestamp(time);  // The timestamp of 10 minutes ago
      //System.out.println(tsPast + " - " + ts);
      System.out.println("\n\n\nChecking for new info\t" + ts);
    } catch (Exception e) {
      System.out.println("Exception Error: ");
      e.printStackTrace();
    }

    // Format the timestamps for the urlParameters to send to the API
    String time1Str = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(tsPast);
    String time2Str = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(ts);

    urlParameters = "AccessToken="+accessToken +
                           "&From="+time1Str +
                           "&To="+time2Str;
    //System.out.println(urlParameters);

    // New URL connection to get all the activities in the last 10 minutes
    HttpURLConnection http = new HttpURLConnection(url1, urlParameters);

    try {
      response = http.sendPost();
      //System.out.println(response);
    } catch (Exception e) {
      System.out.println("Exception Error: ");
      e.printStackTrace();
    }

    try {
      // Array containing all the activities in the last 10 minutes
      myJsonArray = new JSONArray(response);

      // Iterate over each activity
      for (int i=0; i < myJsonArray.length(); i++) {
        //System.out.println(myJsonArray.getJSONObject(i).getString("GUID"));

        // DB Connection Starts here
        connectionUrl =
                    //"jdbc:sqlserver://192.168.100.205.database.windows.net:1433;"
                    "jdbc:sqlserver://192.168.116.1:1433;"
                    + "database=AMHAHMEX;"
                    + "user=sa;"
                    + "password=Limac00;"
                    //+ "encrypt=true;"
                    + "trustServerCertificate=false;"
                    + "loginTimeout=30;";
        queryString = "select * from docDocument WHERE Custom1=?";
        queryString2 = "INSERT INTO dbo.MyTestTable VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        conn = DriverManager.getConnection(connectionUrl);
        //System.out.println("Connected to DB");
        ps = conn.prepareStatement(queryString);
        ps.setString(1, myJsonArray.getJSONObject(i).getString("GUID"));

        // Execute Query to Check if the activity has already been registered
        rs = ps.executeQuery();

        if (rs.next() == false) {
          // Need to Insert

          System.out.println("Nueva Entrada: " + myJsonArray.getJSONObject(i).getString("GUID"));
          urlParameters = "AccessToken="+accessToken +
                                 "&GUID="+myJsonArray.getJSONObject(i).getString("GUID") +
                                 "&ListValues="+listValues;
          //System.out.println(urlParameters);

          // Prepare connection to the API to get the details of the activity
          http.setUrl(url2);
          http.setUrlParams(urlParameters);

          // Get the details of the activity
          response = http.sendPost();
          System.out.println(response);

          // Create a JSONObject containing the details of the activity from the response
          myJsonObject = new JSONObject(response);

          // Check the form type by its form ID
          if ( myJsonObject.getString("FormGUID").compareTo( "iIEgR6dskA" ) == 0 ) {
            // Form1 ISSEMYM detected
            //System.out.println("Form 1 ISSEMYM GUID detected");

            ja_values = myJsonObject.getJSONArray("Values");

            // This Part of the code gets the salesrepContactid(number) from the name of the Tecnico
            salesrepContactid = 0;
            queryString3 = "SELECT ContactID FROM vwLBSContactList WHERE ContactName=?";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, ja_values.getJSONObject(3).getString("Value"));
            rs = ps2.executeQuery();
            if (rs.next()) {
               // If we found the name in the table, then we assign its corresponding ID
               salesrepContactid = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }
            //System.out.println("Found: " + ja_values.getJSONObject(3).getString("Value") + "\nID: "+ salesrepContactid);
            // ---------------------------------------------------

            // Falta Buscar en BusinessEntityID en tabla (por ahora se pone 2 por default)
            queryString3 = "INSERT INTO docDocument(ModuleID,documentTypeID,DocRecipientID,OwnedBusinessEntityID,BusinessEntityID,DepotID,Folio,DateDocument,LanguageID,CurrencyID,Rate,SubTotal,Total,TotalTax,"
    			               + "CreatedOn, CreatedBy,Custom1,salesrepContactid,Comments)"
                         + "VALUES (1214, 40, 1, 1, 2, 1, ?, GETDATE(), 3, 3, 1, 0, 0, 0, GETDATE(), 1, ?, ?, ?);"
                         + "SELECT SCOPE_IDENTITY();";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, myJsonArray.getJSONObject(i).getString("Consecutive"));
            ps2.setString(2, myJsonArray.getJSONObject(i).getString("GUID"));
            ps2.setString(3, salesrepContactid+"");
            ps2.setString(4, ja_values.getJSONObject(8).getString("Value"));
            rs = ps2.executeQuery();
            int iddoc = -2; // If this number stays like this, it indicates an error
            while (rs.next()) {
               iddoc = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }

            // This portion on the code gets the details for the location of the current Activity
            // Details like unidad medica and centro de costos
            urlParameters = "tkn="+accessToken +
                            "&LocationGUID="+myJsonObject.getString("LocationGUID");
            //System.out.println("Sending Request to:\n"+url3+"?"+urlParameters);

            http.setUrl(url3);
            http.setUrlParams(urlParameters);

            response = http.sendPost();
            centroCostos = new JSONObject(response).getJSONArray("Values").getJSONObject(0).getString("Value");
            //System.out.println(centroCostos);
            // -------------------------------------------------

            if (ja_values.getJSONObject(4).getString("Value").compareTo( "Emergencia" ) == 0) {
              // The service IS an Emergency
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,1,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            } else {
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,0,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            }

            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, centroCostos);
            ps2.setString(2, ja_values.getJSONObject(5).getString("Value"));
            ps2.execute();

            JSONArray prodJsonArray = ja_values.getJSONObject(17).getJSONArray("Value");

            for (int j=0; j < prodJsonArray.length(); j++) {
              queryString4 = "SELECT productId, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              if (rs.next()) {
                // Product is al ready in table
                System.out.println("Product Already In  Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                idProd = Integer.parseInt( rs.getString(1) );
                System.out.println(idProd);
              } else {
                // New product has to be registered
                System.out.println("Product Has Not Been Registered In Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                queryString4 = "INSERT INTO orgProduct (productName,unit,unitsale,availableForSale, newProduct, canSaleNegative, productTypeID)"
                             + "VALUES ('"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "1, "
                             + "1, "
                             + "1, "
                             + "1);";
                //System.out.println(queryString4);
                ps2 = conn.prepareStatement(queryString4);
                ps2.execute();
              }
              //System.out.println("Look for: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));

              // Now we are sure that the product is registered in Contpaq
              // We get its ID
              queryString4 = "SELECT productId, priceList, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              rs.next();
              idProd = Integer.parseInt( rs.getString(1) );
              precioLista = Float.parseFloat( rs.getString(2) );
              //System.out.println("Going to insert product: "+prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value")+ " with ID: "+ idProd);

              // We insert into docDocumentItem table in sqlserver
              queryString4 = "INSERT INTO docDocumentItem(DocumentID,mustBeDelivered,Quantity,CantSolicitada,ProductID,[Description],TipoPago,DiscountPerc,TaxTypeID,TaxPerc,UnitPrice,Total,CostPrice,LineNumber,ProductId1)"
                           + "VALUES ("+iddoc+", "
                           + "1, "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + "0, " // + idProd + ", "
                           + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                           + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                           + "0, "
                           + "5, "
                           + "0.16, "
                           + "0, "
                           + "0, "
                           + precioLista+ ", " // Cost Price
                           + ""+ (j+1) +", "
                           + idProd+");"; // ProductID
              System.out.println(queryString4);
              ps2 = conn.prepareStatement(queryString4);
              ps2.execute();
            }
          }
          //
          if ( myJsonObject.getString("FormGUID").compareTo( "E4A104EE-F797-49F2-98DC-F41E61F591FC" ) == 0 ) {
            // Form1 ISSTE Detected
            //System.out.println("Form1 ISSTE Detected");

            ja_values = myJsonObject.getJSONArray("Values");

            // This Part of the code gets the salesrepContactid(number) from the name of the Tecnico
            salesrepContactid = 0;
            queryString3 = "SELECT ContactID FROM vwLBSContactList WHERE ContactName=?";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, ja_values.getJSONObject(3).getString("Value"));
            rs = ps2.executeQuery();
            if (rs.next()) {
               // If we found the name in the table, then we assign its corresponding ID
               salesrepContactid = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }
            //System.out.println("Found: " + ja_values.getJSONObject(3).getString("Value") + "\nID: "+ salesrepContactid);
            // ---------------------------------------------------

            // Falta Buscar en BusinessEntityID en tabla (por ahora se pone 2 por default)
            queryString3 = "INSERT INTO docDocument(ModuleID,documentTypeID,DocRecipientID,OwnedBusinessEntityID,BusinessEntityID,DepotID,Folio,DateDocument,LanguageID,CurrencyID,Rate,SubTotal,Total,TotalTax,"
    			               + "CreatedOn, CreatedBy,Custom1,salesrepContactid,Comments)"
                         + "VALUES (1214, 40, 1, 1, 2, 1, ?, GETDATE(), 3, 3, 1, 0, 0, 0, GETDATE(), 1, ?, ?, ?);"
                         + "SELECT SCOPE_IDENTITY();";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, myJsonArray.getJSONObject(i).getString("Consecutive"));
            ps2.setString(2, myJsonArray.getJSONObject(i).getString("GUID"));
            ps2.setString(3, salesrepContactid+"");
            ps2.setString(4, ja_values.getJSONObject(8).getString("Value"));
            rs = ps2.executeQuery();
            int iddoc = -2; // If this number stays like this, it indicates an error
            while (rs.next()) {
               iddoc = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }

            // This portion on the code gets the details for the location of the current Activity
            // Details like unidad medica and centro de costos
            urlParameters = "tkn="+accessToken +
                            "&LocationGUID="+myJsonObject.getString("LocationGUID");
            //System.out.println("Sending Request to:\n"+url3+"?"+urlParameters);

            http.setUrl(url3);
            http.setUrlParams(urlParameters);
            //System.out.println(url3 + urlParameters);

            response = http.sendPost();
            centroCostos = new JSONObject(response).getJSONArray("Values").getJSONObject(0).getString("Value");
            //System.out.println(centroCostos);
            // -------------------------------------------------

            if (ja_values.getJSONObject(4).getString("Value").compareTo( "Emergencia" ) == 0) {
              // The service IS an Emergency
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,1,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            } else {
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,0,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            }

            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, centroCostos);
            ps2.setString(2, ja_values.getJSONObject(5).getString("Value"));
            ps2.execute();

            JSONArray prodJsonArray = ja_values.getJSONObject(12).getJSONArray("Value");

            for (int j=0; j < prodJsonArray.length(); j++) {
              queryString4 = "SELECT productId, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              if (rs.next()) {
                // Product is al ready in table
                System.out.println("Product Already In  Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                idProd = Integer.parseInt( rs.getString(1) );
                System.out.println(idProd);
              } else {
                // New product has to be registered
                System.out.println("Product Has Not Been Registered In Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                queryString4 = "INSERT INTO orgProduct (productName,unit,unitsale,availableForSale, newProduct, canSaleNegative, productTypeID)"
                             + "VALUES ('"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "1, "
                             + "1, "
                             + "1, "
                             + "1);";
                //System.out.println(queryString4);
                ps2 = conn.prepareStatement(queryString4);
                ps2.execute();
              }
              //System.out.println("Look for: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));

              // Now we are sure that the product is registered in Contpaq
              // We get its ID
              queryString4 = "SELECT productId, priceList, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              rs.next();
              idProd = Integer.parseInt( rs.getString(1) );
              precioLista = Float.parseFloat( rs.getString(2) );
              //System.out.println("Going to insert product: "+prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value")+ " with ID: "+ idProd);

              // We insert into docDocumentItem table in sqlserver
              queryString4 = "INSERT INTO docDocumentItem(DocumentID,mustBeDelivered,Quantity,CantSolicitada,ProductID,[Description],DiscountPerc,TaxTypeID,TaxPerc,UnitPrice,Total,CostPrice,LineNumber,ProductId1)"
                           + "VALUES ("+iddoc+", "
                           + "1, "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + "0, "
                           + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                           + "0, "
                           + "5, "
                           + "0.16, "
                           + "0, "
                           + "0, "
                           + precioLista+ ", " // Cost Price
                           + ""+ (j+1) +", "
                           + idProd+");"; // ProductID
              System.out.println(queryString4);
              ps2 = conn.prepareStatement(queryString4);
              ps2.execute();
            }
          }
          // Check the form type by its form ID
//FORMULARIO MODIFICADO ABAJO
          if ( myJsonObject.getString("FormGUID").compareTo( "1ZY27hxh2L" ) == 0 ) {

            // Form3 ISSEMYM detected
            System.out.println("Form 3 GUID detected");

            ja_values = myJsonObject.getJSONArray("Values");
            //System.out.println(ja_values.getJSONObject(3).getString("Value"));

            // We look for the ID of the parent Requisition Document
            queryString3 = "SELECT * FROM docDocument WHERE folio=?";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, ja_values.getJSONObject(3).getString("Value"));
            rs = ps2.executeQuery();
            int iddoc = -2; // If this number stays like this, it indicates an error

            if (rs.next()) {
              // The Requisition folio was found
              iddoc = Integer.parseInt( rs.getString(1) );
              System.out.println(iddoc +" - "+ ja_values.getJSONObject(3).getString("Value"));

              // Initialize all products to 0
              queryString3 = "UPDATE docDocumentItem "
                           + "SET CantUtilizada=0,Quantity=0 "
                           + "WHERE DocumentID="+iddoc;
              ps2 = conn.prepareStatement(queryString3);
              ps2.executeQuery();

              JSONArray refaccionesJsonArray = ja_values.getJSONObject(6).getJSONArray("Value");

              for (int j=0; j < refaccionesJsonArray.length(); j++) {
                //System.out.println(refaccionesJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));

                // We get the productID
                queryString4 = "SELECT productId, productName FROM orgProduct WHERE productName = ?";
                ps2 = conn.prepareStatement(queryString4);
                ps2.setString(1, refaccionesJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                rs = ps2.executeQuery();
                if (rs.next()) {
                  // Product found in table
                  idProd = Integer.parseInt( rs.getString(1) );
                  System.out.println(idProd + " - " + rs.getString(2));

                  // Update de Document Items of the Requisition (cantidadUtilizada & Quantity)
                  queryString3 = "UPDATE docDocumentItem "
                               + "SET CantUtilizada=?,Quantity=? "
                               + "WHERE DocumentID="+iddoc+" AND Description='"+refaccionesJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value")+"'";
                  ps2 = conn.prepareStatement(queryString3);
                  ps2.setString(1, refaccionesJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value"));
                  ps2.setString(2, refaccionesJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value"));
                  System.out.println(queryString3);
                  ps2.executeUpdate();
                }

                // Update de Document of the Entrega Requisicion (Custom1)
                queryString3 = "UPDATE docDocument "
                             + "SET Custom1=? "
                             + "WHERE sourceDocumentID="+iddoc;
                ps2 = conn.prepareStatement(queryString3);
                ps2.setString(1, myJsonObject.getString("GUID"));
                ps2.executeUpdate();
              }
            }

          }
          //
          if ( myJsonObject.getString("FormGUID").compareTo( "13E5EECF-AF5D-468C-9F2A-B9330A2CD8B1" ) == 0 ) {
            System.out.println("nuevo formulario");
            // Form1 copia1
            /* FOMULARIO 1 BAFAR*/
            //System.out.println("Form 1 copia1 GUID detected");

            ja_values = myJsonObject.getJSONArray("Values");

            // This Part of the code gets the salesrepContactid(number) from the name of the Tecnico
            salesrepContactid = 0;
            queryString3 = "SELECT ContactID FROM vwLBSContactList WHERE ContactName=?";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, ja_values.getJSONObject(3).getString("Value"));
            rs = ps2.executeQuery();
            if (rs.next()) {
               // If we found the name in the table, then we assign its corresponding ID
               salesrepContactid = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }
            //System.out.println("Found: " + ja_values.getJSONObject(3).getString("Value") + "\nID: "+ salesrepContactid);
            // ---------------------------------------------------

            // Falta Buscar en BusinessEntityID en tabla (por ahora se pone 2 por default)
            queryString3 = "INSERT INTO docDocument(ModuleID,documentTypeID,DocRecipientID,OwnedBusinessEntityID,BusinessEntityID,DepotID,Folio,DateDocument,LanguageID,CurrencyID,Rate,SubTotal,Total,TotalTax,"
                         + "CreatedOn, CreatedBy,Custom1,salesrepContactid,Comments)"
                         + "VALUES (1214, 40, 1, 1, 2, 1, ?, GETDATE(), 3, 3, 1, 0, 0, 0, GETDATE(), 1, ?, ?, ?);"
                         + "SELECT SCOPE_IDENTITY();";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, myJsonArray.getJSONObject(i).getString("Consecutive"));
            ps2.setString(2, myJsonArray.getJSONObject(i).getString("GUID"));
            ps2.setString(3, salesrepContactid+"");
            ps2.setString(4, ja_values.getJSONObject(8).getString("Value"));
            rs = ps2.executeQuery();
            int iddoc = -2; // If this number stays like this, it indicates an error
            while (rs.next()) {
               iddoc = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }

            // This portion on the code gets the details for the location of the current Activity
            // Details like unidad medica and centro de costos
            urlParameters = "tkn="+accessToken +
                            "&LocationGUID="+myJsonObject.getString("LocationGUID");
            System.out.println("Sending Request to:\n"+url3+"?"+urlParameters);

            http.setUrl(url3);
            http.setUrlParams(urlParameters);

            response = http.sendPost();
            centroCostos = new JSONObject(response).getJSONArray("Values").getJSONObject(0).getString("Value");
            System.out.println(centroCostos);
            // -------------------------------------------------

            if (ja_values.getJSONObject(4).getString("Value").compareTo( "Emergencia" ) == 0) {
              // The service IS an Emergency
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,1,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            } else {
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,0,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            }

            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, centroCostos);
            ps2.setString(2, ja_values.getJSONObject(5).getString("Value"));
            ps2.execute();

            JSONArray prodJsonArray = ja_values.getJSONObject(17).getJSONArray("Value");

            for (int j=0; j < prodJsonArray.length(); j++) {
              queryString4 = "SELECT productId, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              if (rs.next()) {
                // Product is al ready in table
                System.out.println("Product Already In  Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                idProd = Integer.parseInt( rs.getString(1) );
                System.out.println(idProd);
              } else {
                // New product has to be registered
                System.out.println("Product Has Not Been Registered In Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                queryString4 = "INSERT INTO orgProduct (productName,unit,unitsale,availableForSale, newProduct, canSaleNegative, productTypeID)"
                             + "VALUES ('"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "1, "
                             + "1, "
                             + "1, "
                             + "1);";
                //System.out.println(queryString4);
                ps2 = conn.prepareStatement(queryString4);
                ps2.execute();
              }
              //System.out.println("Look for: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));

              // Now we are sure that the product is registered in Contpaq
              // We get its ID
              queryString4 = "SELECT productId, priceList, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              rs.next();
              idProd = Integer.parseInt( rs.getString(1) );
              precioLista = Float.parseFloat( rs.getString(2) );
              //System.out.println("Going to insert product: "+prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value")+ " with ID: "+ idProd);

              // We insert into docDocumentItem table in sqlserver
              queryString4 = "INSERT INTO docDocumentItem(DocumentID,mustBeDelivered,Quantity,CantSolicitada,ProductID,[Description],TipoPago,DiscountPerc,TaxTypeID,TaxPerc,UnitPrice,Total,CostPrice,LineNumber,ProductId1)"
                           + "VALUES ("+iddoc+", "
                           + "1, "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + "0, "
                           + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                           + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                           + "0, "
                           + "5, "
                           + "0.16, "
                           + "0, "
                           + "0, "
                           + precioLista+ ", " // Cost Price
                           + ""+ (j+1) +", "
                           + idProd+");"; // ProductID
              System.out.println(queryString4);
              ps2 = conn.prepareStatement(queryString4);
              ps2.execute();
            }
          }
          //
          if ( myJsonObject.getString("FormGUID").compareTo( "0DA1D3EC-264F-4083-9B58-0A399AF885B6" ) == 0 ) {
            System.out.println("nuevo formulario");
            // Form1 copia1
            //System.out.println("Form 1 copia1 GUID detected");
            /*FORMULARIO 1 METRO*/

            ja_values = myJsonObject.getJSONArray("Values");

            // This Part of the code gets the salesrepContactid(number) from the name of the Tecnico
            salesrepContactid = 0;
            queryString3 = "SELECT ContactID FROM vwLBSContactList WHERE ContactName=?";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, ja_values.getJSONObject(3).getString("Value"));
            rs = ps2.executeQuery();
            if (rs.next()) {
               // If we found the name in the table, then we assign its corresponding ID
               salesrepContactid = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }
            //System.out.println("Found: " + ja_values.getJSONObject(3).getString("Value") + "\nID: "+ salesrepContactid);
            // ---------------------------------------------------

            // Falta Buscar en BusinessEntityID en tabla (por ahora se pone 2 por default)
            queryString3 = "INSERT INTO docDocument(ModuleID,documentTypeID,DocRecipientID,OwnedBusinessEntityID,BusinessEntityID,DepotID,Folio,DateDocument,LanguageID,CurrencyID,Rate,SubTotal,Total,TotalTax,"
                         + "CreatedOn, CreatedBy,Custom1,salesrepContactid,Comments)"
                         + "VALUES (1214, 40, 1, 1, 2, 1, ?, GETDATE(), 3, 3, 1, 0, 0, 0, GETDATE(), 1, ?, ?, ?);"
                         + "SELECT SCOPE_IDENTITY();";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, myJsonArray.getJSONObject(i).getString("Consecutive"));
            ps2.setString(2, myJsonArray.getJSONObject(i).getString("GUID"));
            ps2.setString(3, salesrepContactid+"");
            ps2.setString(4, ja_values.getJSONObject(8).getString("Value"));
            rs = ps2.executeQuery();
            int iddoc = -2; // If this number stays like this, it indicates an error
            while (rs.next()) {
               iddoc = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }

            // This portion on the code gets the details for the location of the current Activity
            // Details like unidad medica and centro de costos
            urlParameters = "tkn="+accessToken +
                            "&LocationGUID="+myJsonObject.getString("LocationGUID");
            //System.out.println("Sending Request to:\n"+url3+"?"+urlParameters);

            http.setUrl(url3);
            http.setUrlParams(urlParameters);

            response = http.sendPost();
            centroCostos = new JSONObject(response).getJSONArray("Values").getJSONObject(0).getString("Value");
            //System.out.println(centroCostos);
            // -------------------------------------------------

            if (ja_values.getJSONObject(4).getString("Value").compareTo( "Emergencia" ) == 0) {
              // The service IS an Emergency
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,1,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            } else {
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,0,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            }

            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, centroCostos);
            ps2.setString(2, ja_values.getJSONObject(5).getString("Value"));
            ps2.execute();

            JSONArray prodJsonArray = ja_values.getJSONObject(17).getJSONArray("Value");

            for (int j=0; j < prodJsonArray.length(); j++) {
              queryString4 = "SELECT productId, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              if (rs.next()) {
                // Product is al ready in table
                System.out.println("Product Already In  Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                idProd = Integer.parseInt( rs.getString(1) );
                System.out.println(idProd);
              } else {
                // New product has to be registered
                System.out.println("Product Has Not Been Registered In Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                queryString4 = "INSERT INTO orgProduct (productName,unit,unitsale,availableForSale, newProduct, canSaleNegative, productTypeID)"
                             + "VALUES ('"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "1, "
                             + "1, "
                             + "1, "
                             + "1);";
                //System.out.println(queryString4);
                ps2 = conn.prepareStatement(queryString4);
                ps2.execute();
              }
              //System.out.println("Look for: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));

              // Now we are sure that the product is registered in Contpaq
              // We get its ID
              queryString4 = "SELECT productId, priceList, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              rs.next();
              idProd = Integer.parseInt( rs.getString(1) );
              precioLista = Float.parseFloat( rs.getString(2) );
              //System.out.println("Going to insert product: "+prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value")+ " with ID: "+ idProd);

              // We insert into docDocumentItem table in sqlserver
              queryString4 = "INSERT INTO docDocumentItem(DocumentID,mustBeDelivered,Quantity,CantSolicitada,ProductID,[Description],TipoPago,DiscountPerc,TaxTypeID,TaxPerc,UnitPrice,Total,CostPrice,LineNumber,ProductId1)"
                           + "VALUES ("+iddoc+", "
                           + "1, "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + "0, "
                           + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                           + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                           + "0, "
                           + "5, "
                           + "0.16, "
                           + "0, "
                           + "0, "
                           + precioLista+ ", " // Cost Price
                           + ""+ (j+1) +", "
                           + idProd+");"; // ProductID
              System.out.println(queryString4);
              ps2 = conn.prepareStatement(queryString4);
              ps2.execute();
            }
          }
          //
          if ( myJsonObject.getString("FormGUID").compareTo( "24F36729-3759-4291-97D3-19579309B4AF" ) == 0 ) {
               System.out.println("nuevo formulario");
            // Form1 copia1
            //FOMULARIO 1 FAMSA//
            //System.out.println("Form 1 copia1 GUID detected");

            ja_values = myJsonObject.getJSONArray("Values");

            // This Part of the code gets the salesrepContactid(number) from the name of the Tecnico
            salesrepContactid = 0;
            queryString3 = "SELECT ContactID FROM vwLBSContactList WHERE ContactName=?";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, ja_values.getJSONObject(3).getString("Value"));
            rs = ps2.executeQuery();
            if (rs.next()) {
               // If we found the name in the table, then we assign its corresponding ID
               salesrepContactid = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }
            //System.out.println("Found: " + ja_values.getJSONObject(3).getString("Value") + "\nID: "+ salesrepContactid);
            // ---------------------------------------------------

            // Falta Buscar en BusinessEntityID en tabla (por ahora se pone 2 por default)
            queryString3 = "INSERT INTO docDocument(ModuleID,documentTypeID,DocRecipientID,OwnedBusinessEntityID,BusinessEntityID,DepotID,Folio,DateDocument,LanguageID,CurrencyID,Rate,SubTotal,Total,TotalTax,"
                         + "CreatedOn, CreatedBy,Custom1,salesrepContactid,Comments)"
                         + "VALUES (1214, 40, 1, 1, 2, 1, ?, GETDATE(), 3, 3, 1, 0, 0, 0, GETDATE(), 1, ?, ?, ?);"
                         + "SELECT SCOPE_IDENTITY();";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, myJsonArray.getJSONObject(i).getString("Consecutive"));
            ps2.setString(2, myJsonArray.getJSONObject(i).getString("GUID"));
            ps2.setString(3, salesrepContactid+"");
            ps2.setString(4, ja_values.getJSONObject(8).getString("Value"));
            rs = ps2.executeQuery();
            int iddoc = -2; // If this number stays like this, it indicates an error
            while (rs.next()) {
               iddoc = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }

            // This portion on the code gets the details for the location of the current Activity
            // Details like unidad medica and centro de costos
            urlParameters = "tkn="+accessToken +
                            "&LocationGUID="+myJsonObject.getString("LocationGUID");
            //System.out.println("Sending Request to:\n"+url3+"?"+urlParameters);

            http.setUrl(url3);
            http.setUrlParams(urlParameters);

            response = http.sendPost();
            centroCostos = new JSONObject(response).getJSONArray("Values").getJSONObject(0).getString("Value");
            //System.out.println(centroCostos);
            // -------------------------------------------------

            if (ja_values.getJSONObject(4).getString("Value").compareTo( "Emergencia" ) == 0) {
              // The service IS an Emergency
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,1,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            } else {
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,0,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            }

            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, centroCostos);
            ps2.setString(2, ja_values.getJSONObject(5).getString("Value"));
            ps2.execute();

            JSONArray prodJsonArray = ja_values.getJSONObject(17).getJSONArray("Value");

            for (int j=0; j < prodJsonArray.length(); j++) {
              queryString4 = "SELECT productId, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              if (rs.next()) {
                // Product is al ready in table
                System.out.println("Product Already In  Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                idProd = Integer.parseInt( rs.getString(1) );
                System.out.println(idProd);
              } else {
                // New product has to be registered
                System.out.println("Product Has Not Been Registered In Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                queryString4 = "INSERT INTO orgProduct (productName,unit,unitsale,availableForSale, newProduct, canSaleNegative, productTypeID)"
                             + "VALUES ('"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "1, "
                             + "1, "
                             + "1, "
                             + "1);";
                //System.out.println(queryString4);
                ps2 = conn.prepareStatement(queryString4);
                ps2.execute();
              }
              //System.out.println("Look for: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));

              // Now we are sure that the product is registered in Contpaq
              // We get its ID
              queryString4 = "SELECT productId, priceList, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              rs.next();
              idProd = Integer.parseInt( rs.getString(1) );
              precioLista = Float.parseFloat( rs.getString(2) );
              //System.out.println("Going to insert product: "+prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value")+ " with ID: "+ idProd);

              // We insert into docDocumentItem table in sqlserver
              queryString4 = "INSERT INTO docDocumentItem(DocumentID,mustBeDelivered,Quantity,CantSolicitada,ProductID,[Description],TipoPago,DiscountPerc,TaxTypeID,TaxPerc,UnitPrice,Total,CostPrice,LineNumber,ProductId1)"
                           + "VALUES ("+iddoc+", "
                           + "1, "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + "0, "
                           + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                           + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                           + "0, "
                           + "5, "
                           + "0.16, "
                           + "0, "
                           + "0, "
                           + precioLista+ ", " // Cost Price
                           + ""+ (j+1) +", "
                           + idProd+");"; // ProductID
              System.out.println(queryString4);
              ps2 = conn.prepareStatement(queryString4);
              ps2.execute();
            }
          }
          //prueba de formularios
          if ( myJsonObject.getString("FormGUID").compareTo( "960DC7E7-6A51-4AB0-A28F-B3E174901140" ) == 0 ) {
            System.out.println("nuevo formulario");
            // Form1 copia1
            //System.out.println("Form 1 copia1 GUID detected");
            /*FORMULARIO LEVANTAMIENTO Y REQUISISION*/

            ja_values = myJsonObject.getJSONArray("Values");

            // This Part of the code gets the salesrepContactid(number) from the name of the Tecnico
            salesrepContactid = 0;
            queryString3 = "SELECT ContactID FROM vwLBSContactList WHERE ContactName=?";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, ja_values.getJSONObject(3).getString("Value"));
            rs = ps2.executeQuery();
            if (rs.next()) {
               // If we found the name in the table, then we assign its corresponding ID
               salesrepContactid = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }
            //System.out.println("Found: " + ja_values.getJSONObject(3).getString("Value") + "\nID: "+ salesrepContactid);
            // ---------------------------------------------------

            // Falta Buscar en BusinessEntityID en tabla (por ahora se pone 2 por default)
            queryString3 = "INSERT INTO docDocument(ModuleID,documentTypeID,DocRecipientID,OwnedBusinessEntityID,BusinessEntityID,DepotID,Folio,DateDocument,LanguageID,CurrencyID,Rate,SubTotal,Total,TotalTax,"
                         + "CreatedOn, CreatedBy,Custom1,salesrepContactid,Comments)"
                         + "VALUES (1214, 40, 1, 1, 2, 1, ?, GETDATE(), 3, 3, 1, 0, 0, 0, GETDATE(), 1, ?, ?, ?);"
                         + "SELECT SCOPE_IDENTITY();";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, myJsonArray.getJSONObject(i).getString("Consecutive"));
            ps2.setString(2, myJsonArray.getJSONObject(i).getString("GUID"));
            ps2.setString(3, salesrepContactid+"");
            ps2.setString(4, ja_values.getJSONObject(8).getString("Value"));
            rs = ps2.executeQuery();
            int iddoc = -2; // If this number stays like this, it indicates an error
            while (rs.next()) {
               iddoc = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }

            // This portion on the code gets the details for the location of the current Activity
            // Details like unidad medica and centro de costos
            urlParameters = "tkn="+accessToken +
                            "&LocationGUID="+myJsonObject.getString("LocationGUID");
            //System.out.println("Sending Request to:\n"+url3+"?"+urlParameters);

            http.setUrl(url3);
            http.setUrlParams(urlParameters);

            response = http.sendPost();
            centroCostos = new JSONObject(response).getJSONArray("Values").getJSONObject(0).getString("Value");
            //System.out.println(centroCostos);
            // -------------------------------------------------

            if (ja_values.getJSONObject(4).getString("Value").compareTo( "Emergencia" ) == 0) {
              // The service IS an Emergency
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,1,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            } else {
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,0,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            }

            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, centroCostos);
            ps2.setString(2, ja_values.getJSONObject(5).getString("Value"));
            ps2.execute();

            JSONArray prodJsonArray = ja_values.getJSONObject(17).getJSONArray("Value");

            for (int j=0; j < prodJsonArray.length(); j++) {
              queryString4 = "SELECT productId, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              if (rs.next()) {
                // Product is al ready in table
                System.out.println("Product Already In  Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                idProd = Integer.parseInt( rs.getString(1) );
                System.out.println(idProd);
              } else {
                // New product has to be registered
                System.out.println("Product Has Not Been Registered In Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                queryString4 = "INSERT INTO orgProduct (productName,unit,unitsale,availableForSale, newProduct, canSaleNegative, productTypeID)"
                             + "VALUES ('"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "1, "
                             + "1, "
                             + "1, "
                             + "1);";
                //System.out.println(queryString4);
                ps2 = conn.prepareStatement(queryString4);
                ps2.execute();
              }
              //System.out.println("Look for: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));

              // Now we are sure that the product is registered in Contpaq
              // We get its ID
              queryString4 = "SELECT productId, priceList, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              rs.next();
              idProd = Integer.parseInt( rs.getString(1) );
              precioLista = Float.parseFloat( rs.getString(2) );
              //System.out.println("Going to insert product: "+prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value")+ " with ID: "+ idProd);

              // We insert into docDocumentItem table in sqlserver
              queryString4 = "INSERT INTO docDocumentItem(DocumentID,mustBeDelivered,Quantity,CantSolicitada,ProductID,[Description],TipoPago,DiscountPerc,TaxTypeID,TaxPerc,UnitPrice,Total,CostPrice,LineNumber,ProductId1)"
                           + "VALUES ("+iddoc+", "
                           + "1, "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + "0, "
                           + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                           + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                           + "0, "
                           + "5, "
                           + "0.16, "
                           + "0, "
                           + "0, "
                           + precioLista+ ", " // Cost Price
                           + ""+ (j+1) +", "
                           + idProd+");"; // ProductID
              System.out.println(queryString4);
              ps2 = conn.prepareStatement(queryString4);
              ps2.execute();
            }
          }
          //
          if ( myJsonObject.getString("FormGUID").compareTo( "13E5EECF-AF5D-468C-9F2A-B9330A2CD8B1" ) == 0 ) {
            System.out.println("nuevo formulario");
            // Form1 copia1
            /* FOMULARIO 1 BAFAR*/
            //System.out.println("Form 1 copia1 GUID detected");

            ja_values = myJsonObject.getJSONArray("Values");

            // This Part of the code gets the salesrepContactid(number) from the name of the Tecnico
            salesrepContactid = 0;
            queryString3 = "SELECT ContactID FROM vwLBSContactList WHERE ContactName=?";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, ja_values.getJSONObject(3).getString("Value"));
            rs = ps2.executeQuery();
            if (rs.next()) {
               // If we found the name in the table, then we assign its corresponding ID
               salesrepContactid = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }
            //System.out.println("Found: " + ja_values.getJSONObject(3).getString("Value") + "\nID: "+ salesrepContactid);
            // ---------------------------------------------------

            // Falta Buscar en BusinessEntityID en tabla (por ahora se pone 2 por default)
            queryString3 = "INSERT INTO docDocument(ModuleID,documentTypeID,DocRecipientID,OwnedBusinessEntityID,BusinessEntityID,DepotID,Folio,DateDocument,LanguageID,CurrencyID,Rate,SubTotal,Total,TotalTax,"
                         + "CreatedOn, CreatedBy,Custom1,salesrepContactid,Comments)"
                         + "VALUES (1214, 40, 1, 1, 2, 1, ?, GETDATE(), 3, 3, 1, 0, 0, 0, GETDATE(), 1, ?, ?, ?);"
                         + "SELECT SCOPE_IDENTITY();";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, myJsonArray.getJSONObject(i).getString("Consecutive"));
            ps2.setString(2, myJsonArray.getJSONObject(i).getString("GUID"));
            ps2.setString(3, salesrepContactid+"");
            ps2.setString(4, ja_values.getJSONObject(8).getString("Value"));
            rs = ps2.executeQuery();
            int iddoc = -2; // If this number stays like this, it indicates an error
            while (rs.next()) {
               iddoc = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }

            // This portion on the code gets the details for the location of the current Activity
            // Details like unidad medica and centro de costos
            urlParameters = "tkn="+accessToken +
                            "&LocationGUID="+myJsonObject.getString("LocationGUID");
            System.out.println("Sending Request to:\n"+url3+"?"+urlParameters);

            http.setUrl(url3);
            http.setUrlParams(urlParameters);

            response = http.sendPost();
            centroCostos = new JSONObject(response).getJSONArray("Values").getJSONObject(0).getString("Value");
            System.out.println(centroCostos);
            // -------------------------------------------------

            if (ja_values.getJSONObject(4).getString("Value").compareTo( "Emergencia" ) == 0) {
              // The service IS an Emergency
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,1,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            } else {
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,0,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            }

            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, centroCostos);
            ps2.setString(2, ja_values.getJSONObject(5).getString("Value"));
            ps2.execute();

            JSONArray prodJsonArray = ja_values.getJSONObject(17).getJSONArray("Value");

            for (int j=0; j < prodJsonArray.length(); j++) {
              queryString4 = "SELECT productId, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              if (rs.next()) {
                // Product is al ready in table
                System.out.println("Product Already In  Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                idProd = Integer.parseInt( rs.getString(1) );
                System.out.println(idProd);
              } else {
                // New product has to be registered
                System.out.println("Product Has Not Been Registered In Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                queryString4 = "INSERT INTO orgProduct (productName,unit,unitsale,availableForSale, newProduct, canSaleNegative, productTypeID)"
                             + "VALUES ('"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "1, "
                             + "1, "
                             + "1, "
                             + "1);";
                //System.out.println(queryString4);
                ps2 = conn.prepareStatement(queryString4);
                ps2.execute();
              }
              //System.out.println("Look for: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));

              // Now we are sure that the product is registered in Contpaq
              // We get its ID
              queryString4 = "SELECT productId, priceList, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              rs.next();
              idProd = Integer.parseInt( rs.getString(1) );
              precioLista = Float.parseFloat( rs.getString(2) );
              //System.out.println("Going to insert product: "+prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value")+ " with ID: "+ idProd);

              // We insert into docDocumentItem table in sqlserver
              queryString4 = "INSERT INTO docDocumentItem(DocumentID,mustBeDelivered,Quantity,CantSolicitada,ProductID,[Description],TipoPago,DiscountPerc,TaxTypeID,TaxPerc,UnitPrice,Total,CostPrice,LineNumber,ProductId1)"
                           + "VALUES ("+iddoc+", "
                           + "1, "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + "0, "
                           + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                           + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                           + "0, "
                           + "5, "
                           + "0.16, "
                           + "0, "
                           + "0, "
                           + precioLista+ ", " // Cost Price
                           + ""+ (j+1) +", "
                           + idProd+");"; // ProductID
              System.out.println(queryString4);
              ps2 = conn.prepareStatement(queryString4);
              ps2.execute();
            }
          }
          //
          if ( myJsonObject.getString("FormGUID").compareTo( "9119AA9A-3D7A-4F5F-85FD-F58E930131D9" ) == 0 ) {
            // Form1 ISSEMYM detected
            //System.out.println("Form 1 ISSEMYM GUID detected");

            ja_values = myJsonObject.getJSONArray("Values");

            // This Part of the code gets the salesrepContactid(number) from the name of the Tecnico
            salesrepContactid = 0;
            queryString3 = "SELECT ContactID FROM vwLBSContactList WHERE ContactName=?";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, ja_values.getJSONObject(3).getString("Value"));
            rs = ps2.executeQuery();
            if (rs.next()) {
               // If we found the name in the table, then we assign its corresponding ID
               salesrepContactid = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }
            //System.out.println("Found: " + ja_values.getJSONObject(3).getString("Value") + "\nID: "+ salesrepContactid);
            // ---------------------------------------------------

            // Falta Buscar en BusinessEntityID en tabla (por ahora se pone 2 por default)
            queryString3 = "INSERT INTO docDocument(ModuleID,documentTypeID,DocRecipientID,OwnedBusinessEntityID,BusinessEntityID,DepotID,Folio,DateDocument,LanguageID,CurrencyID,Rate,SubTotal,Total,TotalTax,"
    			               + "CreatedOn, CreatedBy,Custom1,salesrepContactid,Comments)"
                         + "VALUES (1214, 40, 1, 1, 2, 1, ?, GETDATE(), 3, 3, 1, 0, 0, 0, GETDATE(), 1, ?, ?, ?);"
                         + "SELECT SCOPE_IDENTITY();";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, myJsonArray.getJSONObject(i).getString("Consecutive"));
            ps2.setString(2, myJsonArray.getJSONObject(i).getString("GUID"));
            ps2.setString(3, salesrepContactid+"");
            ps2.setString(4, ja_values.getJSONObject(8).getString("Value"));
            rs = ps2.executeQuery();
            int iddoc = -2; // If this number stays like this, it indicates an error
            while (rs.next()) {
               iddoc = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }

            // This portion on the code gets the details for the location of the current Activity
            // Details like unidad medica and centro de costos
            urlParameters = "tkn="+accessToken +
                            "&LocationGUID="+myJsonObject.getString("LocationGUID");
            //System.out.println("Sending Request to:\n"+url3+"?"+urlParameters);

            http.setUrl(url3);
            http.setUrlParams(urlParameters);

            response = http.sendPost();
            centroCostos = new JSONObject(response).getJSONArray("Values").getJSONObject(0).getString("Value");
            //System.out.println(centroCostos);
            // -------------------------------------------------

            if (ja_values.getJSONObject(4).getString("Value").compareTo( "Emergencia" ) == 0) {
              // The service IS an Emergency
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,1,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            } else {
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,0,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            }

            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, centroCostos);
            ps2.setString(2, ja_values.getJSONObject(5).getString("Value"));
            ps2.execute();

            JSONArray prodJsonArray = ja_values.getJSONObject(17).getJSONArray("Value");

            for (int j=0; j < prodJsonArray.length(); j++) {
              queryString4 = "SELECT productId, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              if (rs.next()) {
                // Product is al ready in table
                System.out.println("Product Already In  Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                idProd = Integer.parseInt( rs.getString(1) );
                System.out.println(idProd);
              } else {
                // New product has to be registered
                System.out.println("Product Has Not Been Registered In Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                queryString4 = "INSERT INTO orgProduct (productName,unit,unitsale,availableForSale, newProduct, canSaleNegative, productTypeID)"
                             + "VALUES ('"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "1, "
                             + "1, "
                             + "1, "
                             + "1);";
                //System.out.println(queryString4);
                ps2 = conn.prepareStatement(queryString4);
                ps2.execute();
              }
              //System.out.println("Look for: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));

              // Now we are sure that the product is registered in Contpaq
              // We get its ID
              queryString4 = "SELECT productId, priceList, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              rs.next();
              idProd = Integer.parseInt( rs.getString(1) );
              precioLista = Float.parseFloat( rs.getString(2) );
              //System.out.println("Going to insert product: "+prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value")+ " with ID: "+ idProd);

              // We insert into docDocumentItem table in sqlserver
              queryString4 = "INSERT INTO docDocumentItem(DocumentID,mustBeDelivered,Quantity,CantSolicitada,ProductID,[Description],TipoPago,DiscountPerc,TaxTypeID,TaxPerc,UnitPrice,Total,CostPrice,LineNumber,ProductId1)"
                           + "VALUES ("+iddoc+", "
                           + "1, "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + "0, " // + idProd + ", "
                           + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                           + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                           + "0, "
                           + "5, "
                           + "0.16, "
                           + "0, "
                           + "0, "
                           + precioLista+ ", " // Cost Price
                           + ""+ (j+1) +", "
                           + idProd+");"; // ProductID
              System.out.println(queryString4);
              ps2 = conn.prepareStatement(queryString4);
              ps2.execute();
            }
          }
          //preubas de funcion
          if ( myJsonObject.getString("FormGUID").compareTo( "03EE54D8-FEE5-48FE-9CE5-2F27F2A212E3" ) == 0 ) {
            // Form3  ISSSTE detected
            //( 3 REPORTE DE SERVICIO ISSSTE)
            //System.out.println("Form 1 ISSEMYM GUID detected");

            ja_values = myJsonObject.getJSONArray("Values");

            // This Part of the code gets the salesrepContactid(number) from the name of the Tecnico
            salesrepContactid = 0;
            queryString3 = "SELECT ContactID FROM vwLBSContactList WHERE ContactName=?";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, ja_values.getJSONObject(3).getString("Value"));
            rs = ps2.executeQuery();
            if (rs.next()) {
               // If we found the name in the table, then we assign its corresponding ID
               salesrepContactid = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }
            //System.out.println("Found: " + ja_values.getJSONObject(3).getString("Value") + "\nID: "+ salesrepContactid);
            // ---------------------------------------------------

            // Falta Buscar en BusinessEntityID en tabla (por ahora se pone 2 por default)
            queryString3 = "INSERT INTO docDocument(ModuleID,documentTypeID,DocRecipientID,OwnedBusinessEntityID,BusinessEntityID,DepotID,Folio,DateDocument,LanguageID,CurrencyID,Rate,SubTotal,Total,TotalTax,"
    			               + "CreatedOn, CreatedBy,Custom1,salesrepContactid,Comments)"
                         + "VALUES (1214, 40, 1, 1, 2, 1, ?, GETDATE(), 3, 3, 1, 0, 0, 0, GETDATE(), 1, ?, ?, ?);"
                         + "SELECT SCOPE_IDENTITY();";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, myJsonArray.getJSONObject(i).getString("Consecutive"));
            ps2.setString(2, myJsonArray.getJSONObject(i).getString("GUID"));
            ps2.setString(3, salesrepContactid+"");
            ps2.setString(4, ja_values.getJSONObject(8).getString("Value"));
            rs = ps2.executeQuery();
            int iddoc = -2; // If this number stays like this, it indicates an error
            while (rs.next()) {
               iddoc = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }

            // This portion on the code gets the details for the location of the current Activity
            // Details like unidad medica and centro de costos
            urlParameters = "tkn="+accessToken +
                            "&LocationGUID="+myJsonObject.getString("LocationGUID");
            //System.out.println("Sending Request to:\n"+url3+"?"+urlParameters);

            http.setUrl(url3);
            http.setUrlParams(urlParameters);

            response = http.sendPost();
            centroCostos = new JSONObject(response).getJSONArray("Values").getJSONObject(0).getString("Value");
            //System.out.println(centroCostos);
            // -------------------------------------------------

            if (ja_values.getJSONObject(4).getString("Value").compareTo( "Emergencia" ) == 0) {
              // The service IS an Emergency
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,1,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            } else {
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,0,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            }

            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, centroCostos);
            ps2.setString(2, ja_values.getJSONObject(5).getString("Value"));
            ps2.execute();

            JSONArray prodJsonArray = ja_values.getJSONObject(17).getJSONArray("Value");

            for (int j=0; j < prodJsonArray.length(); j++) {
              queryString4 = "SELECT productId, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              if (rs.next()) {
                // Product is al ready in table
                System.out.println("Product Already In  Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                idProd = Integer.parseInt( rs.getString(1) );
                System.out.println(idProd);
              } else {
                // New product has to be registered
                System.out.println("Product Has Not Been Registered In Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                queryString4 = "INSERT INTO orgProduct (productName,unit,unitsale,availableForSale, newProduct, canSaleNegative, productTypeID)"
                             + "VALUES ('"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "1, "
                             + "1, "
                             + "1, "
                             + "1);";
                //System.out.println(queryString4);
                ps2 = conn.prepareStatement(queryString4);
                ps2.execute();
              }
              //System.out.println("Look for: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));

              // Now we are sure that the product is registered in Contpaq
              // We get its ID
              queryString4 = "SELECT productId, priceList, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              rs.next();
              idProd = Integer.parseInt( rs.getString(1) );
              precioLista = Float.parseFloat( rs.getString(2) );
              //System.out.println("Going to insert product: "+prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value")+ " with ID: "+ idProd);

              // We insert into docDocumentItem table in sqlserver
              queryString4 = "INSERT INTO docDocumentItem(DocumentID,mustBeDelivered,Quantity,CantSolicitada,ProductID,[Description],TipoPago,DiscountPerc,TaxTypeID,TaxPerc,UnitPrice,Total,CostPrice,LineNumber,ProductId1)"
                           + "VALUES ("+iddoc+", "
                           + "1, "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + "0, " // + idProd + ", "
                           + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                           + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                           + "0, "
                           + "5, "
                           + "0.16, "
                           + "0, "
                           + "0, "
                           + precioLista+ ", " // Cost Price
                           + ""+ (j+1) +", "
                           + idProd+");"; // ProductID
              System.out.println(queryString4);
              ps2 = conn.prepareStatement(queryString4);
              ps2.execute();
            }
          }
          //
          if ( myJsonObject.getString("FormGUID").compareTo( "93BA9A06-0893-46CA-8887-7670DB6E9BA9" ) == 0 ) {
            // Form1 ISSEMYM detected
            //System.out.println("Form 1 ISSEMYM GUID detected");

            ja_values = myJsonObject.getJSONArray("Values");

            // This Part of the code gets the salesrepContactid(number) from the name of the Tecnico
            salesrepContactid = 0;
            queryString3 = "SELECT ContactID FROM vwLBSContactList WHERE ContactName=?";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, ja_values.getJSONObject(3).getString("Value"));
            rs = ps2.executeQuery();
            if (rs.next()) {
               // If we found the name in the table, then we assign its corresponding ID
               salesrepContactid = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }
            //System.out.println("Found: " + ja_values.getJSONObject(3).getString("Value") + "\nID: "+ salesrepContactid);
            // ---------------------------------------------------

            // Falta Buscar en BusinessEntityID en tabla (por ahora se pone 2 por default)
            queryString3 = "INSERT INTO docDocument(ModuleID,documentTypeID,DocRecipientID,OwnedBusinessEntityID,BusinessEntityID,DepotID,Folio,DateDocument,LanguageID,CurrencyID,Rate,SubTotal,Total,TotalTax,"
    			               + "CreatedOn, CreatedBy,Custom1,salesrepContactid,Comments)"
                         + "VALUES (1214, 40, 1, 1, 2, 1, ?, GETDATE(), 3, 3, 1, 0, 0, 0, GETDATE(), 1, ?, ?, ?);"
                         + "SELECT SCOPE_IDENTITY();";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, myJsonArray.getJSONObject(i).getString("Consecutive"));
            ps2.setString(2, myJsonArray.getJSONObject(i).getString("GUID"));
            ps2.setString(3, salesrepContactid+"");
            ps2.setString(4, ja_values.getJSONObject(8).getString("Value"));
            rs = ps2.executeQuery();
            int iddoc = -2; // If this number stays like this, it indicates an error
            while (rs.next()) {
               iddoc = Integer.parseInt( rs.getString(1) );
               //System.out.println(rs.getString(1));
            }

            // This portion on the code gets the details for the location of the current Activity
            // Details like unidad medica and centro de costos
            urlParameters = "tkn="+accessToken +
                            "&LocationGUID="+myJsonObject.getString("LocationGUID");
            //System.out.println("Sending Request to:\n"+url3+"?"+urlParameters);

            http.setUrl(url3);
            http.setUrlParams(urlParameters);

            response = http.sendPost();
            centroCostos = new JSONObject(response).getJSONArray("Values").getJSONObject(0).getString("Value");
            //System.out.println(centroCostos);
            // -------------------------------------------------

            if (ja_values.getJSONObject(4).getString("Value").compareTo( "Emergencia" ) == 0) {
              // The service IS an Emergency
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,1,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            } else {
              queryString3 = "INSERT INTO docDocumentExt (IDExtra,CC,Emergencia,NoTicket)"
                           + "SELECT "+iddoc+",?,0,?;"
                           + "INSERT INTO docDocumentCFD (DocumentID,FinancialOperationID,Anexo20Ver)"
                           + "SELECT "+iddoc+",0,'3.3'";
            }

            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, centroCostos);
            ps2.setString(2, ja_values.getJSONObject(5).getString("Value"));
            ps2.execute();

            JSONArray prodJsonArray = ja_values.getJSONObject(17).getJSONArray("Value");

            for (int j=0; j < prodJsonArray.length(); j++) {
              queryString4 = "SELECT productId, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              if (rs.next()) {
                // Product is al ready in table
                System.out.println("Product Already In  Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                idProd = Integer.parseInt( rs.getString(1) );
                System.out.println(idProd);
              } else {
                // New product has to be registered
                System.out.println("Product Has Not Been Registered In Table: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                queryString4 = "INSERT INTO orgProduct (productName,unit,unitsale,availableForSale, newProduct, canSaleNegative, productTypeID)"
                             + "VALUES ('"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                             + "1, "
                             + "1, "
                             + "1, "
                             + "1);";
                //System.out.println(queryString4);
                ps2 = conn.prepareStatement(queryString4);
                ps2.execute();
              }
              //System.out.println("Look for: " + prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));

              // Now we are sure that the product is registered in Contpaq
              // We get its ID
              queryString4 = "SELECT productId, priceList, productName FROM orgProduct WHERE productName = ?";
              ps2 = conn.prepareStatement(queryString4);
              ps2.setString(1, prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
              rs = ps2.executeQuery();
              rs.next();
              idProd = Integer.parseInt( rs.getString(1) );
              precioLista = Float.parseFloat( rs.getString(2) );
              //System.out.println("Going to insert product: "+prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value")+ " with ID: "+ idProd);

              // We insert into docDocumentItem table in sqlserver
              queryString4 = "INSERT INTO docDocumentItem(DocumentID,mustBeDelivered,Quantity,CantSolicitada,ProductID,[Description],TipoPago,DiscountPerc,TaxTypeID,TaxPerc,UnitPrice,Total,CostPrice,LineNumber,ProductId1)"
                           + "VALUES ("+iddoc+", "
                           + "1, "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + ""+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value") +", "
                           + "0, " // + idProd + ", "
                           + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value") +"', "
                           + "'"+ prodJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(4).getString("Value") +"', "
                           + "0, "
                           + "5, "
                           + "0.16, "
                           + "0, "
                           + "0, "
                           + precioLista+ ", " // Cost Price
                           + ""+ (j+1) +", "
                           + idProd+");"; // ProductID
              System.out.println(queryString4);
              ps2 = conn.prepareStatement(queryString4);
              ps2.execute();
            }
          }
          //
          if ( myJsonObject.getString("FormGUID").compareTo( "6YNp1YBrUC" ) == 0 ) {

            // Form3 ISSEMYM detected
            System.out.println("Form 3 GUID detected");

            ja_values = myJsonObject.getJSONArray("Values");
            //System.out.println(ja_values.getJSONObject(3).getString("Value"));

            // We look for the ID of the parent Requisition Document
            queryString3 = "SELECT * FROM docDocument WHERE folio=?";
            ps2 = conn.prepareStatement(queryString3);
            ps2.setString(1, ja_values.getJSONObject(3).getString("Value"));
            rs = ps2.executeQuery();
            int iddoc = -2; // If this number stays like this, it indicates an error

            if (rs.next()) {
              // The Requisition folio was found
              iddoc = Integer.parseInt( rs.getString(1) );
              System.out.println(iddoc +" - "+ ja_values.getJSONObject(3).getString("Value"));

              // Initialize all products to 0
              queryString3 = "UPDATE docDocumentItem "
                           + "SET CantUtilizada=0,Quantity=0 "
                           + "WHERE DocumentID="+iddoc;
              ps2 = conn.prepareStatement(queryString3);
              ps2.executeQuery();

              JSONArray refaccionesJsonArray = ja_values.getJSONObject(6).getJSONArray("Value");

              for (int j=0; j < refaccionesJsonArray.length(); j++) {
                //System.out.println(refaccionesJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));

                // We get the productID
                queryString4 = "SELECT productId, productName FROM orgProduct WHERE productName = ?";
                ps2 = conn.prepareStatement(queryString4);
                ps2.setString(1, refaccionesJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value"));
                rs = ps2.executeQuery();
                if (rs.next()) {
                  // Product found in table
                  idProd = Integer.parseInt( rs.getString(1) );
                  System.out.println(idProd + " - " + rs.getString(2));

                  // Update de Document Items of the Requisition (cantidadUtilizada & Quantity)
                  queryString3 = "UPDATE docDocumentItem "
                               + "SET CantUtilizada=?,Quantity=? "
                               + "WHERE DocumentID="+iddoc+" AND Description='"+refaccionesJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(0).getString("Value")+"'";
                  ps2 = conn.prepareStatement(queryString3);
                  ps2.setString(1, refaccionesJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value"));
                  ps2.setString(2, refaccionesJsonArray.getJSONObject(j).getJSONArray("Value").getJSONObject(3).getString("Value"));
                  System.out.println(queryString3);
                  ps2.executeUpdate();
                }

                // Update de Document of the Entrega Requisicion (Custom1)
                queryString3 = "UPDATE docDocument "
                             + "SET Custom1=? "
                             + "WHERE sourceDocumentID="+iddoc;
                ps2 = conn.prepareStatement(queryString3);
                ps2.setString(1, myJsonObject.getString("GUID"));
                ps2.executeUpdate();
              }
            }

          }
          //
        }
         /*else {
          // No need to insert
          // Becuase its already registered
          //System.out.println("No Need to insert: " + rs.getString(1));
        }*/

      }
    } catch (SQLException ex) {
      System.out.println("Error connecting to the DB");
      ex.printStackTrace();
    } catch (Exception e) {
      System.out.println("Exception Error: ");
      e.printStackTrace();
    } finally {
      try { rs.close(); } catch (Exception e) { /* ignored */ }
      try { ps.close(); } catch (Exception e) { /* ignored */ }
      try { ps2.close(); } catch (Exception e) { /* ignored */ }
      try { conn.close(); } catch (Exception e) { /* ignored */ }
    }
  }
}

class HttpURLConnection {
  String url;
  String urlParameters;
  private final String USER_AGENT = "Mozilla/5.0";

  public HttpURLConnection(String argUrl, String argUrlParams){
    this.url = argUrl;
    this.urlParameters = argUrlParams;
  }

  // HTTP POST request
	public String sendPost() throws Exception {
    URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

    //add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

    // Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();

    //int responseCode = con.getResponseCode();
		//System.out.println("\nSending 'POST' request to URL : " + url);
		//System.out.println("Post parameters : " + urlParameters);
		//System.out.println("Response Code : " + responseCode);

    BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream(), "UTF-8")); // Note the importance here of the "UTF-8" to receive ñ and á, etc
		String inputLine;
		StringBuffer response = new StringBuffer();

    while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

    //print result
		//System.out.println(response.toString());
    return response.toString();
  }

  public void setUrl(String newUrl){
    this.url = newUrl;
  }
  public void setUrlParams(String newUrlParams){
    this.urlParameters = newUrlParams;
  }
}

/*SELECT * FROM MyTestTable ORDER BY Fecha, Hora

DELETE FROM MyTestTable

INSERT INTO dbo.MyTestTable
VALUES ('973b4e3d-8b81-4246-bc4f-b5bfd632772e',
		'2019-09-05',
		'02:59 PM',
		'HOSPITALIZACION',
		'',
		'Correctivo',
		'MEDIA',
		'Funcionando',
		'',
		'',
		'',
		'',
		'',
		'',
		'',
		*/
