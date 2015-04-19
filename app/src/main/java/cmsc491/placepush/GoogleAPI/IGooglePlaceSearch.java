package cmsc491.placepush.GoogleAPI;


public interface IGooglePlaceSearch {
    public String searchPlaces(String query, double lat, double lng);
}
