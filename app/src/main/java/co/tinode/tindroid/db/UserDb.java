package co.tinode.tindroid.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.Date;

import co.tinode.tinodesdk.model.Subscription;
import co.tinode.tinodesdk.User;

import static co.tinode.tindroid.R.string.tinode;
import static co.tinode.tindroid.db.TopicDb.COLUMN_IDX_TOPIC;

/**
 * Local cash of known users
 */

public class UserDb implements BaseColumns {
    private static final String TAG = "UserDb";

    /**
     * The name of the main table.
     */
    public static final String TABLE_NAME = "users";
    /**
     * The name of index: topic by account id and topic name.
     */
    public static final String INDEX_NAME = "user_account_name";
    /**
     * Account ID, references accounts._ID
     */
    public static final String COLUMN_NAME_ACCOUNT_ID = "account_id";
    /**
     * Topic name, indexed
     */
    public static final String COLUMN_NAME_UID = "uid";
    /**
     * When the user was updated
     */
    public static final String COLUMN_NAME_UPDATED = "updated";
    /**
     * When the user was deleted
     */
    public static final String COLUMN_NAME_DELETED = "deleted";
    /**
     * Public user description, (what's shown in 'me' topic) blob
     */
    public static final String COLUMN_NAME_PUBLIC = "pub";


    static final int COLUMN_IDX_ID = 0;
    static final int COLUMN_IDX_ACCOUNT_ID = 1;
    static final int COLUMN_IDX_UID = 2;
    static final int COLUMN_IDX_UPDATED = 3;
    static final int COLUMN_IDX_DELETED = 4;
    static final int COLUMN_IDX_PUBLIC = 5;

    /**
     * SQL statement to create Messages table
     */
    static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY," +
                    COLUMN_NAME_ACCOUNT_ID
                    + " REFERENCES " + AccountDb.TABLE_NAME + "(" + AccountDb._ID + ")," +
                    COLUMN_NAME_UID + " TEXT," +
                    COLUMN_NAME_UPDATED + " INT," +
                    COLUMN_NAME_DELETED + " INT," +
                    COLUMN_NAME_PUBLIC + " BLOB)";
    /**
     * Add index on account_id-topic name, in descending order
     */
    static final String CREATE_INDEX =
            "CREATE UNIQUE INDEX " + INDEX_NAME +
                    " ON " + TABLE_NAME + " (" +
                    COLUMN_NAME_ACCOUNT_ID + "," + COLUMN_NAME_UID + ")";

    /**
     * SQL statement to drop the table.
     */
    static final String DROP_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;
    /**
     * Drop the index too
     */
    static final String DROP_INDEX =
            "DROP INDEX IF EXISTS " + INDEX_NAME;

    /**
     * Save user to DB
     *
     * @return ID of the newly added user
     */
    public static long insert(SQLiteDatabase db, Subscription sub) {
        // Log.d(TAG, "Inserting user " + sub.user);

        // Convert subscription description to a map of values
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_ACCOUNT_ID, BaseDb.getInstance().getAccountId());
        values.put(COLUMN_NAME_UID, sub.user);
        values.put(COLUMN_NAME_UPDATED, (sub.updated != null ? sub.updated : new Date()).getTime());
        // values.put(COLUMN_NAME_DELETED, NULL);
        if (sub.pub != null) {
            values.put(COLUMN_NAME_PUBLIC, BaseDb.serialize(sub.pub));
        }
        return db.insert(TABLE_NAME, null, values);
    }

    /**
     * Save user to DB as user generated from invite
     *
     * @return ID of the newly added user
     */
    public static long insert(SQLiteDatabase db, String uid, Object pub) {
        // Log.d(TAG, "Inserting user " + uid + " from invite");

        // Convert subscription description to a map of values
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_ACCOUNT_ID, BaseDb.getInstance().getAccountId());
        values.put(COLUMN_NAME_UID, uid);
        values.put(COLUMN_NAME_UPDATED, new Date().getTime());
        // values.put(COLUMN_NAME_DELETED, NULL);
        if (pub != null) {
            values.put(COLUMN_NAME_PUBLIC, BaseDb.serialize(pub));
        }
        return db.insert(TABLE_NAME, null, values);
    }


    /**
     * Update user record
     *
     * @return true if the record was updated, false otherwise
     */
    public static boolean update(SQLiteDatabase db, Subscription sub) {

        StoredSubscription ss = (StoredSubscription) sub.getLocal();
        if (ss == null || ss.userId <= 0) {
            return false;
        }

        // Convert topic description to a map of values
        ContentValues values = new ContentValues();
        if (sub.updated != null) {
            values.put(COLUMN_NAME_UPDATED, sub.updated.getTime());
        }
        // values.put(COLUMN_NAME_DELETED, NULL);
        if (sub.pub != null) {
            values.put(COLUMN_NAME_PUBLIC, BaseDb.serialize(sub.pub));
        }

        return values.size() <= 0 || db.update(TABLE_NAME, values, _ID + "=" + ss.userId, null) > 0;

        // Log.d(TAG, "Update row, accid=" + BaseDb.getInstance().getAccountId() + " name=" + sub.user + " returned " + updated);
    }

    /**
     * Given UID, get it's database _id
     *
     * @param db database
     * @param uid UID
     * @return _id of the user
     */
    static long getId(SQLiteDatabase db, String uid) {
        long id = -1;
        String sql =
                "SELECT " + _ID +
                        " FROM " + TABLE_NAME +
                        " WHERE " +
                        COLUMN_NAME_ACCOUNT_ID + "=" + BaseDb.getInstance().getAccountId() +
                        " AND " +
                        COLUMN_NAME_UID + "='" + uid + "'";
        Log.d(TAG, sql);
        Cursor c = db.rawQuery(sql, null);
        if (c != null && c.getCount() > 0) {
            if (c.moveToFirst()) {
                id = c.getLong(0);
            }
            c.close();
        }
        return id;
    }

    static <Pu> User<Pu> readOne(SQLiteDatabase db, String uid) {
        // Instantiate topic of an appropriate class ('me' or group)
        User<Pu> user = null;
        String sql =
                "SELECT * FROM " + TABLE_NAME +
                        " WHERE " +
                        COLUMN_NAME_ACCOUNT_ID + "=" + BaseDb.getInstance().getAccountId() +
                        " AND " +
                        COLUMN_NAME_UID + "='" + uid + "'";
        Cursor c = db.rawQuery(sql, null);
        if (c != null && c.getCount() > 0) {
            user = new User<>(uid);
            if (c.moveToFirst()) {
                StoredUser.deserialize(user, c);
            }
            c.close();
        }
        return user;
    }
}
