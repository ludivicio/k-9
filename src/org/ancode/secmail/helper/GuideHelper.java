package org.ancode.secmail.helper;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

public abstract class GuideHelper {

	private static Context mContext;
	private WindowManager wm;
	private WindowManager.LayoutParams params;
	private LinearLayout layout;
	private boolean isShow = false;
	private ViewPager viewPager;
	private String mClassName;

	public GuideHelper(Context context) {
		mContext = context;
		mClassName = mContext.getClass().getSimpleName();
		
	}

	public abstract void onCreate(View view);

	public void showGuide(int layoutId) {
		isShow = true;
		initializeLayout();
		initializeViewPager();
		initializeData(layoutId);
	}

	public void hideGuide() {
		if (isShow) {
			wm.removeView(layout);
			isShow = false;
		}
	}

	public boolean isFristRun() {
		SharedPreferences sp = mContext.getSharedPreferences(
				mClassName.toLowerCase() + "_guide", Context.MODE_PRIVATE);
		boolean isFirstRun = sp.getBoolean("first_run", true);
		return isFirstRun;
	}

	public void saveStatus() {
		SharedPreferences sp = mContext.getSharedPreferences(
				mClassName.toLowerCase() + "_guide", Context.MODE_PRIVATE);
		Editor editor = sp.edit();
		editor.putBoolean("first_run", false);
		editor.commit();
	}

	private void initializeLayout() {
		wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

		DisplayMetrics dm = new DisplayMetrics();
		dm = mContext.getResources().getDisplayMetrics();
		int width = dm.widthPixels;
		int height = dm.heightPixels;

		params = new WindowManager.LayoutParams();
		params.type = 2002;
		params.format = 1;
		params.flags = 40;
		params.width = width;
		params.height = height;

		layout = new LinearLayout(mContext);
		wm.addView(layout, params);
	}

	private void initializeViewPager() {
		viewPager = new ViewPager(mContext);
		viewPager.setLayoutParams(params);
		layout.addView(viewPager);
	}

	private ArrayList<View> initializeData(int id) {

		ArrayList<View> views = new ArrayList<View>();

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT);

		ImageView iv = new ImageView(mContext);
		iv.setBackgroundColor(Color.TRANSPARENT);
		iv.setLayoutParams(params);
		views.add(iv);

		View guideView = LayoutInflater.from(mContext).inflate(id, null);

		onCreate(guideView);

		views.add(guideView);

		GuidePagerAdapter adapter = new GuidePagerAdapter(views);
		viewPager.setAdapter(adapter);
		viewPager.setCurrentItem(1);
		viewPager.setOnPageChangeListener(onPageChangeListener);

		return views;
	}

	private OnPageChangeListener onPageChangeListener = new OnPageChangeListener() {

		private int position = 1;

		@Override
		public void onPageScrollStateChanged(int arg0) {
			if (position == 0) {
				layout.setVisibility(View.GONE);
				position = 1;
				hideGuide();
			}
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageSelected(int arg0) {
			position = arg0;
		}

	};

	private class GuidePagerAdapter extends PagerAdapter {

		private List<View> views;

		public GuidePagerAdapter(List<View> views) {
			this.views = views;
		}

		@Override
		public int getCount() {
			if (views != null) {
				return views.size();
			}
			return 0;
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

		@Override
		public void destroyItem(View container, int position, Object object) {
			((ViewPager) container).removeView(views.get(position));
		}

		@Override
		public Object instantiateItem(View container, int position) {
			((ViewPager) container).addView(views.get(position), 0);
			return views.get(position);
		}
	}
}
