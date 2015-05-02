package groupn.spin_counter.model;

import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;

/**
 * Created by connor on 4/27/15.
 */
interface GlobalService {
    @GET("/user.php")
    public void getUser (@Query("mac_address") String macAddress, Callback<User> callback);

    @GET("/leaderboard.php")
    public void getLeaderboard (Callback<List<User>> callback);

    @POST("/signup.php")
    @FormUrlEncoded
    public void postSignup (@Field("mac_address") String macAddress, @Field("username") String username, Callback<User> callback);

    @POST("/game.php")
    @FormUrlEncoded
    public void postGame (@Field("player1_user_id") int player1UserId, @Field("player2_user_id") int player2UserId, @Field("player1_spins") int player1Spins, @Field("player2_spins") int player2Spins, Callback<Response> callback);

    @POST("/spin.php")
    @FormUrlEncoded
    public void postSpin (@Field("mac_address") String macAddress, @Field("spins") int spins, Callback<Response> callback);

    @POST("/username.php")
    @FormUrlEncoded
    public void postChangeUsername (@Field("mac_address") String macAddress, @Field("username") String username, Callback<User> callback);
}
