import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import org.json.JSONArray;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author ashkany
 */
public class ReverseGeocode {

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(bufferedReader);
            JSONObject json = new JSONObject(jsonText);
            return json;
        } finally {
            is.close();
        }
    }

    public static String getStreetAddress(String latitude, String longitude) throws IOException {
        String googleMapPath = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + latitude + "," + longitude + "&key=AIzaSyDicA-5I2AFVgZMw_8pJRUSZ0BkJgR2bfE";
        JSONObject json = readJsonFromUrl(googleMapPath);
        
        JSONArray results = (JSONArray) json.get("results");
        JSONObject firstComponent = (JSONObject) results.get(0);        
        return firstComponent.get("formatted_address").toString();
    }
    
//    public static void main(String[] args) throws IOException{
//        getStreetAddress("32.98", "-96.75");
//    }
}
