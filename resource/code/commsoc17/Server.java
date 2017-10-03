import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.sql.*;
import java.util.Date;
import java.text.SimpleDateFormat;

// 127.0.17.1
public class Server {

    // for now we are not calling the 
    private static String DestinationPhone = "+19723022013"; // this is the phone number that is called
    private static String AuthorizedCallerPhone = "+14693221354"; // this is our registered phone number

    private static final int PORT = 43005;
    private static final int WEBSERVER_PORT = 43006; // port number of NodeJS server
    private static final String WEBSERVER_ADDR = "localhost"; // ip address of NodeJS server

    // JDBC driver name and database URL
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost/traffic";

    //  Database credentials
    static final String USER = "anrl";
    static final String PASS = "";

    private static Connection conn = null;
    private static Statement stmt = null;

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // to change the date format to MySQL format

    // Printwriters for clients
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();

    public static void main(String[] args) throws Exception {
        System.out.println("Retrieving state...");
        System.out.println("Running server...");

        try {
            //STEP 2: Register JDBC driver
            Class.forName("com.mysql.jdbc.Driver");

            //STEP 3: Open a connection
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            System.out.println("Connected to the specified database successfully...");
            stmt = conn.createStatement();

        } catch (SQLException se) {
            se.printStackTrace();
        }

        ServerSocket listener = new ServerSocket(PORT);

        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            listener.close();
        }
    }

    private static class Handler extends Thread {

        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                // Create character streams for socket
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                writers.add(out);

                // Accept messages from this client and broadcast them
                // Ignore other clients that cannot be broadcast to
                while (true) {
                    String input = in.readLine();
                    // [latitude][space][longitude][space][speed][space][shock][space][type][space][driverId]
                    System.out.println("Read:" + input);
                    if (input == null) {
                        return;
                    }
                    String[] parts = input.split(" ");

                    String message;
                    if (parts[4].equalsIgnoreCase("1")){ // if type is 1 (Acciident
                        message = "Accident";
                    } else {
                        message = "Pothole";
                    }
//                    String message = generateMessage(Integer.parseInt(parts[3]), parts[4]);
                    int eventID = writeEventToDB(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], message);

                    sendEventIDtoNodeJSserver(eventID);

                    callnTextEmergencyContacts(parts[5], parts[0], parts[1]);

                    for (PrintWriter writer : writers) {
                        writer.println("MESSAGE: " + input);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SQLException ex) {
                ex.printStackTrace();
            } finally {

                if (out != null) {
                    writers.remove(out);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Sends the inserted event ID to NodeJS server
         *
         * @param eventID
         * @throws UnknownHostException
         */
        private static void sendEventIDtoNodeJSserver(int eventID) throws UnknownHostException {
            try {
                Socket socket = new Socket(WEBSERVER_ADDR, WEBSERVER_PORT);
                System.out.println("Connected to web server");
                // out & in
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                // writes str in the socket and read
                out.println(eventID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Writes the event to the DB, and returns the ID of the last inserted
         * event
         *
         * @param latitude
         * @param longitude
         * @param speed
         * @param shock
         * @param type
         * @param driverId
         * @return
         * @throws SQLException
         */
        private int writeEventToDB(String latitude, String longitude, String speed, String shock, String type, String driverId, String message) throws SQLException {

            Date date = new Date();
            String currentTime = sdf.format(date);

            String query = "INSERT INTO events (date, latitude, longitude, speed, shock, type, driverId, message)"
                    + " VALUES ('" + currentTime + "', '" + latitude + "', '" + longitude + "', '" + speed + "', '" + shock + "', '" + type + "', '" + driverId + ", '" + message + "');";
            stmt.executeUpdate(query);

            // get the last inserted ID
            query = "SELECT LAST_INSERT_ID() as id;";
            ResultSet rs = stmt.executeQuery(query);
            rs.next();
            return rs.getInt("id");
        }

        private static void callnTextEmergencyContacts(String DriverID, String latitude, String longitude) throws SQLException, IOException {
            String sql = "SELECT * from drivers where id='" + DriverID + "'";
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                //Retrieve by column name
                String name = rs.getString("name");
                String EmergencyName = rs.getString("emergencyName");
                String EmergencyContact = rs.getString("emergencyContact");
                String plate = rs.getString("licensePlate");

                System.out.println("****ACCIDENT****");
                String emergencyMessage = "Hello, We are sorry to deliver this urgent message." + name + ", driver of the car with licence plate number " + plate + " was involved in an accident now, at" + ReverseGeocode.getStreetAddress(latitude, longitude);
                System.out.println(emergencyMessage);
                System.out.println("Contacting 911 and emergency contact " + EmergencyName + " at " + " " + EmergencyContact + "...");

                Calling.textPhone(DestinationPhone, emergencyMessage, AuthorizedCallerPhone); // text the emergency contact

                Calling.createXML(emergencyMessage, "/home/ubuntu/nginx/public/static/call/v1.xml");
                Calling.callPhone(DestinationPhone, "http://ec2-54-213-224-194.us-west-2.compute.amazonaws.com/static/call/v1.xml", AuthorizedCallerPhone);

            }
            rs.close();
        }

        
        /**
         * This function is not completely written yet. It's just a skeleton. 
         * @param shock
         * @param biggestShockAxis
         * @return 
         */
        private static String generateMessage(int shock, String biggestShockAxis) {
            String message = "Accident from the ";
            if (shock < 0) {
                if (biggestShockAxis.equalsIgnoreCase("x")) {
                    message += "driver side";
                } else if (biggestShockAxis.equalsIgnoreCase("y")) {
                    message += "top";
                } else {

                }
            } else {
                if (biggestShockAxis.equalsIgnoreCase("x")) {
                    message += "passenger side";
                } else if (biggestShockAxis.equalsIgnoreCase("y")) {
                    message += "back";
                } else {
                    message += "bottom";
                }
            }

            return message;
        }

    }
}

