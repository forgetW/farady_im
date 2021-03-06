package com.qunar.im.ui.presenter.impl;

import android.content.Context;
import android.text.TextUtils;

import com.qunar.im.base.util.IMUserDefaults;
import com.qunar.im.common.CommonConfig;
import com.qunar.im.common.CurrentPreference;
import com.qunar.im.core.services.QtalkNavicationService;
import com.qunar.im.ui.util.easyphoto.easyphotos.models.album.entity.Photo;
import com.orhanobut.logger.Logger;
import com.qunar.im.base.common.BackgroundExecutor;
import com.qunar.im.base.common.CommonUploader;
import com.qunar.im.base.jsonbean.UploadImageResult;
import com.qunar.im.base.jsonbean.VideoMessageResult;
import com.qunar.im.base.module.AnonymousData;
import com.qunar.im.base.module.AtData;
import com.qunar.im.base.module.ImageItem;
import com.qunar.im.base.module.ImageItemWorkWorldItem;
import com.qunar.im.base.module.MultiItemEntity;
import com.qunar.im.base.module.ReleaseCircleNoChangeItemDate;
import com.qunar.im.base.module.ReleaseContentData;
import com.qunar.im.base.module.ReleaseDataRequest;
import com.qunar.im.base.module.VideoDataResponse;
import com.qunar.im.base.module.WorkWorldResponse;
import com.qunar.im.base.protocol.ProgressRequestListener;
import com.qunar.im.base.util.DataUtils;
import com.qunar.im.base.util.MessageUtils;
import com.qunar.im.core.services.FileProgressRequestBody;
import com.qunar.im.ui.presenter.ReleaseCirclePresenter;
import com.qunar.im.ui.presenter.views.ReleaseCircleView;
import com.qunar.im.base.protocol.ProtocolCallback;
import com.qunar.im.base.structs.MessageType;
import com.qunar.im.base.structs.WorkWorldItemState;
import com.qunar.im.base.transit.IUploadRequestComplete;
import com.qunar.im.base.transit.UploadImageRequest;
import com.qunar.im.base.util.ChatTextHelper;
import com.qunar.im.base.util.JsonUtils;
import com.qunar.im.utils.HttpUtil;
import com.qunar.im.utils.QtalkStringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.qunar.im.base.structs.MessageType.link;

public class ReleaseCircleManagerPresenter implements ReleaseCirclePresenter {

    private ReleaseCircleView mView;
    private List uploadList = new ArrayList<>();
    private String uuid;
    private AnonymousData mAnonymousData;
    private ReleaseContentData releaseContentData;
    private Context mContext;

    public static final int REAL_NAME = 0;
    public static final int ANONYMOUS_NAME = 1;

    public ReleaseCircleManagerPresenter(Context context) {
        this.mContext = context;
        uuid = "0-" + UUID.randomUUID().toString().replace("-", "");
    }


