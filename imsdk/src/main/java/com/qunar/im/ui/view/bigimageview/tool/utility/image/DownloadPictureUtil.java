package com.qunar.im.ui.view.bigimageview.tool.utility.image;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Environment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.qunar.im.ui.view.bigimageview.ImagePreview;
import com.qunar.im.ui.view.bigimageview.glide.engine.SimpleFileTarget;
import com.qunar.im.ui.view.bigimageview.tool.text.MD5Util;
import com.qunar.im.ui.view.bigimageview.tool.utility.file.FileUtil;
import com.qunar.im.ui.view.bigimageview.tool.utility.file.SingleMediaScanner;
import com.qunar.im.ui.view.bigimageview.tool.utility.ui.ToastUtil;
import com.qunar.im.ui.view.bigimageview.view.MyGlideUrl;

import java.io.File;


/**
 * @author 工藤
 * @email gougou@16fan.com
 * com.fan16.cn.util.picture
 * create at 2018/5/4  16:34
 * description:图片下载工具类
 */
public class DownloadPictureUtil {

    public static void downloadPicture(final Context context, final String url, final PicCallBack picCallBack, final boolean showToast) {

                Glide.with(context.getApplicationContext())
//                .load(new MyGlideUrl(url))
                        .load(url.startsWith("file:///")?url:new MyGlideUrl(url))
                        .downloadOnly(new SimpleFileTarget() {
                            @Override
                            public void onLoadStarted(Drawable placeholder) {
                                super.onLoadStarted(placeholder);
                                if(showToast){

                                ToastUtil.getInstance()._short(context, "开始下载...");
                                }
                            }

                            @Override
                            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                super.onLoadFailed(e, errorDrawable);
                                if(showToast){

                                    ToastUtil.getInstance()._short(context, "保存失败");
                                }
                            }

                            @Override
                            public void onResourceReady(File resource, GlideAnimation<? super File> glideAnimation) {
                                final String downloadFolderName = ImagePreview.getInstance().getFolderName();
                                final String path = Environment.getExternalStorageDirectory() + "/" + downloadFolderName + "/";
                                String name = "";
                                try {
                                    name = url.substring(url.lastIndexOf("/") + 1, url.length());
                                    if (name.contains(".")) {
                                        name = name.substring(0, name.lastIndexOf("."));
                                    }
                                    name = MD5Util.md5Encode(name);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    name = System.currentTimeMillis() + "";
                                }
                                String mimeType = ImageUtil.getImageTypeWithMime(resource.getAbsolutePath());
                                name = name + "." + mimeType;
                                FileUtil.createFileByDeleteOldFile(path + name);
                                boolean result = FileUtil.copyFile(resource, path, name);
                                if (result) {
                                    if(showToast){

                                        ToastUtil.getInstance()._short(context, "成功保存到 ".concat(path).concat(name));
                                    }
                                    picCallBack.onDownLoadSuccess(path.concat(name));
                                    new SingleMediaScanner(context, path.concat(name), new SingleMediaScanner.ScanListener() {
                                        @Override
                                        public void onScanFinish() {
                                            // scanning...
                                            String a = "aaaa";
                                        }
                                    });
                                } else {
                                    ToastUtil.getInstance()._short(context, "保存失败");
                                }
                            }
                        });



    }


    public interface PicCallBack{
       void onDownLoadSuccess(String str);
    }
}