package org.ancode.secmail.guide;

import org.ancode.secmail.R;
import org.ancode.secmail.helper.GuideHelper;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.view.View;
import android.widget.ImageView;

public class MessageListGuide extends GuideHelper{

	public MessageListGuide(Context context) {
		super(context);
	}

	@Override
	public void onCreate(View view) {
		ImageView gestureView = (ImageView) view.findViewById(R.id.iv_gesture);
		gestureView.setBackgroundResource(R.anim.message_list_guide);
		AnimationDrawable animDrawable = (AnimationDrawable) gestureView.getBackground();
		animDrawable.start();
	}

}
