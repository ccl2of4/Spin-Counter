package groupn.spin_counter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

import groupn.spin_counter.model.DataRepository;
import groupn.spin_counter.model.User;


public class LoginActivity extends ActionBarActivity {

    public static final String PURPOSE = "purpose";
    public static final int PURPOSE_FIND_USER = 5;
    public static final int PURPOSE_CHANGE_USERNAME = 6;

    private DataRepository mDataRepository;
    private static String TAG = "LoginActivity";

    private EditText mEditText;
    private TextView mTextView;
    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mEditText = (EditText)findViewById(R.id.username);
        mTextView = (TextView)findViewById(R.id.change_username_textview);
        mButton = (Button)findViewById(R.id.confirm_username);

        mDataRepository = DataRepository.getInstance(DataRepository.Type.Global, getApplicationContext());

        int purpose = getIntent().getIntExtra(PURPOSE, PURPOSE_FIND_USER);
        if (purpose == PURPOSE_FIND_USER) {
            findUser();
        } else if (purpose == PURPOSE_CHANGE_USERNAME) {
            setUpChangeUsername();
        }
    }

    public SpinCounterApplication getSpinCounterApplication() {
        return (SpinCounterApplication)getApplication();
    }

    private void hideUI() {
        mEditText.setVisibility(View.INVISIBLE);
        mTextView.setVisibility(View.INVISIBLE);
        mButton.setVisibility(View.INVISIBLE);
    }

    private void showUI () {
        mEditText.setVisibility(View.VISIBLE);
        mTextView.setVisibility(View.VISIBLE);
        mButton.setVisibility(View.VISIBLE);
    }

    private void setUpChangeUsername() {
        mTextView.setText(R.string.change_username);
        mEditText.setHint(getSpinCounterApplication().getUser().username);
        mButton.setOnClickListener(new ChangeUsernameOnClickListener());
    }

    private void setUpRegisterUsername() {
        mTextView.setText(R.string.register_username);
        mButton.setOnClickListener(new RegisterUsernameOnClickListener());
    }

    private void findUser() {
        hideUI();
        final ProgressDialog progressDialog = ProgressDialog.show(this, getString(R.string.verifying_credentials), null, true);
        mDataRepository.getUserInfo(new DataRepository.Callback<User> () {

            // user exists in DB. the activity's purpose has been fulfilled, so we can finish
            @Override
            public void success(User user) {
                getSpinCounterApplication().setUser(user);
                progressDialog.dismiss();
                finish();
            }

            @Override
            public void failure(boolean networkError) {
                if (networkError) {
                    showDialog(0);
                    progressDialog.dismiss();
                }

                // user doesn't exist in DB. we need to create an entry by asking the user for a
                // username
                else {
                    showUI();
                    setUpRegisterUsername();
                    progressDialog.dismiss();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(final int id) {

        // just keep trying to connect until we're successful.
        // can't use the app without an account
        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).
                setCancelable(true).
                setMessage(R.string.network_error).
                setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        findUser();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

        return super.onCreateDialog(id);
    }

    // behavior for both registering and changing usernames is identical
    private class UsernameResultHandler implements DataRepository.Callback<User> {
        private ProgressDialog mProgressDialog;
        public UsernameResultHandler(ProgressDialog progressDialog) {
            mProgressDialog = progressDialog;
        }
        @Override
        public void success(User user) {
            getSpinCounterApplication().setUser(user);

            mProgressDialog.dismiss();
            Intent intent = new Intent(LoginActivity.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }

        @Override
        public void failure(boolean networkError) {
            mProgressDialog.dismiss();
            if (networkError) {
                Toast.makeText(LoginActivity.this, R.string.network_error, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(LoginActivity.this, R.string.username_taken, Toast.LENGTH_LONG).show();
            }
        }
    }

    private class ChangeUsernameOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if(mEditText.getText().length() != 0) {
                final ProgressDialog progressDialog = ProgressDialog.show(LoginActivity.this, getString(R.string.verifying_credentials), null, true);
                mDataRepository.changeUsername(mEditText.getText().toString(), new UsernameResultHandler(progressDialog));
            }
        }
    }

    private class RegisterUsernameOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            final ProgressDialog progressDialog = ProgressDialog.show(LoginActivity.this, getString(R.string.verifying_credentials), null, true);
            mDataRepository.registerUsername(mEditText.getText().toString(), new UsernameResultHandler(progressDialog));
        }
    }
}
