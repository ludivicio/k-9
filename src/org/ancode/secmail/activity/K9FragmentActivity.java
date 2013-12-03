package org.ancode.secmail.activity;

import org.ancode.secmail.activity.K9ActivityCommon.K9ActivityMagic;
import org.ancode.secmail.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;

import android.os.Bundle;
import android.view.MotionEvent;

import com.actionbarsherlock.app.SherlockFragmentActivity;


public class K9FragmentActivity extends SherlockFragmentActivity implements K9ActivityMagic {

    private K9ActivityCommon mBase;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        mBase = K9ActivityCommon.newInstance(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mBase.preDispatchTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void setupGestureDetector(OnSwipeGestureListener listener) {
        mBase.setupGestureDetector(listener);
    }
}
