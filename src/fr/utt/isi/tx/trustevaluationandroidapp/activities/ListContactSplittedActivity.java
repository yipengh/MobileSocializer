package fr.utt.isi.tx.trustevaluationandroidapp.activities;

import java.util.ArrayList;
import java.util.List;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;

import fr.utt.isi.tx.trustevaluationandroidapp.MainActivity;
import fr.utt.isi.tx.trustevaluationandroidapp.R;
import fr.utt.isi.tx.trustevaluationandroidapp.fragments.FacebookFriendListFragment;
import fr.utt.isi.tx.trustevaluationandroidapp.fragments.LinkedinContactListFragment;
import fr.utt.isi.tx.trustevaluationandroidapp.fragments.LocalEmailListFragment;
import fr.utt.isi.tx.trustevaluationandroidapp.fragments.LocalPhoneListFragment;
import fr.utt.isi.tx.trustevaluationandroidapp.fragments.TwitterFriendListFragment2;
import fr.utt.isi.tx.trustevaluationandroidapp.models.LocalContact;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Window;

public class ListContactSplittedActivity extends ActionBarActivity implements
		ActionBar.TabListener {

	// tag for log
	private static final String TAG = "ListContactSplittedActivity";

	// key of contact type passed in bundles
	public static final String KEY_CONTACT_TYPE = "contact_type";

	// contact types
	public static final int LOCAL_PHONE = 0;
	public static final int LOCAL_EMAIL = 1;
	public static final int FACEBOOK = 2;
	public static final int TWITTER = 3;
	public static final int LINKEDIN = 4;
	public static final int CONTACT_TYPE_COUNT = LINKEDIN + 1;
	
	public static List<LocalContact> localPhoneList = null;
	
	@SuppressWarnings("rawtypes")
	public static List<ArrayList> contactLists = new ArrayList<ArrayList>(CONTACT_TYPE_COUNT);

	// contact type
	public static int contactType = LOCAL_PHONE;
	
	// fragment array index
	private static final int LOCAL_PHONE_LIST_FRAGMENT = 0;
	private static final int LOCAL_EMAIL_LIST_FRAGMENT = 1;
	private static final int FACEBOOK_FRIEND_LIST_FRAGMENT = 2;
	private static final int TWITTER_FRIEND_LIST_FRAGMENT = 3;
	private static final int LINKEDIN_CONTACT_LIST_FRAGMENT = 4;
	private static final int FACEBOOK_USER_SETTINGS_FRAGMENT = 5;

	// number of fragments
	private static final int FRAGMENT_COUNT = FACEBOOK_USER_SETTINGS_FRAGMENT + 1;

	// fragment array
	private Fragment[] fragments = new Fragment[FRAGMENT_COUNT];

	// pager adapter
	private ContactFragmentPagerAdapter mPagerAdapter;

	// view pager
	private ViewPager mViewPager;
	
	// progress dialog for all fragments
	public static ProgressDialog mProgressDialog;

	/**
	 * Facebook UiLifecycleHelper
	 */
	private UiLifecycleHelper uiHelper;

	/**
	 * Facebook session state change listener
	 */
	private Session.StatusCallback callback = new Session.StatusCallback() {

		@Override
		public void call(Session session, SessionState state,
				Exception exception) {
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "activity state: onCreate");
		super.onCreate(savedInstanceState);

		// create facebook ui lifecycle helper and pass the listener
		uiHelper = new UiLifecycleHelper(this, callback);
		uiHelper.onCreate(savedInstanceState);

		setContentView(R.layout.activity_list_contact_splitted);
		
		// setup the progress dialog
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mProgressDialog.setMessage("Loading...");

		// create pager adapter
		mPagerAdapter = new ContactFragmentPagerAdapter(
				getSupportFragmentManager());

		// get action bar by support library
		final ActionBar actionBar = getSupportActionBar();

		// set navigation mode to tab mode
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		actionBar.setDisplayShowHomeEnabled(true);

		// get view pager
		mViewPager = (ViewPager) findViewById(R.id.pager);

		// set pager adapter to view pager
		mViewPager.setAdapter(mPagerAdapter);

		// set listener
		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		// add tabs to action bar
		actionBar.addTab(actionBar.newTab().setText("PhoneBook")
				.setTag(LOCAL_PHONE).setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText("EmailBook")
				.setTag(LOCAL_EMAIL).setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText("Facebook")
				.setTag(FACEBOOK).setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText("Twitter").setTag(TWITTER)
				.setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText("LinkedIn")
				.setTag(LINKEDIN).setTabListener(this));
		
		for (int i = 0; i < CONTACT_TYPE_COUNT; i++) {
			contactLists.add(null);
		}
		
		MainActivity.mProgressDialog.dismiss();
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {

	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		contactType = (Integer) tab.getTag();
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {

	}

	@Override
	public void onResume() {
		super.onResume();
		uiHelper.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		uiHelper.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		uiHelper.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		uiHelper.onSaveInstanceState(outState);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Session.getActiveSession().onActivityResult(this, requestCode,
				resultCode, data);
	}
	
	private class ContactFragmentPagerAdapter extends FragmentPagerAdapter {

		public ContactFragmentPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int fragmentIndex) {
			Log.v("PagerAdapter", "fragment index: " + fragmentIndex);
			Fragment f;
			switch (fragmentIndex) {
			default:
				return null;
			case LOCAL_PHONE_LIST_FRAGMENT:
				if (fragments[LOCAL_PHONE_LIST_FRAGMENT] != null) {
					return fragments[LOCAL_PHONE_LIST_FRAGMENT];
				}
				f = new LocalPhoneListFragment();
				fragments[LOCAL_PHONE_LIST_FRAGMENT] = f;
				return f;
			case LOCAL_EMAIL_LIST_FRAGMENT:
				if (fragments[LOCAL_EMAIL_LIST_FRAGMENT] != null) {
					return fragments[LOCAL_EMAIL_LIST_FRAGMENT];
				}
				f = new LocalEmailListFragment();
				fragments[LOCAL_EMAIL_LIST_FRAGMENT] = f;
				return f;
			case FACEBOOK_FRIEND_LIST_FRAGMENT:
				if (fragments[FACEBOOK_FRIEND_LIST_FRAGMENT] != null) {
					return fragments[FACEBOOK_FRIEND_LIST_FRAGMENT];
				}
				f = new FacebookFriendListFragment();
				fragments[FACEBOOK_FRIEND_LIST_FRAGMENT] = f;
				return f;
			case TWITTER_FRIEND_LIST_FRAGMENT:
				if (fragments[TWITTER_FRIEND_LIST_FRAGMENT] != null) {
					return fragments[TWITTER_FRIEND_LIST_FRAGMENT];
				}
				f = new TwitterFriendListFragment2();
				fragments[TWITTER_FRIEND_LIST_FRAGMENT] = f;
				return f;
			case LINKEDIN_CONTACT_LIST_FRAGMENT:
				if (fragments[LINKEDIN_CONTACT_LIST_FRAGMENT] != null) {
					return fragments[LINKEDIN_CONTACT_LIST_FRAGMENT];
				}
				f = new LinkedinContactListFragment();
				fragments[LINKEDIN_CONTACT_LIST_FRAGMENT] = f;
				return f;
			}
		}

		@Override
		public int getCount() {
			return FRAGMENT_COUNT - 1;
		}

	}

}
