package com.sailstech.mapping.ui;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.sailstech.mapping.R;
import com.sailstech.mapping.util.DensityUtils;
import com.sailstech.mapping.util.KeyboardUtils;

public class LocShareNameDialog {

    private Dialog dialog;
    private View contentView;
    private EditText etName;
    private Context context;
    private String groupId = "";
    private boolean isOwner = false;

    public LocShareNameDialog(Context context, OnFinishClickListener onFinishClickListener) {
        this.context = context;
        LayoutInflater inflater = LayoutInflater.from(context);
        contentView = inflater.inflate(R.layout.ipsmap_dialog_loc_share_name, null);
        etName = (EditText) contentView.findViewById(R.id.et_name);
        contentView.findViewById(R.id.tv_cancel).setOnClickListener(v -> dismiss());
        contentView.findViewById(R.id.iv_close).setOnClickListener(v -> dismiss());
        contentView.findViewById(R.id.tv_finish).setOnClickListener(v -> {
            String name =etName.getText().toString();
            if(TextUtils.isEmpty(name.trim())){
                Toast.makeText(context,"照片名称不能为空",Toast.LENGTH_SHORT).show();
//                T.showShort(R.string.ipsmap_empty_content);
                return;
            }
            onFinishClickListener.onFinish(name);
            dismiss();
        });
        dialog = new Dialog(context, R.style.IpsmapDialogBottom);
        dialog.setContentView(contentView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setCanceledOnTouchOutside(false);
        Window win = dialog.getWindow();
        WindowManager.LayoutParams lp = win.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        win.setAttributes(lp);
        int padding = DensityUtils.dp2px(context, 16);
        win.getDecorView().setPadding(padding, 0, padding, padding);
        win.setGravity(Gravity.BOTTOM);
        dialog.setCancelable(false);
    }

    public void show() {
        if(dialog.isShowing()){
            return;
        }
        KeyboardUtils.openKeybord(context, etName);
        dialog.show();
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            etName.setText("");
            KeyboardUtils.closeKeybord(context, etName);
            dialog.dismiss();
        }
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getUserName() {
        return etName.getText().toString();
    }



    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
    }

    public interface OnFinishClickListener{
        void onFinish(String userName);
    }

}
