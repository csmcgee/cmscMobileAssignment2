package cmsc491.placepush.GoogleAPI;

import java.util.ArrayList;

public class GooglePlaceResponse {
    public ArrayList<Place> results;

    public static class Place {
        public Place(String name, String address, double lat, double lng){
            this.geometry = new Geometry(new Geometry.Location(lat, lng));
            this.formatted_address = address;
            this.name = name;
        }
        public Geometry geometry;
        public String id;
        public String formatted_address;
        public String name;

        public static class Geometry {
            public Geometry(Location location){
                this.location = location;
            }
            public Location location;

            public static class Location {
                public Location(double lat, double lng){
                    this.lat = lat;
                    this.lng = lng;
                }
                public double lat;
                public double lng;
            }
        }
    }

}