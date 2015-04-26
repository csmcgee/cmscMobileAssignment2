package cmsc491.placepush.GoogleAPI;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

public class PPGooglePlaceSearch implements IGooglePlaceSearch {

    private static final String API_KEY = "AIzaSyActXAVulKOryAr7XHTiYazqts0zOepouo";
    public static final String GOOGLE_URL = "https://maps.googleapis.com/maps/api/place/textsearch/json?key=" + API_KEY;

    @Override
    public String searchPlaces(String query, double lat, double lng) {
        try {
            query = URLEncoder.encode(query, "UTF-8");
            String full_url = String.format("%s&query=%s&location=%.6f,%.6f&radius=10000", GOOGLE_URL, query, lat, lng);
            URL url = new URL(full_url);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();

            String line;
            while((line = in.readLine()) != null)
                sb.append(line);

            connection.disconnect();
            return sb.toString();

        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