    public List deepcopy(List src) throws IOException,
            ClassNotFoundException {
        ByteArrayOutputStream byteout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteout);
        out.writeObject(src);
        ByteArrayInputStream bytein = new ByteArrayInputStream(byteout
                .toByteArray());
        ObjectInputStream in = new ObjectInputStream(bytein);
        List dest = (List) in.readObject();
        return dest;

    }

    @Override
    public boolean release() {
        List<MultiItemEntity> list = new ArrayList<>();
        Photo video = null;
        try {
            list = deepcopy(mView.getUpdateImageList());
            video = mView.getUpdateVideo();

        } catch (Exception e) {

        }

        if (list.get(list.size() - 1) instanceof ReleaseCircleNoChangeItemDate) {
            list.remove(list.size() - 1);
        }
        String content = mView.getContent().trim();
        if (list.size() == 0 && TextUtils.isEmpty(content) && mView.getEntity() == null && video == null) {
            mView.showToast("??????????????????????????????/??????");
            //???????????????????????????
            return false;
        }
        if (!mView.isCheck()) {
            mView.showToast("??????????????????1000??????????????????");
            return false;
        }

        releaseContentData = new ReleaseContentData();

        mView.showProgress();
        if (list.size() > 0) {
            uploadImgAndRelease(list);
        } else if (video != null) {
            uploadAndRelease(video);
        } else {

            startRelease();
        }
        return true;

    }

    private void uploadAndRelease(final Photo file) {

        boolean needTran = false;
        String time = IMUserDefaults.getStandardUserDefaults().getStringValue(CommonConfig.globalContext,
                CurrentPreference.getInstance().getUserid()
                        + QtalkNavicationService.getInstance().getXmppdomain()
                        + CommonConfig.isDebug
                        + "videoTime");
        if(TextUtils.isEmpty(time)){
            time = (16*1000)+"";
        }
        if (file.duration > Long.parseLong(time)) {
            needTran = false;
        } else {
            needTran = true;
        }

        boolean finalNeedTran = needTran;
        HttpUtil.videoCheckAndUpload(file.path, needTran, new FileProgressRequestBody.ProgressRequestListener() {
            @Override
            public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
//                Logger.i("??????????????????:bytesWritten:"+bytesWritten+",contentLength:"+contentLength+",done:"+done);
                Logger.i("??????????????????:done:" + done);
            }
        }, new ProtocolCallback.UnitCallback<VideoDataResponse>() {
            @Override
            public void onCompleted(VideoDataResponse videoDataResponse) {


                if (videoDataResponse != null && videoDataResponse.getData().isReady()) {
                    final VideoMessageResult videoInfo = MessageUtils.getBasicVideoInfo(file.path);

                    videoInfo.LocalVideoOutPath = file.path;
                    videoInfo.ThumbUrl = videoDataResponse.getData().getFirstThumbUrl();
                    videoInfo.ThumbName = videoDataResponse.getData().getFirstThumb();
                    if (finalNeedTran) {
                        videoInfo.newVideo = true;
                        videoInfo.FileName = videoDataResponse.getData().getTransFilename();

                        videoInfo.Width = videoDataResponse.getData().getTransFileInfo().getWidth();
                        videoInfo.Height = videoDataResponse.getData().getTransFileInfo().getHeight();
                        videoInfo.FileUrl = videoDataResponse.getData().getTransUrl();
                        videoInfo.Duration = String.valueOf(videoDataResponse.getData().getTransFileInfo().getDuration());
                        videoInfo.FileSize = String.valueOf(videoDataResponse.getData().getTransFileInfo().getVideoSize());
                        videoInfo.fileMd5 = videoDataResponse.getData().getTransFileMd5();
                    } else {
                        videoInfo.newVideo = false;
                        videoInfo.FileName = videoDataResponse.getData().getOriginFilename();
                        videoInfo.Width = videoDataResponse.getData().getOriginFileInfo().getWidth();
                        videoInfo.Height = videoDataResponse.getData().getOriginFileInfo().getHeight();
                        videoInfo.FileUrl = videoDataResponse.getData().getOriginUrl();
                        videoInfo.Duration = String.valueOf(videoDataResponse.getData().getOriginFileInfo().getDuration());
                        videoInfo.FileSize = String.valueOf(videoDataResponse.getData().getOriginFileInfo().getVideoSize());

                        videoInfo.fileMd5 = videoDataResponse.getData().getOriginFileMd5();
                    }
//                    videoInfo.newVideo = true;
                    Logger.i("?????????????????????????????? :" + JsonUtils.getGson().toJson(videoDataResponse));

                    String jsonVideo = JsonUtils.getGson().toJson(videoInfo);
                    releaseContentData.setVideoContent(videoInfo);
                    releaseContentData.setType(MessageType.video);
                    releaseContentData.setContent("?????????????????????!");

                    startRelease();

//                    message.setBody(jsonVideo);
//                    message.setExt(jsonVideo);
//                    message.setMsgType(ProtoMessageOuterClass.MessageType.MessageTypeSmallVideo_VALUE);
//
//                    if (snapStatus) {
//                        HttpUtil.handleSnapMessage(message);
//                    }
//                    HttpUtil.addEncryptMessageInfo(message.getToID(), message, ProtoMessageOuterClass.MessageType.MessageTypeSmallVideo_VALUE);
//                    if (isFromChatRoom) {
//                        ConnectionUtil.getInstance().sendGroupTextOrEmojiMessage(message);
//                    } else {
//                        ConnectionUtil.getInstance().sendTextOrEmojiMessage(message);
//                    }
                } else {
                    mView.dismissProgress();
                    mView.showToast("????????????,?????????");
                    Logger.i("??????????????????????????????:   " + JsonUtils.getGson().toJson(videoDataResponse));
//                    message.setMessageState(MessageStatus.LOCAL_STATUS_FAILED);
//                    IMDatabaseManager.getInstance().UpdateChatStateMessage(message, false);
//                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Send_Failed, message.getMessageId());
//                    InternDatas.sendingLine.remove(message.getId());
                }
                Logger.i("??????????????????:" + JsonUtils.getGson().toJson(videoDataResponse));
            }

            @Override
            public void onFailure(String errMsg) {
                mView.dismissProgress();
                mView.showToast("????????????,?????????");
                Logger.i("??????????????????:" + errMsg);
            }
        });
    }

