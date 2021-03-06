package com.qunar.im.ui.util.videoPlayUtil;

import android.app.Dialog;
import android.os.Environment;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.qunar.im.ui.util.easyphoto.easyphotos.EasyPhotos;
import com.orhanobut.logger.Logger;
import com.qunar.im.base.module.DownLoadFileResponse;
import com.qunar.im.base.protocol.ProtocolCallback;
import com.qunar.im.core.services.FileProgressResponseBody;
import com.qunar.im.ui.R;
import com.qunar.im.ui.util.ShareUtil;
import com.qunar.im.ui.view.CommonDialog;
import com.qunar.im.utils.HttpUtil;

import java.io.File;

import static com.qunar.im.base.util.Utils.showToast;

public class VideoPlayUtil {

    protected static CommonDialog.Builder commonDialog;

    public static VideoBuilder createVideoPlay(FragmentActivity activity) {
        return VideoBuilder.createVideoBuilder(activity);
    }

    public static VideoBuilder createVideoPlay(Fragment fragmentV) {
        return VideoBuilder.createVideoBuilder(fragmentV);

    }

    public static void openLocalVideo(final FragmentActivity activity, String playPath, String fileName, String firstThumb){
        VideoBuilder.createVideoBuilder(activity)
                .setPlayPath(playPath)
                .setAutoPlay(true)
                .setFileName(fileName)
                .setPlayThumb(firstThumb)
                .setShowFull(false)
                .setShowShare(false)
                .setIsCycle(false)
                .start();
    }


    public static void conAndWwOpen(final FragmentActivity activity, String playPath, String fileName, String firstThumb, String downPath, boolean onlyDownLoad, String fileSize) {
        VideoBuilder.createVideoBuilder(activity)
                .setPlayPath(playPath)
                .setAutoPlay(true)
                .setFileName(fileName)
                .setPlayThumb(firstThumb)
                .setDownLoadPath(downPath)
                .setShowFull(true)
                .setShowShare(true)
                .setFileSize(fileSize)
                .setIsCycle(true)
                .setOnlyDownLoad(onlyDownLoad)
                .setDownLoadLis(new VideoDownLoadCallback() {
                    @Override
                    public void onClickDownLoad(final View v,String palyPath, String downloadPath, String fileName) {
                        String filepath = Environment.getExternalStorageDirectory().getAbsolutePath();
                        File file = new File(filepath, fileName);
                        if (file.exists()) {
                            Logger.i("????????????????????????:" + file.getAbsolutePath());
                            showToast(v.getContext(), "?????????????????????");
                        } else {
                            HttpUtil.fileDownload(downloadPath, fileName, new FileProgressResponseBody.ProgressResponseListener() {
                                @Override
                                public void onResponseProgress(long bytesRead, long contentLength, boolean done) {
                                    Logger.i("??????????????????:bytesRead:" + bytesRead + ",contentLength:" + contentLength + ",done:" + done);
//

                                }
                            }, new ProtocolCallback.UnitCallback<DownLoadFileResponse>() {
                                @Override
                                public void onCompleted(final DownLoadFileResponse videoDataResponse) {
                                    Logger.i("??????????????????");


                                }

                                @Override
                                public void onFailure(String errMsg) {
                                }
                            });
                        }
                    }
                })
                .setShareLis(new VideoShareCallback() {
                    @Override
                    public void onClickShare(final View v, final String playPath, final String downloadPath, final String fileName) {
                        commonDialog = new CommonDialog.Builder(v.getContext());
                        // ...
                        commonDialog.setItems(v.getContext().getResources().getStringArray(R.array.atom_ui_video_share)).setOnItemClickListener(new CommonDialog.Builder.OnItemClickListener() {
                            @Override
                            public void OnItemClickListener(Dialog dialog, int postion) {
                                switch (postion) {
                                    case 0:

                                       File tFile = new File(playPath);
                                       if(tFile.exists()){
                                           Logger.i("??????????????????:" + tFile.getAbsolutePath());
                                           showToast(v.getContext(), "???????????????");
                                           ShareUtil.shareVideo(v.getContext(), tFile, "????????????");
                                       }else {
                                           String filepath = Environment.getExternalStorageDirectory().getAbsolutePath();
                                           File file = new File(filepath, fileName);
                                           if (file.exists()) {
                                               Logger.i("????????????????????????:" + file.getAbsolutePath());
                                               showToast(v.getContext(), "?????????????????????");
                                               ShareUtil.shareVideo(v.getContext(), file, "????????????");
                                           } else {

                                               showToast(v.getContext(), "????????????");
                                               HttpUtil.fileDownload(downloadPath, fileName, new FileProgressResponseBody.ProgressResponseListener() {
                                                   @Override
                                                   public void onResponseProgress(long bytesRead, long contentLength, boolean done) {
                                                       Logger.i("??????????????????:bytesRead:" + bytesRead + ",contentLength:" + contentLength + ",done:" + done);
                                                   }
                                               }, new ProtocolCallback.UnitCallback<DownLoadFileResponse>() {
                                                   @Override
                                                   public void onCompleted(final DownLoadFileResponse videoDataResponse) {
                                                       Logger.i("??????????????????");


                                                   }

                                                   @Override
                                                   public void onFailure(String errMsg) {
                                                   }
                                               });
                                               break;

                                           }
                                       }
                                }
                            }

                        }).create().show();
                    }
                }).start();
    }


}
