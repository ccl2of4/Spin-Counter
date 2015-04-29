package groupn.spin_counter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
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

import java.util.Arrays;
import java.util.List;

import groupn.spin_counter.model.DataRepository;
import groupn.spin_counter.model.User;


public class LoginActivity extends ActionBarActivity {

    private DataRepository mDataRepository;
    private static String TAG = "LoginActivity";

    private EditText mEditText;
    private TextView mTextView;
    private Button mButton;

    private User mUser;

    private final static int DIALOG_NETWORK_ERROR = 1;
    private final static int DIALOG_USERNAME_TAKEN = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mEditText = (EditText)findViewById(R.id.username);
        mTextView = (TextView)findViewById(R.id.change_username_textview);
        mButton = (Button)findViewById(R.id.confirm_username);

        mDataRepository = DataRepository.getInstance(DataRepository.Type.Global, getApplicationContext());
        mUser = (User)getIntent().getSerializableExtra("User");

        // User is not null. this means we are here for a username change
        if (mUser != null) {
            setUpChangeUsername();
        }

        // User is null. we don't know if it's in the DB or not
        else {
            findUser();
        }
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
        mEditText.setHint(mUser.username);
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
            // user exists in DB
            @Override
            public void success(User user) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("User", user);
                startActivity(intent);
                progressDialog.dismiss();
            }

            @Override
            public void failure(boolean networkError) {
                if (networkError) {
                    // should probably handle this more gracefully
                    finish();
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(final int id) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).
                setCancelable(true).
                setPositiveButton(R.string.ok, null);

        switch (id) {
            case DIALOG_USERNAME_TAKEN: {
                builder.setMessage(R.string.username_taken);
                break;
            }
            case DIALOG_NETWORK_ERROR : {
                builder.setMessage(R.string.network_error);
                break;
            }
        }

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
            mProgressDialog.dismiss();
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("User", user);
            startActivity(intent);
        }

        @Override
        public void failure(boolean networkError) {
            mProgressDialog.dismiss();
            if (networkError) {
                showDialog(DIALOG_NETWORK_ERROR);
            } else {
                showDialog(DIALOG_USERNAME_TAKEN);
            }
        }
    }

    private class ChangeUsernameOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            final ProgressDialog progressDialog = ProgressDialog.show(LoginActivity.this, getString(R.string.verifying_credentials), null, true);
            mDataRepository.changeUsername(mEditText.getText().toString(), new UsernameResultHandler(progressDialog));
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
