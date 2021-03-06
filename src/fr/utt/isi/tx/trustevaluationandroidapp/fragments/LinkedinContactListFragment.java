package fr.utt.isi.tx.trustevaluationandroidapp.fragments;

import java.util.List;

import org.brickred.socialauth.Contact;
import org.brickred.socialauth.android.DialogListener;
import org.brickred.socialauth.android.SocialAuthAdapter;
import org.brickred.socialauth.android.SocialAuthError;
import org.brickred.socialauth.android.SocialAuthListener;
import org.brickred.socialauth.android.SocialAuthAdapter.Provider;

import fr.utt.isi.tx.trustevaluationandroidapp.R;
import fr.utt.isi.tx.trustevaluationandroidapp.activities.ListContactSplittedActivity;
import fr.utt.isi.tx.trustevaluationandroidapp.adapters.SocialAuthContactListAdapter;
import fr.utt.isi.tx.trustevaluationandroidapp.config.Config;
import fr.utt.isi.tx.trustevaluationandroidapp.database.TrustEvaluationDataContract;
import fr.utt.isi.tx.trustevaluationandroidapp.database.TrustEvaluationDbHelper;
import fr.utt.isi.tx.trustevaluationandroidapp.tasks.MatchingTask;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

public class LinkedinContactListFragment extends Fragment implements
		OnClickListener {

	// Tag for debug
	private static final String TAG = "LinkedinContactListFragment";

	// database helper
	private static TrustEvaluationDbHelper mDbHelper = null;

	// shared preferences
	private static SharedPreferences mSharedPreferences;

	// is first visit
	private boolean isFirstVisit = true;

	// adapter by socialAuth lib
	private SocialAuthAdapter adapter;

	// login button view
	private Button loginButton;

	// friend list view
	private ListView friendList;

	// update button view
	private Button updateButton;

	// whether the authorization (adapter.authorize(...)) is for retrieving
	// contacts or just for the assignment of adapter (used in logout flow)
	private boolean isAuthorizationForContacts = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.v(TAG, "Creating fragment...");

		// get shared preferences
		mSharedPreferences = getActivity().getSharedPreferences(Config.PREF_NAME_LINKEDIN,
				Context.MODE_PRIVATE);

		// get boolean "is_first_visit" from shared preferences
		isFirstVisit = mSharedPreferences.getBoolean(Config.PREF_IS_FIRST_VISIT_LINKEDIN, true);

		adapter = new SocialAuthAdapter(new ResponseListener());

		// notify that the fragment has options menu
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_linkedin_contact_list,
				container, false);

		Log.v(TAG, "Creating view...");

		// get login button view
		loginButton = (Button) view.findViewById(R.id.login_button);
		loginButton.setOnClickListener(this);

		// get friend list view
		friendList = (ListView) view.findViewById(R.id.linkedin_friend_list);

		// get update button view
		updateButton = (Button) view.findViewById(R.id.update_button);
		updateButton.setOnClickListener(this);

		toggleView();

		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		// only add the menu when the fragment is showing
		if (this.isVisible() && !isFirstVisit) {
			if (menu.size() <= 1) {
				menu.add(R.string.logout);
			}
		} else {
			menu.clear();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getTitle().equals(getResources().getString(R.string.logout))) {
			// log out flow
			proceedLogout();

			// recreate options menu
			getActivity().supportInvalidateOptionsMenu();

			return true;
		}
		return false;
	}

	@Override
	public void onResume() {
		super.onResume();

		// create db helper
		if (mDbHelper == null) {
			mDbHelper = new TrustEvaluationDbHelper(getActivity());
		}

		if (!isFirstVisit) {
			// get contact list from database
			List<Contact> contactList = mDbHelper.getLinkedinContacts(null);
			if (contactList != null) {
				SocialAuthContactListAdapter mAdapter = new SocialAuthContactListAdapter(
						getActivity(), R.layout.linkedin_contact_list,
						contactList);
				mAdapter.setProvider(Provider.LINKEDIN);
				friendList.setAdapter(mAdapter);
				return;
			}

			proceed();
		}
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.login_button:
			proceed();
			break;
		case R.id.update_button:
			// update flow
			if (mDbHelper == null) {
				mDbHelper = new TrustEvaluationDbHelper(getActivity());
			}
			// clear table and re-proceed the data retrieve and data insert
			mDbHelper
					.clearTable(TrustEvaluationDataContract.LinkedinContact.TABLE_NAME);
			proceed();
			break;
		default:
			break;
		}
	}

	private void toggleView() {
		if (isFirstVisit) {
			loginButton.setVisibility(View.VISIBLE);
			friendList.setVisibility(View.INVISIBLE);
			updateButton.setVisibility(View.INVISIBLE);
		} else {
			loginButton.setVisibility(View.GONE);
			friendList.setVisibility(View.VISIBLE);
			updateButton.setVisibility(View.VISIBLE);
		}
	}

	private void proceed() {
		if (adapter == null) {
			adapter = new SocialAuthAdapter(new ResponseListener());
		}
		adapter.authorize(getActivity(), Provider.LINKEDIN);
	}

	private void proceedLogout() {
		// re-assign the adapter by sending authorization request without
		// retrieving contacts
		isAuthorizationForContacts = false;
		proceed();

		ListContactSplittedActivity.mProgressDialog.dismiss();

		// sign out via adapter
		adapter.signOut(getActivity(), Provider.LINKEDIN.toString());

		// set "is_first_visit" to true
		isFirstVisit = true;
		Editor e = mSharedPreferences.edit();
		e.putBoolean(Config.PREF_IS_FIRST_VISIT_LINKEDIN, isFirstVisit);
		e.commit();

		toggleView();

		// clear table
		if (mDbHelper == null) {
			mDbHelper = new TrustEvaluationDbHelper(getActivity());
		}
		mDbHelper
				.clearTable(TrustEvaluationDataContract.LinkedinContact.TABLE_NAME);
	}

	private final class ResponseListener implements DialogListener {

		@Override
		public void onComplete(Bundle values) {
			if (isAuthorizationForContacts) {
				ListContactSplittedActivity.mProgressDialog.show();

				// set "is_first_visit" to false
				isFirstVisit = false;
				Editor e = mSharedPreferences.edit();
				e.putBoolean(Config.PREF_IS_FIRST_VISIT_LINKEDIN, isFirstVisit);
				e.commit();

				toggleView();

				getActivity().supportInvalidateOptionsMenu();

				// contact list
				adapter.getContactListAsync(new ContactDataListener());
			}
			isAuthorizationForContacts = true;
		}

		@Override
		public void onError(SocialAuthError error) {
			Log.d(TAG, "Error");
			error.printStackTrace();
		}

		@Override
		public void onCancel() {
			Log.d(TAG, "Cancelled");
		}

		@Override
		public void onBack() {
			Log.d(TAG, "Dialog Closed by pressing Back Key");

		}
	}

	private final class ContactDataListener implements
			SocialAuthListener<List<Contact>> {

		@Override
		public void onExecute(String provider, List<Contact> t) {

			Log.d(TAG, "Receiving Data");
			List<Contact> contactsList = t;

			if (contactsList != null && contactsList.size() > 0) {
				// update database
				mDbHelper.insertLinkedinContact(contactsList);

				// set up the list view
				SocialAuthContactListAdapter mAdapter = new SocialAuthContactListAdapter(
						getActivity(), R.layout.linkedin_contact_list,
						contactsList);
				mAdapter.setProvider(Provider.LINKEDIN);
				friendList.setAdapter(mAdapter);
				
				// do matching in background
				new MatchingTask(getActivity()).execute(ListContactSplittedActivity.LINKEDIN);
			} else {
				Log.d(TAG, "Contact List Empty");
			}

			ListContactSplittedActivity.mProgressDialog.dismiss();
		}

		@Override
		public void onError(SocialAuthError e) {

		}
	}

}
