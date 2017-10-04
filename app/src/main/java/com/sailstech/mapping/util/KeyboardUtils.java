package com.sailstech.mapping.util;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;


public class KeyboardUtils
{
	/**
	 * 打卡软键盘
	 * @param mContext
	 *            上下文
	 * @param mEditText
	 *            输入框
	 *
	 */
	public static void openKeybord(Context mContext, View mEditText)
	{
		InputMethodManager imm = (InputMethodManager) mContext
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(mEditText, InputMethodManager.RESULT_SHOWN);
		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
				InputMethodManager.HIDE_IMPLICIT_ONLY);
	}

	/**
	 * 关闭软键盘
	 * @param mContext
	 *            上下文
	 * @param mEditText
	 *            输入框
	 *
	 */
	public static void closeKeybord(Context mContext, View mEditText)
	{
		InputMethodManager imm = (InputMethodManager) mContext
				.getSystemService(Context.INPUT_METHOD_SERVICE);

		imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
	}
	
	public static void hideKeyboard(Activity activity) {
		InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
		View view=activity.getCurrentFocus();
		if(view!=null){
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}												 
	}

}

