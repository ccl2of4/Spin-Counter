package groupn.spin_counter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import groupn.spin_counter.model.User;

/**
 * Created by Mike on 5/4/2015.
 */
public class UserAdapter extends ArrayAdapter<User> {

    private FriendsActivity callingReference;

    public UserAdapter(Context context, int resource, FriendsActivity activity) {
        super(context, resource);
        callingReference = activity;
    }

    public UserAdapter(Context context, int resource, List<User> items, FriendsActivity activity) {
        super(context, resource, items);
        callingReference = activity;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            v = inflater.inflate(R.layout.friends_item_layout, null);
        }

        final User u = getItem(position);

        if (u != null) {
            TextView name = (TextView) v.findViewById(R.id.friends_username);
            TextView max = (TextView) v.findViewById(R.id.friends_maxSpins);
            TextView wins = (TextView) v.findViewById(R.id.friends_wins);

            ImageButton delete = (ImageButton) v.findViewById(R.id.friends_delete);

            if (name != null) {
                name.setText(u.username);
            }

            if (max != null) {
                max.setText(getContext().getResources().getString(R.string.max_spins_label) + " "  + u.maxSpins);
            }

            if (wins != null) {
                wins.setText(getContext().getResources().getString(R.string.games_won_label) + " " + u.gamesWon);
            }

            if (delete != null) {
                delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        callingReference.deleteFriend(u);
                    }
                });
            }
        }
        return v;
    }

}
