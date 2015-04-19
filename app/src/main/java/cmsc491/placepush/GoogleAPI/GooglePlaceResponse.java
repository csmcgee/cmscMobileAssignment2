package cmsc491.placepush.GoogleAPI;

import java.util.ArrayList;

public class GooglePlaceResponse {
    public ArrayList<Place> results;

    public static class Place {
        public Geometry geometry;
        public String id;
        public String formatted_address;
        public String name;

        public static class Geometry {
            public Location location;

            public static class Location {
                public double lat;
                public double lng;
            }
        }
    }

}