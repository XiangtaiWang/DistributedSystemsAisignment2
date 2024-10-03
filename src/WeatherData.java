import java.time.LocalDateTime;

public class WeatherData{
    public String id;
    public String name;
    public String state;
    public String time_zone;
    public float lat;
    public float lon;
    public String local_date_time;
    public String local_date_time_full;
    public float air_temp;
    public float apparent_t;
    public String cloud;
    public float dewpt;
    public float press;
    public int rel_hum;
    public String wind_dir;
    public int wind_spd_kmh;
    public int wind_spd_kt;
    public LocalDateTime lastUpdateTime;

    public void setProperty(String key, String value) {
        if (key.equals("id")) {
            id = value;
        }
        else if (key.equals("name")) {
            name = value;
        }
        else if (key.equals("state")) {
            state = value;
        }
        else if (key.equals("time_zone")) {
            time_zone = value;
        }
        else if (key.equals("lat")) {
            lat = Float.parseFloat(value);
        }
        else if (key.equals("lon")) {
            lon = Float.parseFloat(value);
        }
        else if (key.equals("local_date_time")) {
            local_date_time = value;
        }
        else if (key.equals("local_date_time_full")) {
            local_date_time_full = value;
        }
        else if (key.equals("air_temp")) {
            air_temp = Float.parseFloat(value);
        }
        else if (key.equals("apparent_t")) {
            apparent_t = Float.parseFloat(value);
        }
        else if (key.equals("cloud")) {
            cloud = value;
        }
        else if (key.equals("dewpt")) {
            dewpt = Float.parseFloat(value);
        }
        else if (key.equals("press")) {
            press = Float.parseFloat(value);
        }
        else if (key.equals("rel_hum")) {
            rel_hum = Integer.parseInt(value);
        }
        else if (key.equals("wind_dir")) {
            wind_dir = value;
        }
        else if (key.equals("wind_spd_kmh")) {
            wind_spd_kmh = Integer.parseInt(value);
        }
        else if (key.equals("wind_spd_kt")) {
            wind_spd_kt = Integer.parseInt(value);
        }
//        else {
//            throw new RuntimeException("Unknown property: " + key);
//        }
    }
    public void setLastUpdateTime() {
        lastUpdateTime = LocalDateTime.now();
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        WeatherData data = (WeatherData) obj;
        return id.equals(data.id);
    }
}
