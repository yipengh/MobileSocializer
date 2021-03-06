package fr.utt.isi.tx.trustevaluationandroidapp.database;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.brickred.socialauth.Contact;

import com.facebook.model.GraphUser;

import fr.utt.isi.tx.trustevaluationandroidapp.activities.ListContactSplittedActivity;
import fr.utt.isi.tx.trustevaluationandroidapp.config.Config;
import fr.utt.isi.tx.trustevaluationandroidapp.models.LocalContact;
import fr.utt.isi.tx.trustevaluationandroidapp.models.MergedContactNode;
import fr.utt.isi.tx.trustevaluationandroidapp.models.PseudoFacebookGraphUser;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

public class TrustEvaluationDbHelper extends SQLiteOpenHelper {

	private static final String TAG = "TrustEvaluationDbHelper";

	// increase database version when database schema changes
	public static final int DATABASE_VERSION = 66;

	public static final String DATABASE_NAME = "TrustEvaluation.db";

	public static final String[] TABLE_NAMES = {
			TrustEvaluationDataContract.ContactNode.TABLE_NAME,
			TrustEvaluationDataContract.LocalPhoneContact.TABLE_NAME,
			TrustEvaluationDataContract.LocalEmailContact.TABLE_NAME,
			TrustEvaluationDataContract.FacebookContact.TABLE_NAME,
			TrustEvaluationDataContract.TwitterContact.TABLE_NAME,
			TrustEvaluationDataContract.LinkedinContact.TABLE_NAME };

	private SQLiteDatabase readable = null;
	private SQLiteDatabase writable = null;

