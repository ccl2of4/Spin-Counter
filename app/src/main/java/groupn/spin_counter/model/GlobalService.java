package groupn.spin_counter.model;

import java.util.List;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;

/**
 * Created by connor on 4/27/15.
 */
public interface GlobalService {
    @GET("/user.php")
    public void getUser (@Query("mac_address") int macAddress, Callback<User> callback);

    @GET("/leaderboard.php")
    public void getLeaderboard (Callback<List<User>> callback);

    @POST("/signup.php")
    public void postSignup (@Body int macAddress, @Body String username);

    @POST("/game.php")
    public void postGame (@Body String winnerUserId, @Body String loserUserId);

    @POST("/spin.php")
    public void postSpin (@Query("mac_address") int macAddress, @Body int spins);
}
