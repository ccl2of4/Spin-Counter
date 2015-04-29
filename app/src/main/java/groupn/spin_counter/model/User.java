package groupn.spin_counter.model;

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
}