	public TrustEvaluationDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TrustEvaluationDataContract.ContactNode.SQL_CREATE_ENTRIES);
		db.execSQL(TrustEvaluationDataContract.LocalPhoneContact.SQL_CREATE_ENTRIES);
		db.execSQL(TrustEvaluationDataContract.LocalEmailContact.SQL_CREATE_ENTRIES);
		db.execSQL(TrustEvaluationDataContract.FacebookContact.SQL_CREATE_ENTRIES);
		db.execSQL(TrustEvaluationDataContract.TwitterContact.SQL_CREATE_ENTRIES);
		db.execSQL(TrustEvaluationDataContract.LinkedinContact.SQL_CREATE_ENTRIES);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		dropDatabase(db);
		onCreate(db);
	}

	public void dropDatabase(SQLiteDatabase db) {
		db.execSQL(TrustEvaluationDataContract.ContactNode.SQL_DELETE_ENTRIES);
		db.execSQL(TrustEvaluationDataContract.ContactNode.SQL_DELETE_VIRTUAL_FTS_ENTRIES);
		db.execSQL(TrustEvaluationDataContract.LocalPhoneContact.SQL_DELETE_ENTRIES);
		db.execSQL(TrustEvaluationDataContract.LocalEmailContact.SQL_DELETE_ENTRIES);
		db.execSQL(TrustEvaluationDataContract.FacebookContact.SQL_DELETE_ENTRIES);
		db.execSQL(TrustEvaluationDataContract.TwitterContact.SQL_DELETE_ENTRIES);
		db.execSQL(TrustEvaluationDataContract.LinkedinContact.SQL_DELETE_ENTRIES);
	}

	public void clearTable(String tableName) {
		writable = this.getWritableDatabase();

		String query = "DELETE FROM " + tableName;
		Log.v(TAG, "deleting table " + tableName);
		SQLiteStatement statement = writable.compileStatement(query);
		writable.beginTransaction();
		statement.execute();
		writable.setTransactionSuccessful();
		writable.endTransaction();
		writable.close();
	}

	public void clearDatabase() {
		for (int i = 0; i < TABLE_NAMES.length; i++) {
			clearTable(TABLE_NAMES[i]);
		}
	}

	public void insertLocalContact(int contactType, List<LocalContact> contacts) {
		writable = this.getWritableDatabase();

		String tableName;
		if (contactType == ListContactSplittedActivity.LOCAL_PHONE) {
			tableName = TrustEvaluationDataContract.LocalPhoneContact.TABLE_NAME;
		} else if (contactType == ListContactSplittedActivity.LOCAL_EMAIL) {
			tableName = TrustEvaluationDataContract.LocalEmailContact.TABLE_NAME;
		} else {
			return;
		}

		String query = "INSERT INTO " + tableName + " VALUES (?,?,?,?,?,?,?)";
		SQLiteStatement statement = writable.compileStatement(query);
		writable.beginTransaction();

		Iterator<LocalContact> i = contacts.iterator();
		while (i.hasNext()) {
			LocalContact contact = i.next();
			statement.clearBindings();
			statement.bindString(2, contact.getContactId());
			statement.bindString(3, contact.getDisplayName());
			statement.bindString(4, contact.getContactDetail());
			statement.bindString(5, contact.getContactUri().toString());
			statement.execute();
		}
		writable.setTransactionSuccessful();
		writable.endTransaction();
		writable.close();
	}

	public List<LocalContact> getLocalContacts(int contactType, String sortOrder) {
		readable = this.getReadableDatabase();

		String tableName;
		String[] columnNames = new String[4];
		if (contactType == ListContactSplittedActivity.LOCAL_PHONE) {
			tableName = TrustEvaluationDataContract.LocalPhoneContact.TABLE_NAME;
			columnNames[0] = TrustEvaluationDataContract.LocalPhoneContact.COLUMN_NAME_LOCAL_ID;
			columnNames[1] = TrustEvaluationDataContract.LocalPhoneContact.COLUMN_NAME_LOCAL_NAME;
			columnNames[2] = TrustEvaluationDataContract.LocalPhoneContact.COLUMN_NAME_LOCAL_NUMBER;
			columnNames[3] = TrustEvaluationDataContract.LocalPhoneContact.COLUMN_NAME_LOCAL_URI;
		} else if (contactType == ListContactSplittedActivity.LOCAL_EMAIL) {
			tableName = TrustEvaluationDataContract.LocalEmailContact.TABLE_NAME;
			columnNames[0] = TrustEvaluationDataContract.LocalEmailContact.COLUMN_NAME_LOCAL_ID;
			columnNames[1] = TrustEvaluationDataContract.LocalEmailContact.COLUMN_NAME_LOCAL_NAME;
			columnNames[2] = TrustEvaluationDataContract.LocalEmailContact.COLUMN_NAME_LOCAL_EMAIL;
			columnNames[3] = TrustEvaluationDataContract.LocalEmailContact.COLUMN_NAME_LOCAL_URI;
		} else {
			return null;
		}

		List<LocalContact> contacts = new ArrayList<LocalContact>();
		Cursor c = readable.query(tableName, null, null, null, null, null,
				sortOrder);
		if (c.getCount() == 0) {
			contacts = null;
		} else {
			while (c.moveToNext()) {
				contacts.add(new LocalContact(c.getString(c
						.getColumnIndex(columnNames[0])), c.getString(c
						.getColumnIndex(columnNames[1])), c.getString(c
						.getColumnIndex(columnNames[2])), Uri.parse(c
						.getString(c.getColumnIndex(columnNames[3])))));
			}
		}
		c.close();

		readable.close();

		return contacts;
	}

	public void insertTwitterContact(List<Contact> contacts) {
		writable = this.getWritableDatabase();

		String query = "INSERT INTO "
				+ TrustEvaluationDataContract.TwitterContact.TABLE_NAME
				+ " VALUES (?,?,?,?,?,?,?,?)";
		SQLiteStatement statement = writable.compileStatement(query);
		writable.beginTransaction();

		Iterator<Contact> i = contacts.iterator();
		while (i.hasNext()) {
			Contact contact = i.next();
			statement.clearBindings();
			statement.bindString(2, contact.getId());
			// real display name
			statement.bindString(3, contact.getFirstName());
			// twitter user name
			statement.bindString(4, contact.getDisplayName());
			// profile image url
			statement.bindString(5, contact.getProfileImageURL());
			statement.execute();
		}

		writable.setTransactionSuccessful();
		writable.endTransaction();
		writable.close();
	}

	public List<Contact> getTwitterContacts(String sortOrder) {
		readable = this.getReadableDatabase();

		List<Contact> contacts = new ArrayList<Contact>();
		Cursor c = readable.query(
				TrustEvaluationDataContract.TwitterContact.TABLE_NAME, null,
				null, null, null, null, sortOrder);
		if (c.getCount() == 0) {
			contacts = null;
		} else {
			while (c.moveToNext()) {
				Contact contact = new Contact();
				contact.setId(c.getString(c
						.getColumnIndex(TrustEvaluationDataContract.TwitterContact.COLUMN_NAME_TWITTER_ID)));
				contact.setFirstName(c.getString(c
						.getColumnIndex(TrustEvaluationDataContract.TwitterContact.COLUMN_NAME_TWITTER_NAME)));
				contact.setDisplayName(c.getString(c
						.getColumnIndex(TrustEvaluationDataContract.TwitterContact.COLUMN_NAME_TWITTER_USERNAME)));
				contact.setProfileImageURL(c.getString(c
						.getColumnIndex(TrustEvaluationDataContract.TwitterContact.COLUMN_NAME_TWITTER_PROFILE_IMAGE_URL)));
				contacts.add(contact);
			}
		}
		c.close();

		readable.close();

		return contacts;
	}

	public void insertLinkedinContact(List<Contact> contacts) {
		writable = this.getWritableDatabase();

		String query = "INSERT INTO "
				+ TrustEvaluationDataContract.LinkedinContact.TABLE_NAME
				+ " VALUES (?,?,?,?,?,?,?,?)";
		SQLiteStatement statement = writable.compileStatement(query);
		writable.beginTransaction();

		Iterator<Contact> i = contacts.iterator();
		while (i.hasNext()) {
			Contact contact = i.next();
			statement.clearBindings();
			statement.bindString(2, contact.getId());
			statement.bindString(3, contact.getFirstName());
			statement.bindString(4, contact.getLastName());
			// profile image url
			String url = contact.getProfileImageURL();
			if (url == null) {
				statement.bindNull(5);
			} else {
				statement.bindString(5, contact.getProfileImageURL());
			}
			statement.execute();
		}

		writable.setTransactionSuccessful();
		writable.endTransaction();
		writable.close();
	}

	public List<Contact> getLinkedinContacts(String sortOrder) {
		readable = this.getReadableDatabase();

		List<Contact> contacts = new ArrayList<Contact>();
		Cursor c = readable.query(
				TrustEvaluationDataContract.LinkedinContact.TABLE_NAME, null,
				null, null, null, null, sortOrder);
		if (c.getCount() == 0) {
			contacts = null;
		} else {
			while (c.moveToNext()) {
				Contact contact = new Contact();
				contact.setId(c.getString(c
						.getColumnIndex(TrustEvaluationDataContract.LinkedinContact.COLUMN_NAME_LINKEDIN_ID)));
				contact.setFirstName(c.getString(c
						.getColumnIndex(TrustEvaluationDataContract.LinkedinContact.COLUMN_NAME_LINKEDIN_FIRST_NAME)));
				contact.setLastName(c.getString(c
						.getColumnIndex(TrustEvaluationDataContract.LinkedinContact.COLUMN_NAME_LINKEDIN_LAST_NAME)));
				contact.setProfileImageURL(c.getString(c
						.getColumnIndex(TrustEvaluationDataContract.LinkedinContact.COLUMN_NAME_LINKEDIN_PROFILE_IMAGE_URL)));
				contacts.add(contact);
			}
		}
		c.close();

		readable.close();

		return contacts;
	}

	public void insertFacebookContacts(List<GraphUser> contacts) {
		writable = this.getWritableDatabase();

		String query = "INSERT INTO "
				+ TrustEvaluationDataContract.FacebookContact.TABLE_NAME
				+ " VALUES (?,?,?,?,?,?,?,?)";
		SQLiteStatement statement = writable.compileStatement(query);
		writable.beginTransaction();

		Iterator<GraphUser> i = contacts.iterator();
		while (i.hasNext()) {
			GraphUser contact = i.next();
			// get facebook id
			String facebookId = contact.getId();

			// form facebook profile picture url by facebook id
			String facebookProfilePictureUrl = "http://graph.facebook.com/"
					+ facebookId + "/picture";

			// do statement
			statement.clearBindings();
			statement.bindString(2, facebookId);
			statement.bindString(3, contact.getName());
			statement.bindString(4, facebookProfilePictureUrl);
			statement.execute();
		}

		writable.setTransactionSuccessful();
		writable.endTransaction();
		writable.close();
	}

	public List<PseudoFacebookGraphUser> getFacebookContacts(String sortOrder) {
		readable = this.getReadableDatabase();

		List<PseudoFacebookGraphUser> contacts = new ArrayList<PseudoFacebookGraphUser>();
		Cursor c = readable.query(
				TrustEvaluationDataContract.FacebookContact.TABLE_NAME, null,
				null, null, null, null, sortOrder);
		if (c.getCount() == 0) {
			contacts = null;
		} else {
			while (c.moveToNext()) {
				// get facebook id from cursor
				String facebookId = c
						.getString(c
								.getColumnIndex(TrustEvaluationDataContract.FacebookContact.COLUMN_NAME_FACEBOOK_ID));

				// get facebook name from cursor
				String facebookName = c
						.getString(c
								.getColumnIndex(TrustEvaluationDataContract.FacebookContact.COLUMN_NAME_FACEBOOK_NAME));

				// form the profile image url by facebook id
				String facebookProfileImageUrl = "http://graph.facebook.com/"
						+ facebookId + "/picture";

				PseudoFacebookGraphUser contact = new PseudoFacebookGraphUser(
						facebookId, facebookName, facebookProfileImageUrl);
				contacts.add(contact);
			}
		}
		c.close();

		readable.close();

		return contacts;
	}

	public void updateCommonFriendList(Map<String, String> commonFriendMap,
			int contactType) {
		writable = this.getWritableDatabase();

		// set the parameters of sql query depending on SNS type
		String tableName;
		String commonFriendListColumnName;
		String contactIdColumnName;
		switch (contactType) {
		case ListContactSplittedActivity.FACEBOOK:
			tableName = TrustEvaluationDataContract.FacebookContact.TABLE_NAME;
			commonFriendListColumnName = TrustEvaluationDataContract.FacebookContact.COLUMN_NAME_FACEBOOK_COMMON_FRIEND_LIST;
			contactIdColumnName = TrustEvaluationDataContract.FacebookContact.COLUMN_NAME_FACEBOOK_ID;
			break;
		case ListContactSplittedActivity.TWITTER:
			tableName = TrustEvaluationDataContract.TwitterContact.TABLE_NAME;
			commonFriendListColumnName = TrustEvaluationDataContract.TwitterContact.COLUMN_NAME_TWITTER_COMMON_FRIEND_LIST;
			contactIdColumnName = TrustEvaluationDataContract.TwitterContact.COLUMN_NAME_TWITTER_ID;
			break;
		case ListContactSplittedActivity.LINKEDIN:
			tableName = TrustEvaluationDataContract.LinkedinContact.TABLE_NAME;
			commonFriendListColumnName = TrustEvaluationDataContract.LinkedinContact.COLUMN_NAME_LINKEDIN_COMMON_FRIEND_LIST;
			contactIdColumnName = TrustEvaluationDataContract.LinkedinContact.COLUMN_NAME_LINKEDIN_ID;
			break;
		default:
			return;
		}

		// the query
		String query = "UPDATE " + tableName + " SET "
				+ commonFriendListColumnName + "=? WHERE "
				+ contactIdColumnName + "=?";

		// do statement by transaction
		SQLiteStatement statement = writable.compileStatement(query);
		writable.beginTransaction();

		for (Entry<String, String> entry : commonFriendMap.entrySet()) {
			String contactId = entry.getKey();
			String commonFriendList = entry.getValue();
			Log.v(TAG, "key: " + contactId + ", value: " + commonFriendList);

			statement.clearBindings();
			statement.bindString(1, commonFriendList);
			statement.bindString(2, contactId);
			statement.execute();
		}

		writable.setTransactionSuccessful();
		writable.endTransaction();
		writable.close();
	}

	public List<MergedContactNode> getMergedContacts(String sortOrder) {
		return getMergedContacts(
				TrustEvaluationDataContract.ContactNode.TABLE_NAME, null, null,
				sortOrder);
	}

	public List<MergedContactNode> getMergedContacts(String tableName,
			String selection, String[] selectionArgs, String sortOrder) {
		String displayName;
		int id;
		int sourceScore;
		int trustScore;
		int isLocalPhone;
		int isLocalEmail;
		int isFacebook;
		int isTwitter;
		int isLinkedin;

		readable = this.getReadableDatabase();

		List<MergedContactNode> contacts = new ArrayList<MergedContactNode>();

		Cursor c = readable.query(tableName, null, selection, selectionArgs,
				null, null, sortOrder);
		if (c.getCount() == 0) {
			contacts = null;
		} else {
			while (c.moveToNext()) {
				id = c.getInt(c
						.getColumnIndex(TrustEvaluationDataContract.ContactNode._ID));
				displayName = c
						.getString(c
								.getColumnIndex(TrustEvaluationDataContract.ContactNode.COLUMN_NAME_DISPLAY_NAME_GLOBAL));
				sourceScore = c
						.getInt(c
								.getColumnIndex(TrustEvaluationDataContract.ContactNode.COLUMN_NAME_SOURCE_SCORE));
				trustScore = c
						.getInt(c
								.getColumnIndex(TrustEvaluationDataContract.ContactNode.COLUMN_NAME_TRUST_SCORE));
				isLocalPhone = c
						.getInt(c
								.getColumnIndex(TrustEvaluationDataContract.ContactNode.COLUMN_NAME_IS_LOCAL_PHONE));
				isLocalEmail = c
						.getInt(c
								.getColumnIndex(TrustEvaluationDataContract.ContactNode.COLUMN_NAME_IS_LOCAL_EMAIL));
				isFacebook = c
						.getInt(c
								.getColumnIndex(TrustEvaluationDataContract.ContactNode.COLUMN_NAME_IS_FACEBOOK));
				isTwitter = c
						.getInt(c
								.getColumnIndex(TrustEvaluationDataContract.ContactNode.COLUMN_NAME_IS_TWITTER));
				isLinkedin = c
						.getInt(c
								.getColumnIndex(TrustEvaluationDataContract.ContactNode.COLUMN_NAME_IS_LINKEDIN));

				MergedContactNode contact = new MergedContactNode();
				contact.setId(String.valueOf(id));
				contact.setDisplayNameGlobal(displayName);
				contact.setSourceScore(sourceScore);
				contact.setTrustScore(trustScore);
				contact.setIsLocalPhone(isLocalPhone);
				contact.setIsLocalEmail(isLocalEmail);
				contact.setIsFacebook(isFacebook);
				contact.setIsTwitter(isTwitter);
				contact.setIsLinkedin(isLinkedin);
				contact.setFacebookId(c.getString(c
						.getColumnIndex(TrustEvaluationDataContract.ContactNode.COLUMN_NAME_FACEBOOK_ID)));
				contacts.add(contact);
			}
		}
		c.close();

		readable.close();

		return contacts;

	}

	public void calculateTrustIndex() { // for now, facebook only
		readable = this.getReadableDatabase();

		// parameters for select query of common friend list strings
		String tableName = TrustEvaluationDataContract.FacebookContact.TABLE_NAME;
		String[] columns = {
				TrustEvaluationDataContract.FacebookContact.COLUMN_NAME_FACEBOOK_ID,
				TrustEvaluationDataContract.FacebookContact.COLUMN_NAME_FACEBOOK_COMMON_FRIEND_LIST,
				TrustEvaluationDataContract.FacebookContact.COLUMN_NAME_IS_INDEXED };
		String selection = TrustEvaluationDataContract.FacebookContact.COLUMN_NAME_FACEBOOK_COMMON_FRIEND_LIST
				+ " <> ?";// + " AND " +
							// TrustEvaluationDataContract.FacebookContact.COLUMN_NAME_IS_INDEXED
							// + " = ?";
		String[] selectionArgs = new String[] { "" };
		Cursor c = readable.query(tableName, columns, selection, selectionArgs,
				null, null, null, null);

		if (c.getCount() == 0) {
			Log.d(TAG, "no common friends for calculation of trust index");
			return;
		}

		writable = this.getWritableDatabase();

		// statement for update query of trust index score
		String query = "UPDATE "
				+ TrustEvaluationDataContract.ContactNode.TABLE_NAME
				+ " SET "
				+ TrustEvaluationDataContract.ContactNode.COLUMN_NAME_TRUST_SCORE
				+ "=? WHERE "
				+ TrustEvaluationDataContract.ContactNode.COLUMN_NAME_FACEBOOK_ID
				+ "=?";

		// do statement by transaction
		SQLiteStatement statement = writable.compileStatement(query);
		writable.beginTransaction();

		// parameters for select query of summary of source score of each
		// facebook friend
		// who has common friends with me
		tableName = TrustEvaluationDataContract.ContactNode.TABLE_NAME;
		columns = new String[] { "SUM("
				+ TrustEvaluationDataContract.ContactNode.COLUMN_NAME_SOURCE_SCORE
				+ ")" };

		while (c.moveToNext()) {
			if (c.getInt(c
					.getColumnIndex(TrustEvaluationDataContract.FacebookContact.COLUMN_NAME_IS_INDEXED)) == 1) {
				Log.v(TAG, "object indexed, pass to next.");
				continue;
			}
			// clear clause
			selection = "";
			selectionArgs = new String[] {};

			// get necessary fields for summary query
			String facebookId = c
					.getString(c
							.getColumnIndex(TrustEvaluationDataContract.FacebookContact.COLUMN_NAME_FACEBOOK_ID));
			String[] facebookCommonFriendList = c
					.getString(
							c.getColumnIndex(TrustEvaluationDataContract.FacebookContact.COLUMN_NAME_FACEBOOK_COMMON_FRIEND_LIST))
					.split(";");

			// selection clause and its arguments
			StringBuilder selectionStringBuilder = new StringBuilder();
			ArrayList<String> selectionArgsArrayList = new ArrayList<String>();

			for (int i = 0; i < facebookCommonFriendList.length; i++) {
				// Log.v(TAG, facebookId + ", " + facebookCommonFriendList[i]);

				// selection clause
				selectionStringBuilder
						.append(TrustEvaluationDataContract.ContactNode.COLUMN_NAME_FACEBOOK_ID
								+ " = ?");
				if (i != facebookCommonFriendList.length - 1) {
					selectionStringBuilder.append(" OR ");
				}

				// selection arguments
				selectionArgsArrayList.add(facebookCommonFriendList[i]);
			}

			// convert the selection clause and its arguments
			selection = selectionStringBuilder.toString();
			Log.v(TAG, "selection string: " + selection);
			selectionArgs = selectionArgsArrayList.toArray(selectionArgs);
			for (int i = 0; i < selectionArgs.length; i++) {
				Log.v(TAG, "selection args: " + selectionArgs[i]);
			}

			// do the query and obtain the trust index score
			Cursor cIndex = readable.query(tableName, columns, selection,
					selectionArgs, null, null, null);
			int trustIndexScore = 0;
			if (cIndex.moveToNext()) {
				Log.v(TAG, "id:" + facebookId + ", index: " + cIndex.getInt(0));
				trustIndexScore = cIndex.getInt(0);
			}
			cIndex.close();

			// update the index score to corresponding facebook account in
			// contact node
			statement.clearBindings();
			statement.bindLong(1, trustIndexScore);
			statement.bindString(2, facebookId);
			statement.execute();
		}

		writable.setTransactionSuccessful();
		writable.endTransaction();
		writable.close();

		c.close();

		readable.close();
	}

	public int getContactNodeIdByFacebookId(String facebookId) {
		readable = this.getReadableDatabase();

		String tableName = TrustEvaluationDataContract.ContactNode.TABLE_NAME;
		String[] columns = new String[] { TrustEvaluationDataContract.ContactNode._ID };
		String selection = TrustEvaluationDataContract.ContactNode.COLUMN_NAME_FACEBOOK_ID
				+ " = ?";
		String[] selectionArgs = new String[] { facebookId };

		Cursor c = readable.query(tableName, columns, selection, selectionArgs,
				null, null, null);

		int contactNodeId = 0;

		if (c.moveToNext()) {
			contactNodeId = c.getInt(0);
		}

		// readable.close();

		return contactNodeId;
	}

	public SparseArray<String> getFacebookCommonFriendsContactNodeIds() {
		SparseArray<String> map = new SparseArray<String>();

		readable = this.getReadableDatabase();

		// get all facebook contacts and their common friend list
		String tableName = TrustEvaluationDataContract.FacebookContact.TABLE_NAME;
		String[] columns = new String[] {
				TrustEvaluationDataContract.FacebookContact.COLUMN_NAME_FACEBOOK_ID,
				TrustEvaluationDataContract.FacebookContact.COLUMN_NAME_FACEBOOK_COMMON_FRIEND_LIST };

		Cursor c = readable.query(tableName, columns, null, null, null, null,
				null);

		while (c.moveToNext()) {
			String facebookId = c
					.getString(c
							.getColumnIndex(TrustEvaluationDataContract.FacebookContact.COLUMN_NAME_FACEBOOK_ID));
			String[] facebookCommonFriendList = c
					.getString(
							c.getColumnIndex(TrustEvaluationDataContract.FacebookContact.COLUMN_NAME_FACEBOOK_COMMON_FRIEND_LIST))
					.split(";");

			// build query
			tableName = TrustEvaluationDataContract.ContactNode.TABLE_NAME;
			columns = new String[] { TrustEvaluationDataContract.ContactNode._ID };
			// build selection
			StringBuilder selectionBuilder = new StringBuilder();
			for (int i = 0; i < facebookCommonFriendList.length; i++) {
				selectionBuilder
						.append(TrustEvaluationDataContract.ContactNode.COLUMN_NAME_FACEBOOK_ID
								+ " = ?");
				if (i != facebookCommonFriendList.length - 1) {
					selectionBuilder.append(" OR ");
				}
			}
			String selection = selectionBuilder.toString();
			String[] selectionArgs = facebookCommonFriendList;

			Cursor cContactNodes = readable.query(tableName, columns,
					selection, selectionArgs, null, null, null);

			StringBuilder idBuilder = new StringBuilder();
			while (cContactNodes.moveToNext()) {
				idBuilder.append(cContactNodes.getInt(0));
				idBuilder.append(";");
			}

			cContactNodes.close();

			int id = getContactNodeIdByFacebookId(facebookId);

			map.put(id, idBuilder.toString());
		}

		readable.close();

		return map;
	}

	public void exportNodeContactTable(Context context) throws IOException {
		SparseArray<String> facebookRelationMap = getFacebookCommonFriendsContactNodeIds();

		StringBuilder mStringBuilder = new StringBuilder();

		// write the header
		mStringBuilder
				.append("id,source_score,trust_score,is_local_phone,is_local_email,is_facebook,is_twitter,is_linkedin,\n");

		// get contact node list from db
		List<MergedContactNode> contactNodes = getMergedContacts(TrustEvaluationDataContract.ContactNode.COLUMN_NAME_TRUST_SCORE
				+ " DESC");

		for (int i = 0; i < contactNodes.size(); i++) {
			MergedContactNode contactNode = contactNodes.get(i);
			if (contactNode.getIsFacebook() == 0) {
				// if contact not detected on facebook, don't export
				continue;
			}
			mStringBuilder.append(contactNode.getId());
			mStringBuilder.append(",");
			mStringBuilder.append(contactNode.getSourceScore());
			mStringBuilder.append(",");
			mStringBuilder.append(contactNode.getTrustScore());
			mStringBuilder.append(",");
			mStringBuilder.append(contactNode.getIsLocalPhone());
			mStringBuilder.append(",");
			mStringBuilder.append(contactNode.getIsLocalEmail());
			mStringBuilder.append(",");
			mStringBuilder.append(contactNode.getIsFacebook());
			mStringBuilder.append(",");
			mStringBuilder.append(contactNode.getIsTwitter());
			mStringBuilder.append(",");
			mStringBuilder.append(contactNode.getIsLinkedin());
			mStringBuilder.append(",");
			mStringBuilder.append("\n");
		}

		mStringBuilder.append("\n");
		mStringBuilder.append("Couples facebook\n");
		for (int i = 0; i < facebookRelationMap.size(); i++) {
			int key = facebookRelationMap.keyAt(i);
			String[] ids = facebookRelationMap.get(key).split(";");
			if (key != 0 && ids.length > 0 && ids[0] != "") {
				for (int j = 0; j < ids.length; j++) {
					mStringBuilder.append(key);
					mStringBuilder.append(",");
					mStringBuilder.append(ids[j]);
					mStringBuilder.append("\n");
				}
			}
		}

		FileOutputStream fos = context.openFileOutput(Config.EXPORT_FILE_NAME,
				Context.MODE_WORLD_READABLE);
		fos.write(mStringBuilder.toString().getBytes());
		fos.close();
	}
}
