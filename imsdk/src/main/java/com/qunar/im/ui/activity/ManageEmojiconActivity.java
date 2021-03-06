package com.qunar.im.ui.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.qunar.im.base.module.UserConfigData;
import com.qunar.im.ui.presenter.IManageEmotionPresenter;
import com.qunar.im.ui.presenter.impl.ManageEmotionPresenter;
import com.qunar.im.ui.presenter.views.IManageEmotionView;
import com.qunar.im.ui.util.EmotionUtils;
import com.qunar.im.ui.R;
import com.qunar.im.ui.adapter.ManageEmojiconAdapter;
import com.qunar.im.ui.imagepicker.ImagePicker;
import com.qunar.im.base.module.ImageItem;
import com.qunar.im.ui.imagepicker.ui.ImageGridActivity;
import com.qunar.im.ui.view.QtNewActionBar;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhaokai on 16-1-21.
 */
public class ManageEmojiconActivity extends IMBaseActivity implements IManageEmotionView {

    public static final int ADD_EMOTICON = 0x32;
    List<String> selectedImages;
    GridView grid;
    TextView tv_delete;
    RelativeLayout bottom_container;
    boolean deleteState = false;
    ManageEmojiconAdapter adapter;
    IManageEmotionPresenter manageEmotionPresenter = new ManageEmotionPresenter();
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.atom_ui_activity_manage_emojicon);
        manageEmotionPresenter.setView(this);
        bindViews();
        initView();
    }

    private void bindViews() {
        grid = (GridView) findViewById(R.id.grid);
        tv_delete = (TextView) findViewById(R.id.tv_delete);
        bottom_container = (RelativeLayout) findViewById(R.id.bottom_container);
        tv_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(adapter.getSelectList().size()>0) {
                    manageEmotionPresenter.deleteEmotions();
                }
            }
        });
    }

    void initView(){
        final QtNewActionBar actionBar = (QtNewActionBar) this.findViewById(R.id.my_action_bar);
        setNewActionBar(actionBar);
        setActionBarTitle(R.string.atom_ui_title_my_emojicon);
        setActionBarRightText(R.string.atom_ui_btn_manage);
        setActionBarRightTextClick(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!deleteState) {
                    //???????????????&????????????
                    setActionBarRightText(R.string.atom_ui_common_complete);
                    deleteState = true;
//                    bottom_container.setVisibility(View.VISIBLE);
                    adapter.setStatus(deleteState);
                } else {
//                    bottom_container.setVisibility(View.GONE);
                    //???????????????
                    if(adapter.getSelectList().size()>0) {
                        showDeleteDialog();
                    }else{
                        //???????????????????????????
                        deleteState = false;
                        setActionBarRightText(R.string.atom_ui_btn_manage);
                        adapter.setStatus(deleteState);
                    }
                }
            }
        });

        adapter = new ManageEmojiconAdapter(this,EmotionUtils.getFavorEmoticonFileDir());
        adapter.setAddEmojBtnEvent(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPic();
            }
        });
        grid.setAdapter(adapter);
        manageEmotionPresenter.loadLocalEmotions();
    }

    void selectPic() {
//        Intent intent = new Intent(ManageEmojiconActivity.this, PictureSelectorActivity.class);
//        intent.putExtra(PictureSelectorActivity.TYPE, PictureSelectorActivity.TYPE_EMOJICON);
//        intent.putExtra(PictureSelectorActivity.SHOW_EDITOR, false);
//        startActivityForResult(intent, ADD_EMOTICON);
        //?????????????????????
        ImagePicker.getInstance().setSelectLimit(9);
        Intent intent1 = new Intent(this, ImageGridActivity.class);
                                /* ???????????????????????????????????????????????????????????????
                                 * ???????????????ImagePickerActivity
                                 * */
//                                intent1.putExtra(ImageGridActivity.EXTRAS_IMAGES,images);
        startActivityForResult(intent1, ADD_EMOTICON);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK)
            return;
        if (requestCode == ADD_EMOTICON) {
//            selectedImages= data.getStringArrayListExtra(PictureSelectorActivity.KEY_SELECTED_PIC);
            selectedImages = new ArrayList<>();
            //?????????????????????
            ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
            if (images.size() > 0) {
                for (ImageItem image : images) {
                    selectedImages.add(image.path);
                }
            }
            manageEmotionPresenter.addEmotions();
            selectedImages = null;
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        if(deleteState){//??????????????????
            setActionBarRightText(R.string.atom_ui_btn_manage);
            deleteState = false;
            adapter.setStatus(deleteState);
        }else{
            setResult(RESULT_OK);
            finish();
        }
    }

    private void showDeleteDialog(){
        commonDialog.setTitle(getString(R.string.atom_ui_tip_dialog_prompt));
        commonDialog.setMessage(getString(R.string.atom_ui_tip_message_delete));
        commonDialog.setPositiveButton(getString(R.string.atom_ui_common_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                dialog.dismiss();
                setActionBarRightText(R.string.atom_ui_btn_manage);
                manageEmotionPresenter.deleteEmotions();
                //???????????????????????????

            }
        });

        commonDialog.setNegativeButton(getString(R.string.atom_ui_common_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        commonDialog.create().show();
    }

    @Override
    public List<UserConfigData> getDeletedEmotions() {
        return adapter.getSelectList();
    }

    @Override
    public List<String> getAddedEmotions() {
        return selectedImages;
    }

    @Override
    public void updateSuccessful() {
//        initView();
        deleteState = false;
//        adapter.setStatus(deleteState);
    }

    @Override
    public void setEmotionList(final List<String> list) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.setFilePaths(list);
                adapter.setStatus(deleteState);
            }
        });

    }

    @Override
    public void setEmotionNewList(final List<UserConfigData> list) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.setFileHtml(list);
                adapter.setStatus(deleteState);
            }
        });
    }
}
