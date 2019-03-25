package co.tinode.tindroid;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.HashMap;

import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import co.tinode.tindroid.media.VxCard;
import co.tinode.tinodesdk.ComTopic;
import co.tinode.tinodesdk.NotConnectedException;
import co.tinode.tinodesdk.PromisedReply;
import co.tinode.tinodesdk.Topic;
import co.tinode.tinodesdk.model.ServerMessage;

import static android.app.Activity.RESULT_OK;

/**
 * Fragment for adding/editing a group topic
 */
public class CreateGroupFragment extends Fragment
        implements UiUtils.ContactsLoaderResultReceiver, ContactsAdapter.ClickListener {

    private static final String TAG = "CreateGroupFragment";

    private ImageLoader mImageLoader; // Handles loading the contact image in a background thread

    private PromisedReply.FailureListener<ServerMessage> mFailureListener;

    private ContactsAdapter mAdapter;
    private HashMap<String, Boolean> mGroupMembers = new HashMap<>();
    private ChipGroup mSelectedMembers;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mImageLoader = UiUtils.getImageLoaderInstance(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        return inflater.inflate(R.layout.fragment_add_group, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstance) {

        final FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        mFailureListener = new PromisedReply.FailureListener<ServerMessage>() {
            @Override
            public PromisedReply<ServerMessage> onFailure(final Exception err) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (err instanceof NotConnectedException) {
                            Toast.makeText(activity, R.string.no_connection, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(activity, R.string.action_failed, Toast.LENGTH_SHORT).show();
                        }
                        startActivity(new Intent(activity, ChatsActivity.class));
                    }
                });
                return null;
            }
        };

        view.findViewById(R.id.uploadAvatar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiUtils.requestAvatar(CreateGroupFragment.this);
            }
        });

        mSelectedMembers = view.findViewById(R.id.selectedMembers);

        // Recycler view with all available Tinode contacts.
        RecyclerView rv = view.findViewById(R.id.contact_list);
        rv.setLayoutManager(new LinearLayoutManager(activity));
        rv.setHasFixedSize(true);
        rv.addItemDecoration(new DividerItemDecoration(activity, DividerItemDecoration.VERTICAL));

        mAdapter = new ContactsAdapter(activity, mImageLoader, this);
        rv.setAdapter(mAdapter);

        // This button creates the new group.
        view.findViewById(R.id.goNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText titleEdit = activity.findViewById(R.id.editTitle);
                final String topicTitle = titleEdit.getText().toString();
                if (TextUtils.isEmpty(topicTitle)) {
                    titleEdit.setError(getString(R.string.name_required));
                    return;
                }
                final String subtitle = ((EditText) activity.findViewById(R.id.editPrivate)).getText().toString();

                final String tags = ((EditText) activity.findViewById(R.id.editTags)).getText().toString();

                if (mGroupMembers.size() == 0) {
                    Toast.makeText(activity, R.string.add_one_member, Toast.LENGTH_SHORT).show();
                    return;
                }

                Bitmap bmp = null;
                try {
                    bmp = ((BitmapDrawable) ((ImageView) activity.findViewById(R.id.imageAvatar))
                            .getDrawable()).getBitmap();
                } catch (ClassCastException ignored) {
                    // If image is not loaded, the drawable is a vector.
                    // Ignore it.
                }

                createTopic(activity, topicTitle, bmp, subtitle, UiUtils.parseTags(tags));
            }
        });

        LoaderManager.getInstance(activity).initLoader(0, null,
                new UiUtils.ContactsLoaderCallback(getActivity(), this));
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        // In the case onPause() is called during a fling the image loader is
        // un-paused to let any remaining background work complete.
        mImageLoader.setPauseWork(false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (requestCode == UiUtils.SELECT_PICTURE && resultCode == RESULT_OK) {
            UiUtils.acceptAvatar(getActivity(), (ImageView) activity.findViewById(R.id.imageAvatar), data);
        }
    }

    @Override
    public void receiveResult(int id, Cursor data) {
        mAdapter.resetContent(data, null);
    }

    private void createTopic(final Activity activity, final String title,
                             final Bitmap avatar, final String subtitle, final String[] tags) {
        final ComTopic<VxCard> topic = new ComTopic<>(Cache.getTinode(), (Topic.Listener) null);
        topic.setPub(new VxCard(title, avatar));
        topic.setPriv(subtitle);
        topic.setTags(tags);
        try {
            topic.subscribe().thenApply(new PromisedReply.SuccessListener<ServerMessage>() {
                @Override
                public PromisedReply<ServerMessage> onSuccess(ServerMessage result) throws Exception {
                    for (String user : mGroupMembers.keySet()) {
                        topic.invite(user, null /* use default */);
                    }

                    Intent intent = new Intent(activity, MessageActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    intent.putExtra("topic", topic.getName());
                    startActivity(intent);

                    return null;
                }
            }, mFailureListener);
        } catch (NotConnectedException ignored) {
            Toast.makeText(activity, R.string.no_connection, Toast.LENGTH_SHORT).show();
            // Go back to contacts
        } catch (Exception ex) {
            Toast.makeText(activity, R.string.failed_to_create_topic, Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Failed to create topic", ex);
        }

        startActivity(new Intent(activity, ChatsActivity.class));
    }

    @Override
    public void onClick(String topicName, ContactsAdapter.ViewHolder holder) {
        Activity activity = getActivity();
        if (activity != null) {
            Chip chip = UiUtils.makeChip(activity,
                    (String) holder.text1.getText(),
                    holder.icon.getDrawable(), topicName);
            chip.setOnCloseIconClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // noinspection SuspiciousMethodCalls
                    mGroupMembers.remove(view.getTag(0));
                    mSelectedMembers.removeView(view);
                }
            });
            mSelectedMembers.addView(chip);
        }
    }
}
