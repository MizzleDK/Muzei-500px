package com.miz.muzei.fivehundredpx;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

public class SettingsActivity extends Activity {

	public static final String HIDE_ADULT_CONTENT = "prefsHideAdultContent";
	public static final String UPDATE_ON_WIFI_ONLY = "prefsUpdateOnWifiOnly";

	private String[] mCategoriesArray, mCategoriesUrlArray;

	private List<Source> mFeatureList = new ArrayList<Source>();
	private List<Source> mCategoriesList = new ArrayList<Source>();
	private List<Interval> mIntervalList = new ArrayList<Interval>();

	private TextView mDataSources, mCategorySettings, mOther, mFeature, mCategory, mRefreshInterval, mCategories;
	private Spinner mFeatureSpinner, mRefreshIntervalSpinner;
	private CheckBox mHideAdultContent, mUpdateOnWifiOnly;
	private Button mEditCategories;
	private Editor mEditor;

	private Typeface tf;

	private long startTime;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings);

		mEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();

		tf = Typeface.createFromAsset(getAssets(), "RobotoCondensed-Regular.ttf");

		startTime = System.currentTimeMillis();

		mDataSources = (TextView) findViewById(R.id.dataSources);
		mDataSources.setTypeface(tf);

		mCategorySettings = (TextView) findViewById(R.id.categorySettings);
		mCategorySettings.setTypeface(tf);

		mOther = (TextView) findViewById(R.id.other);
		mOther.setTypeface(tf);

		mFeature = (TextView) findViewById(R.id.feature);
		mFeature.setTypeface(tf);

		mCategory = (TextView) findViewById(R.id.category);
		mCategory.setTypeface(tf);

		mRefreshInterval = (TextView) findViewById(R.id.refreshInterval);
		mRefreshInterval.setTypeface(tf);

		mCategories = (TextView) findViewById(R.id.categories);

		mEditCategories = (Button) findViewById(R.id.editCategories);
		mEditCategories.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showEditCategoriesDialog();
			}
		});

		mFeatureSpinner = (Spinner) findViewById(R.id.featureSpinner);
		mRefreshIntervalSpinner = (Spinner) findViewById(R.id.refreshIntervalSpinner);

		mHideAdultContent = (CheckBox) findViewById(R.id.hideAdultContent);
		mHideAdultContent.setChecked(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(HIDE_ADULT_CONTENT, false));
		mHideAdultContent.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditor.putBoolean(HIDE_ADULT_CONTENT, isChecked);
				mEditor.commit();

				// Update the image if the selected category is Nude
				if (isChecked && PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this).getString(FiveHundredPxArtSource.CATEGORY_PREFERENCE, "").equals("Nude"))
					setupCategorySpinner();
			}
		});

		mUpdateOnWifiOnly = (CheckBox) findViewById(R.id.updateOnWifiOnly);
		mUpdateOnWifiOnly.setChecked(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(UPDATE_ON_WIFI_ONLY, false));
		mUpdateOnWifiOnly.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditor.putBoolean(UPDATE_ON_WIFI_ONLY, isChecked);
				mEditor.commit();
			}
		});

		mCategoriesArray = getResources().getStringArray(R.array.categoryNames);
		mCategoriesUrlArray = new String[mCategoriesArray.length];
		for (int i = 0; i < mCategoriesArray.length; i++)
			try {
				mCategoriesUrlArray[i] = URLEncoder.encode(mCategoriesArray[i], "UTF-8");
			} catch (UnsupportedEncodingException e) {}

		// Set up feature spinner
		setupFeatureSpinner();

		// Set up categories spinner
		setupCategorySpinner();

		// Set up interval spinner
		setupIntervalSpinner();
	}

	private void setupFeatureSpinner() {
		mFeatureList.clear();
		mFeatureList.add(new Source(getString(R.string.popular), FiveHundredPxArtSource.POPULAR));
		mFeatureList.add(new Source(getString(R.string.fresh), FiveHundredPxArtSource.FRESH));
		mFeatureList.add(new Source(getString(R.string.upcoming), FiveHundredPxArtSource.UPCOMING));
		mFeatureList.add(new Source(getString(R.string.editors), FiveHundredPxArtSource.EDITORS));

		mFeatureSpinner.setAdapter(new ArrayAdapter<Source>(this, android.R.layout.simple_list_item_1, mFeatureList));
		mFeatureSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				mEditor.putString(FiveHundredPxArtSource.FEATURE_PREFERENCE, mFeatureList.get(arg2).getUrlName());
				mEditor.commit();

				forceUpdate();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});

		String preference = PreferenceManager.getDefaultSharedPreferences(this).getString(FiveHundredPxArtSource.FEATURE_PREFERENCE, FiveHundredPxArtSource.POPULAR);

		for (int i = 0; i < mFeatureList.size(); i++)
			if (preference.equals(mFeatureList.get(i).getUrlName()))
				mFeatureSpinner.setSelection(i, true);
	}

	private void setupCategorySpinner() {
		mCategoriesList.clear();

		boolean hideAdultContent = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(HIDE_ADULT_CONTENT, false);
		String nudeCategory = getString(R.string.nudeCategory);

		for (int i = 0; i < mCategoriesArray.length; i++) {
			if (hideAdultContent && mCategoriesArray[i].equals(nudeCategory))
				continue;
			mCategoriesList.add(new Source(mCategoriesArray[i], mCategoriesUrlArray[i]));
		}

		String preference = PreferenceManager.getDefaultSharedPreferences(this).getString(FiveHundredPxArtSource.CATEGORY_PREFERENCE, "");
		if (preference.isEmpty()) {
			mCategories.setText(R.string.allCategories);
		} else {
			String[] preferences = preference.split(",");
			StringBuilder sb = new StringBuilder();
			for (String pref : preferences) {
				for (int i = 0; i < mCategoriesList.size(); i++)
					if (pref.equals(mCategoriesList.get(i).getUrlName()))
						sb.append(mCategoriesList.get(i).getName() + " ,");
			}
			mCategories.setText(sb.substring(0, sb.length() - 2)); // Remove ' ,' at the end
		}

		/*mCategorySpinner.setAdapter(new ArrayAdapter<Source>(this, android.R.layout.simple_list_item_1, mCategoriesList));
		mCategorySpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				mEditor.putString(FiveHundredPxArtSource.CATEGORY_PREFERENCE, mCategoriesList.get(arg2).getUrlName());
				mEditor.commit();

				forceUpdate();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});

		String preference = PreferenceManager.getDefaultSharedPreferences(this).getString(FiveHundredPxArtSource.CATEGORY_PREFERENCE, "");

		for (int i = 0; i < mCategoriesList.size(); i++)
			if (preference.equals(mCategoriesList.get(i).getUrlName()))
				mCategorySpinner.setSelection(i, true);*/
	}

	private void setupIntervalSpinner() {
		mIntervalList.clear();
		mIntervalList.add(new Interval(R.string.everyHour, 1000 * 60 * 60 * 1));
		mIntervalList.add(new Interval(R.string.everyThreeHours, 1000 * 60 * 60 * 3));
		mIntervalList.add(new Interval(R.string.everySixHours, 1000 * 60 * 60 * 6));
		mIntervalList.add(new Interval(R.string.everyNineHours, 1000 * 60 * 60 * 9));
		mIntervalList.add(new Interval(R.string.everyTwelveHours, 1000 * 60 * 60 * 12));
		mIntervalList.add(new Interval(R.string.everyDay, 1000 * 60 * 60 * 24));

		mRefreshIntervalSpinner.setAdapter(new ArrayAdapter<Interval>(this, android.R.layout.simple_list_item_1, mIntervalList));
		mRefreshIntervalSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				mEditor.putInt(FiveHundredPxArtSource.REFRESH_INTERVAL_PREFERENCE, mIntervalList.get(arg2).getTimeMillis());
				mEditor.commit();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}	
		});

		int preference = PreferenceManager.getDefaultSharedPreferences(this).getInt(FiveHundredPxArtSource.REFRESH_INTERVAL_PREFERENCE, 1000 * 60 * 60 * 3);

		for (int i = 0; i < mIntervalList.size(); i++)
			if (preference == mIntervalList.get(i).getTimeMillis())
				mRefreshIntervalSpinner.setSelection(i, true);
	}

	private void forceUpdate() {
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(UPDATE_ON_WIFI_ONLY, false) && !MizLib.isWifiConnected(this))
			return;

		if (startTime + 500 < System.currentTimeMillis()) // Hack
			startService(new Intent(SettingsActivity.this, FiveHundredPxArtSource.class).setAction(FiveHundredPxArtSource.ACTION_FORCE_UPDATE));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	private void showEditCategoriesDialog() {
		final ArrayList<Integer> mSelectedItems = new ArrayList<Integer>();  // Where we track the selected items

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.categories)
		// Specify the list array, the items to be selected by default (null for none),
		// and the listener through which to receive callbacks when items are selected
		.setMultiChoiceItems(R.array.categoryNames, null,
				new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				if (which == 0) {

				}


				if (isChecked) {
					// If the user checked the item, add it to the selected items
					mSelectedItems.add(which);
				} else if (mSelectedItems.contains(which)) {
					// Else, if the item is already in the array, remove it 
					mSelectedItems.remove(Integer.valueOf(which));
				}
			}
		})
		.setNeutralButton(R.string.allCategories, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub

			}
		})
		// Set the action buttons
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				// User clicked OK, so save the mSelectedItems results somewhere
				// or return them to the component that opened the dialog

			}
		})
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {

			}
		});

		builder.show();
	}

	/**
	 * Inner class so we don't have to pass a Context each time we want to use getString().
	 * @author Michell
	 *
	 */
	private class Interval {

		private int name, timeMillis;

		public Interval(int name, int timeMillis) {
			this.name = name;
			this.timeMillis = timeMillis;
		}

		public String getName() {
			return getString(name);
		}

		public int getTimeMillis() {
			return timeMillis;
		}

		@Override
		public String toString() {
			return getName();
		}
	}
}