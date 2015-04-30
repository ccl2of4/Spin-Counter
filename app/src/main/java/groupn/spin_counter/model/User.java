package groupn.spin_counter.model;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by connor on 4/27/15.
 */
public class User implements Serializable {

    @SerializedName("user_id")
    public int userId;

    @SerializedName("mac_address")
    public String macAddress;

    @SerializedName("username")
    public String username;

    @SerializedName("max_spins")
    public int maxSpins;

    @SerializedName("games_won")
    public int gamesWon;

    @SerializedName("games_lost")
    public int gamesLost;

    @SerializedName("games_tied")
    public int gamesTied;

    public int getGamesPlayed () {
        return gamesWon + gamesLost + gamesTied;
    }

    // used for storing in shared preferences
    public String serialize() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
    public static User deserialize(String serializedData) {
        Gson gson = new Gson();
        return gson.fromJson(serializedData, User.class);
    }
}