//    private void uploadAndRelease( final Photo file) {
//        final UploadImageRequest request = new UploadImageRequest();
//        final String firstFramPath = FileUtils.getFristFrameOfFile(file.path);
//
//        if (!TextUtils.isEmpty(firstFramPath)) {
//            //??????video??????????????????????????????????????????????????????
//            BitmapFactory.Options option = ImageUtils.getImageSize(firstFramPath);
//            final int width = option.outWidth;
//            final int height = option.outHeight;
//
//            final String fileName = file.path.substring(file.path.lastIndexOf("/") + 1);
//            final VideoMessageResult videoInfo = MessageUtils.getBasicVideoInfo(file.path);
//            videoInfo.FileName = fileName;
//            videoInfo.ThumbUrl = firstFramPath;
//            videoInfo.FileUrl = file.path;
//            videoInfo.Width = String.valueOf(width);
//            videoInfo.Height = String.valueOf(height);
//
////            message.setBody(firstFramPath);
////            message.setExt(JsonUtils.getGson().toJson(videoInfo));
////            message.setMsgType(ProtoMessageOuterClass.MessageType.MessageTypeSmallVideo_VALUE);
////            if (snapStatus) {
////                HttpUtil.handleSnapMessage(message);
////            }
////            HttpUtil.addEncryptMessageInfo(message.getToID(), message, ProtoMessageOuterClass.MessageType.MessageTypeSmallVideo_VALUE);
//            //???????????????????????????
////            chatView.setNewMsg2DialogueRegion(message);
////            curMsgNum++;
//
////            if (snapStatus) {
////                HttpUtil.handleSnapMessage(message);
////            }
//
////            IMDatabaseManager.getInstance().InsertChatMessage(message, false);
////            IMDatabaseManager.getInstance().InsertIMSessionList(message, false);
//
//            request.filePath = firstFramPath;
//            request.FileType = UploadImageRequest.IMAGE;
////            request.id = message.getId();
//            request.requestComplete = new IUploadRequestComplete() {
//                @Override
//                public void onRequestComplete(String id, UploadImageResult result) {
//                    if (result != null && !TextUtils.isEmpty(result.httpUrl)) {
//                        Logger.i("??????????????????????????????  msg url = " + result.httpUrl);
//                        File targetFile = MyDiskCache.getFile(QtalkStringUtils.addFilePathDomain(result.httpUrl));
//                        File sourceFile = new File(firstFramPath);
//                        FileUtils.copy(sourceFile, targetFile);
//                        sendVideoFile(file.path, result.httpUrl,file.width,file.height);
//                    } else {
//                        mView.dismissProgress();
//                        mView.showToast("????????????,?????????");
//                        Logger.i("?????????????????????????????????  msg id = " + id);
////                        message.setMessageState(MessageStatus.LOCAL_STATUS_FAILED);
////                        IMDatabaseManager.getInstance().UpdateChatStateMessage(message, false);
////                        IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Send_Failed, message.getMessageId());
//                    }
//                }
//
//                @Override
//                public void onError(String msg) {
//                    mView.dismissProgress();
//                    mView.showToast("????????????,?????????");
//                    Logger.i("?????????????????????????????????  msg url = " + msg);
////                    message.setMessageState(MessageStatus.LOCAL_STATUS_FAILED);
////                    IMDatabaseManager.getInstance().UpdateChatStateMessage(message, false);
////                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Send_Failed, message.getMessageId());
////                    InternDatas.sendingLine.remove(message.getId());
////                    updateDbOnSuccess(message, false);
//                }
//            };
//            CommonUploader.getInstance().setUploadImageRequest(request);
//        } else {
//            File f = new File(file.path);
//            if (f.exists()) {
//                sendVideoFile(file.path,"",file.width,file.height);
//            }
//        }
//    }

    /**
     * ???????????????????????????
     */
    protected void sendVideoFile(final String sourceFilePath, final String frameUrl, int videoW, int videoH) {
        String uuid = UUID.randomUUID().toString();

        final String fileName = sourceFilePath.substring(sourceFilePath.lastIndexOf("/") + 1);
        final VideoMessageResult videoInfo = MessageUtils.getBasicVideoInfo(sourceFilePath);
        videoInfo.FileName = fileName;
        videoInfo.ThumbUrl = frameUrl;
        videoInfo.FileUrl = sourceFilePath;
        videoInfo.Height = String.valueOf(videoH);
        videoInfo.Width = String.valueOf(videoW);

        final UploadImageRequest request = new UploadImageRequest();
        request.filePath = sourceFilePath;
        request.FileType = UploadImageRequest.FILE;
        request.id = uuid;
        request.progressRequestListener = new ProgressRequestListener() {
            @Override
            public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
//                callback.updataProgress((int) (bytesWritten / contentLength), done);
//                chatView.updateUploadProgress(message, (int) (bytesWritten / contentLength), done);
            }
        };
        Logger.i("????????????????????????:" + JsonUtils.getGson().toJson(request));
        request.requestComplete = new IUploadRequestComplete() {
            @Override
            public void onRequestComplete(String id, UploadImageResult result) {
                mView.dismissProgress();
                if (result != null && !TextUtils.isEmpty(result.httpUrl)) {
                    Logger.i("??????????????????????????????  msg url = " + result.httpUrl);
                    videoInfo.FileUrl = result.httpUrl;

                    String jsonVideo = JsonUtils.getGson().toJson(videoInfo);
                    releaseContentData.setVideoContent(videoInfo);
                    releaseContentData.setType(MessageType.video);
                    releaseContentData.setContent("?????????????????????!");

                    startRelease();

//                    message.setBody(jsonVideo);
//                    message.setExt(jsonVideo);
//                    message.setMsgType(ProtoMessageOuterClass.MessageType.MessageTypeSmallVideo_VALUE);
//
//                    if (snapStatus) {
//                        HttpUtil.handleSnapMessage(message);
//                    }
//                    HttpUtil.addEncryptMessageInfo(message.getToID(), message, ProtoMessageOuterClass.MessageType.MessageTypeSmallVideo_VALUE);
//                    if (isFromChatRoom) {
//                        ConnectionUtil.getInstance().sendGroupTextOrEmojiMessage(message);
//                    } else {
//                        ConnectionUtil.getInstance().sendTextOrEmojiMessage(message);
//                    }
                } else {

                    mView.showToast("????????????,?????????");
                    Logger.i("??????????????????????????????:   " + JsonUtils.getGson().toJson(result));
//                    message.setMessageState(MessageStatus.LOCAL_STATUS_FAILED);
//                    IMDatabaseManager.getInstance().UpdateChatStateMessage(message, false);
//                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Send_Failed, message.getMessageId());
//                    InternDatas.sendingLine.remove(message.getId());
                }
            }

            @Override
            public void onError(String msg) {
                mView.dismissProgress();
                mView.showToast("????????????,?????????");
                Logger.i("??????????????????????????????  msg url = " + msg);
//                message.setMessageState(MessageStatus.LOCAL_STATUS_FAILED);
//                IMDatabaseManager.getInstance().UpdateChatStateMessage(message, false);
//                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Send_Failed, message.getMessageId());
//
//                chatView.setNewMsg2DialogueRegion(message);
//                curMsgNum++;
//
//                InternDatas.sendingLine.remove(message.getId());
            }
        };
        CommonUploader.getInstance().setUploadImageRequest(request);
    }


    private void uploadImgAndRelease(List<MultiItemEntity> list) {
        final boolean[] isOk = {true};
        uploadList = Collections.synchronizedList(new ArrayList<ImageItemWorkWorldItem>());
        for (int i = 0; i < list.size(); i++) {
            uploadList.add(new ImageItemWorkWorldItem());
        }
        //todo ??????????????????
        final List<MultiItemEntity> finalList = list;
        BackgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final int[] current = {0};

                for (int i = 0; i < finalList.size(); i++) {
                    final MultiItemEntity item = finalList.get(i);
                    UploadImageRequest request = new UploadImageRequest();
                    request.FileType = UploadImageRequest.IMAGE;
                    request.filePath = ((ImageItem) item).path;
                    final int finalI = i;
                    request.requestComplete = new IUploadRequestComplete() {
                        @Override
                        public void onRequestComplete(String id, final UploadImageResult result) {

                            try {

//                                String reJson = Protocol.parseStream(response);
                                ImageItemWorkWorldItem stringJson = new ImageItemWorkWorldItem();
//                                        JsonUtils.getGson().fromJson(reJson, ImageItemWorkWorldItem.class);
                                if (result != null && !TextUtils.isEmpty(result.httpUrl)) {
                                    String url = QtalkStringUtils.addFilePathDomain(result.httpUrl, false);

                                    stringJson.local = ((ImageItem) item).path;
                                    stringJson.data = url;
                                    Logger.i("??????????????????-??????:" + stringJson.data);
                                    uploadList.remove(finalI);
                                    uploadList.add(finalI, stringJson);

                                    synchronized (current) {
                                        current[0]++;
                                    }
                                    if (current[0] == finalList.size()) {
                                        checkRelease(isOk[0]);
                                    }
//                                    if (stringJson.ret) {
//
//
//                                    } else {
//
////                                                checkRelease(false);
//                                    }
//
                                } else {
                                    synchronized (current) {
                                        current[0]++;
                                        isOk[0] = false;
                                    }
                                    if (current[0] == finalList.size()) {
                                        checkRelease(isOk[0]);
                                    }
                                }
                            } catch (Exception e) {
//                                        LogUtil.e(TAG, "error", e);
                                Logger.i("????????????????????????-??????:" + e.getMessage());

                                Logger.i("????????????????????????:" + e.getMessage());
                            }


//                            String perUrl = Constants.Config.PERSISTENT_IMAGE + "?url=" + url;
//                            HttpUrlConnectionHandler.executeGet(perUrl, new HttpRequestCallback() {
//                                @Override
//                                public void onComplete(InputStream response) {
//                                    try {
//
//                                        String reJson = Protocol.parseStream(response);
//                                        ImageItemWorkWorldItem stringJson =
//                                                JsonUtils.getGson().fromJson(reJson, ImageItemWorkWorldItem.class);
//                                        stringJson.local = ((ImageItem) item).path;
//                                        Logger.i("??????????????????-??????:" + stringJson.data);
//                                        if (stringJson.ret) {
//                                            uploadList.remove(finalI);
//                                            uploadList.add(finalI,stringJson);
//
//                                            synchronized (current) {
//                                                current[0]++;
//                                            }
//                                            if (current[0] == finalList.size()) {
//                                                checkRelease(isOk[0]);
//                                            }
//
//                                        }else{
//                                            synchronized (current) {
//                                                current[0]++;
//                                                isOk[0] =false;
//                                            }
//                                            if (current[0] == finalList.size()) {
//                                                checkRelease(isOk[0]);
//                                            }
////                                                checkRelease(false);
//                                        }
////
//
//                                    } catch (Exception e) {
////                                        LogUtil.e(TAG, "error", e);
//                                        Logger.i("????????????????????????-??????:" + e.getMessage());
//
//                                        Logger.i("????????????????????????:" + e.getMessage());
//                                    }
//
//                                }
//
//                                @Override
//                                public void onFailure(Exception e) {
//                                    Logger.i("????????????????????????-??????:" + e.getMessage());
//
//                                    synchronized (current) {
//                                        current[0]++;
//                                        isOk[0]= false;
//                                    }
//                                    if (current[0] == finalList.size()) {
//                                        checkRelease(isOk[0]);
////                                        configItem.value = JsonUtils.getGson().toJson(finalRemoteItems);
////                                        HttpUtil.setRemoteConfig(configItems,null);
//                                    }
//                                }
//                            });
                        }

                        @Override
                        public void onError(String msg) {
                            Logger.i("????????????????????????-??????:" + msg);

                            synchronized (current) {
                                current[0]++;
                                isOk[0] = false;
                            }
                            if (current[0] == finalList.size()) {
                                checkRelease(isOk[0]);
//                                configItem.value = JsonUtils.getGson().toJson(finalRemoteItems);
//                                HttpUtil.setRemoteConfig(configItems,null);
                            }
                        }
                    };
                    CommonUploader.getInstance().setUploadImageRequest(request);
                }
//                    for (final MultiItemEntity item : finalList) {
//
//                    }
            }
        });
    }

    public void checkLink() {
        if (mView.getEntity() == null) {
            return;
        }
        releaseContentData.setLinkContent(mView.getEntity());
        releaseContentData.setType(link);
        releaseContentData.setContent("?????????????????????!");
    }

    public String getAtList() {
        Map<String, String> map = mView.getAtList();
        List<AtData> dataList = new ArrayList<>();
        AtData ad = new AtData();
        ad.setType(10001);
        List<AtData.DataBean> atList = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            AtData.DataBean atdb = new AtData.DataBean();
            atdb.setJid(entry.getKey());
            atdb.setText(entry.getValue().trim());
            atList.add(atdb);
        }
        ad.setData(atList);
        dataList.add(ad);

        String str = "";
        str = JsonUtils.getGson().toJson(dataList);
        return str;
    }

    public void startRelease() {


        String str = mView.getContent();
        String msg = ChatTextHelper.textToHTML(str);


        releaseContentData.setExContent(msg);
        if (TextUtils.isEmpty(releaseContentData.getContent())) {
            releaseContentData.setContent(str);
        }


        //?????????????????????????????????????????????
        checkLink();


        ReleaseDataRequest releaseDataRequest = new ReleaseDataRequest();


        releaseDataRequest.setUuid(uuid);
        releaseDataRequest.setAtList(getAtList());
        if (mView.getIdentityType() == REAL_NAME) {
            releaseDataRequest.setAnonymous(0);
            releaseDataRequest.setAnonymousName("");
            releaseDataRequest.setAnonymousPhoto("");
        } else if (mView.getIdentityType() == ANONYMOUS_NAME) {
            mAnonymousData = mView.getAnonymousData();
            if (mAnonymousData == null) {
                //?????????????????????????????? ????????????
                return;
            }

            DataUtils.getInstance(mContext).putPreferences("workworldAnonymous" + uuid, JsonUtils.getGson().toJson(mAnonymousData));

            releaseDataRequest.setAnonymous(1);
            releaseDataRequest.setAnonymousName(mAnonymousData.getData().getAnonymous());
            releaseDataRequest.setAnonymousPhoto(mAnonymousData.getData().getAnonymousPhoto());
        }
        releaseDataRequest.setContent(JsonUtils.getGson().toJson(releaseContentData));
        int postType = WorkWorldItemState.hot | WorkWorldItemState.top | WorkWorldItemState.normal;
        releaseDataRequest.setPostType(postType);
        HttpUtil.releaseWorkWorldV2(releaseDataRequest, new ProtocolCallback.UnitCallback<WorkWorldResponse>() {
            @Override
            public void onCompleted(WorkWorldResponse workWorldResponse) {
                mView.dismissProgress();

                if (workWorldResponse == null) {
                    mView.showToast("???????????????????????????");
                } else {

                    mView.closeActivitvAndResult(workWorldResponse.getData().getNewPost());
                }
            }

            @Override
            public void onFailure(String errMsg) {
                mView.dismissProgress();
                mView.closeActivitvAndResult(null);
//                mView.closeActivitv();
            }
        });
    }

    public void checkRelease(boolean isOk) {

        Logger.i("????????????:" + isOk + "--:" + JsonUtils.getGson().toJson(uploadList));
        if (isOk) {
            releaseContentData.setImgList(uploadList);
            releaseContentData.setType(MessageType.image);

            startRelease();
        } else {
            mView.dismissProgress();
            mView.showToast("????????????,?????????");
            //????????????????????? ?????????????????????????????????
        }
    }

    @Override
    public void setView(ReleaseCircleView view) {
        mView = view;

    }

    @Override
    public void getAnonymous() {
        if (mAnonymousData != null) {
            mView.setAnonymousData(mAnonymousData);
        } else {


            HttpUtil.getAnonymous(uuid, new ProtocolCallback.UnitCallback<AnonymousData>() {
                @Override
                public void onCompleted(AnonymousData anonymousData) {
                    mView.setAnonymousData(anonymousData);
                    mAnonymousData = anonymousData;
                }

                @Override
                public void onFailure(String errMsg) {

                }
            });
        }
    }

    @Override
    public String getUUID() {
        return uuid;
    }
}
