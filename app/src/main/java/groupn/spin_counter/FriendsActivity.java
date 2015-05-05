package groupn.spin_counter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import groupn.spin_counter.model.DataRepository;
import groupn.spin_counter.model.User;

/**
 * Created by Mike on 5/4/2015.
 */
public class FriendsActivity extends ActionBarActivity {

    private static final String TAG = "FriendsActivity";

    private DataRepository mDataRepository;
    private ListView mUserListView;
    private List<String> mYourFriends;
    private List<User> mLatestResults;

    private ArrayAdapter<String> mAdapter;
    private int black;
    private Typeface font;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);
        mYourFriends = new ArrayList<String>();
        mLatestResults = new ArrayList<User>();

        mDataRepository = DataRepository.getInstance (DataRepository.Type.Global, getApplicationContext ());

        mUserListView = (ListView) findViewById(R.id.friends_list);

        font = Typeface.createFromAsset(getAssets(), "fonts/orangejuice.otf");

        // customize action bar
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        View view = LayoutInflater.from(this).inflate(R.layout.abs_layout_friends, null);
        ((TextView)view.findViewById(R.id.mytext)).setTypeface(font);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(params);
        getSupportActionBar().setCustomView(view);
        black = Color.parseColor("#ff000000");

        populateList ();
    }

    private void populateList() {
        final FriendsActivity ref = this;
        mDataRepository.getFollowedUsers(getSpinCounterApplication().getUser(), new DataRepository.Callback<List<User>>() {
            @Override
            public void success(List<User> result) {
                mYourFriends.clear();
                for (User u: result) {
                    mYourFriends.add(u.username);
                }
                UserAdapter userAdapter = new UserAdapter(getApplicationContext(), R.layout.friends_item_layout, result, ref);
                mUserListView.setAdapter(userAdapter);
            }

            @Override
            public void failure(boolean networkError) {
                if (!networkError) {
                    Log.e(TAG, "Some failure besides network error");
                }
            }
        });
    }

    public void deleteFriend(User friend) {
        mDataRepository.unfollowUser(getSpinCounterApplication().getUser(), friend, new DataRepository.Callback<Void>() {
            @Override
            public void success(Void result) {
                populateList();
            }

            @Override
            public void failure(boolean networkError) {
                // nothing
            }
        });
    }

    private SpinCounterApplication getSpinCounterApplication () {
        return (SpinCounterApplication)getApplication();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_friends, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.add_friend:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.title_add_friend);

                final ArrayList<String> adapterBack = new ArrayList<String>();
                mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, adapterBack);

                // Set up the input
                final AutoCompleteTextView input = new AutoCompleteTextView(this);
                input.setThreshold(1);
                input.setAdapter(mAdapter);
                builder.setView(input);
                input.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        // nothing
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        mDataRepository.searchUsers(s.toString(), new DataRepository.Callback<List<User>>() {
                            @Override
                            public void success(List<User> result) {
                                mAdapter.clear();
                                mLatestResults = result;
                                for (User u: result) {
                                    if (!mYourFriends.contains(u.username) &&
                                            !u.username.equals(getSpinCounterApplication().getUser().username)) {
                                        mAdapter.add(u.username);
                                        if (mAdapter.getCount() > 10) {
                                            break;
                                        }
                                    }
                                }
                                mAdapter.getFilter().filter(input.getText().toString(), input);
                            }

                            @Override
                            public void failure(boolean networkError) {

                            }
                        });
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        // nothing
                    }
                });

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        User toFollow = null;
                        String nameToFollow = input.getText().toString();
                        for (User u : mLatestResults) {
                            if (u.username.equals(nameToFollow)) {
                                toFollow = u;
                                break;
                            }
                        }
                        if (toFollow == null) {
                            Toast.makeText(getApplicationContext(), getText(R.string.toast_failure), Toast.LENGTH_LONG).show();
                        } else {
                            mDataRepository.followUser(getSpinCounterApplication().getUser(), toFollow, new DataRepository.Callback<Void>() {
                                @Override
                                public void success(Void result) {
                                    populateList();
                                }

                                @Override
                                public void failure(boolean networkError) {
                                    Toast.makeText(getApplicationContext(), getText(R.string.toast_failure), Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
                return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_top);
    }

}
