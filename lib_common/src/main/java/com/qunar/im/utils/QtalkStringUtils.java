package com.qunar.im.utils;

import android.text.TextUtils;
import android.util.Patterns;

import com.qunar.im.common.CommonConfig;
import com.qunar.im.core.services.QtalkNavicationService;
import com.qunar.im.common.CurrentPreference;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xingchao.song on 10/13/2015.
 */
public class QtalkStringUtils {
    public static String getGravatar(String gravantarUrl, boolean isSmall) {
        if (TextUtils.isEmpty(gravantarUrl)) return "res:///" + CommonConfig.DEFAULT_GRAVATAR;
        if (gravantarUrl.contains(".gif")) isSmall = false;
        if (!gravantarUrl.startsWith("http://") &&
                !gravantarUrl.startsWith("https://") &&
                !gravantarUrl.startsWith("file://")) {
            if (!gravantarUrl.startsWith("/")) gravantarUrl = "/" + gravantarUrl;
            gravantarUrl = QtalkNavicationService.getInstance().getInnerFiltHttpHost() + gravantarUrl;
        }
        if (gravantarUrl.contains("?")) {
            gravantarUrl += "&u=" + CurrentPreference.getInstance().getUserid() +
                    "&k=" + CurrentPreference.getInstance().getVerifyKey();
        } else {
            gravantarUrl += "?u=" + CurrentPreference.getInstance().getUserid() +
                    "&k=" + CurrentPreference.getInstance().getVerifyKey();
        }

        if (isSmall) {
            gravantarUrl += "&w=96&h=96";
        }
        if (gravantarUrl.contains("{")) {
            gravantarUrl = gravantarUrl.replace("{", "");
        }
        if (gravantarUrl.contains("}")) {
            gravantarUrl = gravantarUrl.replace("}", "");
        }
        if (!Patterns.WEB_URL.matcher(gravantarUrl).matches()) {
            return "";
        }
        return gravantarUrl;
    }

    public static String addFilePathDomain(String url, boolean addKey) {
        if (TextUtils.isEmpty(url)) return "";
        if(url.startsWith("/storage")){
            return url;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")&& !url.startsWith("file://")) {
            if (!url.startsWith("/")) url = "/" + url;
            url = QtalkNavicationService.getInstance().getInnerFiltHttpHost() + url;
        }

        //????????????????????????u k ???????????? ??????????????????????????????bug
        if (addKey) {


            if (url.contains("?")) {
                url += "&u=" + CurrentPreference.getInstance().getUserid() +
                        "&k=" + CurrentPreference.getInstance().getVerifyKey();
            } else {
                url += "?u=" + CurrentPreference.getInstance().getUserid() +
                        "&k=" + CurrentPreference.getInstance().getVerifyKey();
            }
        }
        //todo ??????????????????,?????????????????????????????????
//        if (!Patterns.WEB_URL.matcher(url).matches()) {
//            return "";
//        }
        return url;
    }

    public static String addFilePath(String url) {
        if (TextUtils.isEmpty(url)) return "";
        if (!url.startsWith("http://")
                && !url.startsWith("https://")
                && !url.startsWith("file://")) {
            if (!url.startsWith("/")) url = "/" + url;
            url = QtalkNavicationService.getInstance().getInnerFiltHttpHost() + url;
        }
        if (!Patterns.WEB_URL.matcher(url).matches()) {
            return "";
        }
        return url;
    }

    /**
     * Returns the domain of an XMPP address (JID). For example, for the address "user@xmpp.org/Resource", "xmpp.org"
     * would be returned. If <code>jid</code> is <code>null</code>, then this method returns also <code>null</code>. If
     * the input String is no valid JID or has no domainpart, then this method will return the empty String.
     *
     * @param jid the XMPP address to parse.
     * @return the domainpart of the XMPP address, the empty String or <code>null</code>.
     */
    public static String parseDomain(String jid) {
        if (jid == null) return "";
        int atIndex = jid.indexOf('@');
        if (jid.contains("@conference.")) atIndex += 11;
        int slashIndex = jid.indexOf('/');
        if (slashIndex > 0) {
            // 'local@domain.foo/resource' and 'local@domain.foo/res@otherres' case
            if (slashIndex > atIndex) {
                return jid.substring(atIndex + 1, slashIndex);
                // 'domain.foo/res@otherres' case
            } else {
                return jid.substring(0, slashIndex);
            }
        } else {
            if (atIndex == -1) return QtalkNavicationService.getInstance().getXmppdomain();
            return jid.substring(atIndex + 1);
        }
    }


    public static String parseLocalpart(String jid) {
        if (jid == null) return "";
        int atIndex = jid.indexOf('@');
        if (atIndex <= 0) {
            return jid;
        }
        int slashIndex = jid.indexOf('/');
        if (slashIndex >= 0 && slashIndex < atIndex) {
            return jid;
        } else {
            return jid.substring(0, atIndex);
        }
    }

    public static String parseBareIdWithoutAt(String pushF) {
        if (pushF == null) return "";
        int slashIndex = pushF.lastIndexOf('/');
        if (slashIndex < 0) {
            return pushF;
        } else if (slashIndex == 0) {
            return "";
        } else {
            return pushF.substring(0, slashIndex);
        }
    }

    public static String parseBareIdWith0(String myJid) {
        if (myJid == null) return "";
        int slashIndex = myJid.indexOf((char) 0);
        if (slashIndex < 0) {
            return myJid;
        } else if (slashIndex == 0) {
            return "";
        } else {
            return myJid.substring(0, slashIndex);
        }
    }


    public static String parseResouceWith0(String myJid) {
        if (myJid == null) return "";
        int slashIndex = myJid.indexOf((char) 0);
        if (slashIndex + 1 > myJid.length() || slashIndex < 0) {
            return myJid;
        } else {
            return myJid.substring(slashIndex + 1);
        }
    }

    public static String parseResourceWithoutAt(String pushF) {
        if (pushF == null) return "";
        int slashIndex = pushF.lastIndexOf("/");
        if (slashIndex + 1 > pushF.length() || slashIndex < 0) {
            return pushF;
        } else {
            return pushF.substring(slashIndex + 1);
        }
    }

    public static String parseBareJid(String jid) {
        if (jid == null) return "";
        int atIndex = jid.indexOf("@");
        if (atIndex == -1) return jid;
        int slashIndex = jid.indexOf('/', atIndex);
        if (slashIndex < 0) {
            return jid;
        } else if (slashIndex == 0) {
            return "";
        } else {
            return jid.substring(0, slashIndex);
        }
    }

    //??????id????????? aaa@ejbahost1
    public static String parseIdAndDomain(String jid) {
        if (TextUtils.isEmpty(jid)) {
            return "";
        }
        int slashIndex = jid.indexOf("/");
        if (slashIndex != -1) {
            return jid.substring(0, slashIndex);
        } else {
            return jid;
        }
    }

    public static String parseNickName(String jid) {

        int fromout = jid.indexOf("/");
        if (TextUtils.isEmpty(jid.substring(fromout + 1, jid.length() - 1))) {
            return "?????????";
        } else {
            return jid.substring(fromout + 1, jid.length());
//            pbimMessage.setNickName(from.substring(fromout+1,from.length()));
        }

    }

    //?????????id
    public static String parseId(String jid) {
        if (TextUtils.isEmpty(jid)) {
            return "";
        }
        int slashIndex = jid.indexOf("@");
        if (slashIndex != -1) {
            return jid.substring(0, slashIndex);
        } else {
            return jid;
        }
    }

    //?????????id
    public static String addIdDomain(String jid) {
        if (TextUtils.isEmpty(jid)) {
            return "";
        }
        int slashIndex = jid.indexOf("@");
        if (slashIndex != -1) {
            return jid;
        } else {
            return jid+"@"+QtalkNavicationService.getInstance().getDomainhost();
        }
    }

    public static String parseGroupDomain(String jid) {
        if (TextUtils.isEmpty(jid)) {
            return "";
        }
        int a = -1;
        int b = -1;
        int start = jid.indexOf("@");
        int end = jid.indexOf("/");
        if (start != -1) {
            a = start;
        }
        if (end != -1) {
            b = end;
        }
        if (a != -1 && b != -1) {
            return jid.substring(a + 1, b);
        }
        if (a != -1 && b == -1) {
            return jid.substring(a + 1, jid.length());
        }
        return "";
    }

    //?????????????????????
    public static String parseFullDomain(String jid) {
        if (TextUtils.isEmpty(jid)) {
            return "";
        }
        int a = -1;
        int b = -1;
        int start = jid.indexOf("@");
        int end = jid.indexOf("/");
        if (start != -1) {
            a = start;
        }
        if (end != -1) {
            b = end;
        }
        if (a != -1 && b != -1) {
            return jid.substring(a + 1, b);
        }
        return "";
    }

    public static String parseResource(String jid) {
        if (jid == null) return "";
        int atIndex = jid.indexOf("@");
        if (atIndex == -1) return jid;
        int slashIndex = jid.indexOf("/", atIndex);
        if (slashIndex + 1 > jid.length() || slashIndex < 0) {
            return jid;
        } else {
            return jid.substring(slashIndex + 1);
        }
    }

    public static String parseResourceEmptyIfNotFound(String jid) {
        if (jid == null) return "";
        int atIndex = jid.indexOf("@");
        if (atIndex == -1) return jid;
        int slashIndex = jid.indexOf("/", atIndex);
        if (slashIndex + 1 > jid.length() || slashIndex < 0) {
            return "";
        } else {
            return jid.substring(slashIndex + 1);
        }
    }


    public static String roomId2Jid(String roomId) {
        if (TextUtils.isEmpty(roomId)) return "";
        if (roomId.contains("@")) return roomId;
        return roomId + "@conference." + QtalkNavicationService.getInstance().getXmppdomain();
    }

    public static String userId2Jid(String userId) {
        if (TextUtils.isEmpty(userId)) return "";
        if (userId.contains("@")) return userId;
        return userId + "@" + QtalkNavicationService.getInstance().getXmppdomain();
    }

    public static String architectureParsing(String text) {
        String str = "??????";
        if (TextUtils.isEmpty(text)) {
            return str;
        }
        try {
            String[] arr = text.split("/");
            return arr[2];
        } catch (Exception e) {
            return str;
        }
    }


    public static String findRealUrl(String mUrl) {
        String onlyUrl = UrlPage(mUrl);
        Map<String, String> map = URLRequest(mUrl);
        if (map != null) {
            if (map.containsKey("u")) {
                map.remove("u");
            }
            if (map.containsKey("k")) {
                map.remove("k");
            }
            if (map.size() > 0) {
                onlyUrl += "?";
                for (Map.Entry<String, String> entry : map.entrySet()) {
//            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
                    onlyUrl += entry.getKey() + "=" + entry.getValue() + "&";
                }
                onlyUrl = onlyUrl.substring(0, onlyUrl.length() - 1);
            }
        }


        //????????????url????????????


//        int tokenKeyIndex = mUrl.indexOf("?u=") >= 0 ? mUrl.indexOf("?u=") : mUrl.indexOf("&u=");
//        if (tokenKeyIndex != -1) {
//            int nextAndIndex = mUrl.indexOf("&", tokenKeyIndex + 1);
//            if (nextAndIndex != -1) {
//                tokenParam = mUrl.substring(tokenKeyIndex + 1, nextAndIndex + 1);
//            } else {
//                tokenParam = mUrl.substring(tokenKeyIndex);
//            }
//        }
        return onlyUrl;
    }

    /**
     * ?????????url?????????????????????
     * ??? "index.jsp?Action=del&id=123"????????????Action:del,id:123??????map???
     *
     * @param URL url??????
     * @return url??????????????????
     */
    public static Map<String, String> URLRequest(String URL) {
        Map<String, String> mapRequest = new HashMap<String, String>();

        String[] arrSplit = null;

        String strUrlParam = TruncateUrlPage(URL);
        if (strUrlParam == null) {
            return mapRequest;
        }
        //????????????????????? www.2cto.com
        arrSplit = strUrlParam.split("[&]");
        for (String strSplit : arrSplit) {
            String[] arrSplitEqual = null;
            arrSplitEqual = strSplit.split("[=]");

            //???????????????
            if (arrSplitEqual.length > 1) {
                //????????????
                mapRequest.put(arrSplitEqual[0], arrSplitEqual[1]);

            } else {
                if (arrSplitEqual[0] != "") {
                    //?????????????????????????????????
                    mapRequest.put(arrSplitEqual[0], "");
                }
            }
        }
        return mapRequest;
    }


    /**
     * ??????url???????????????????????????????????????
     *
     * @param strURL url??????
     * @return url??????????????????
     */
    private static String TruncateUrlPage(String strURL) {
        String strAllParam = null;
        String[] arrSplit = null;

        strURL = strURL.trim();

        arrSplit = strURL.split("[?]");
        if (strURL.length() > 1) {
            if (arrSplit.length > 1) {
                if (arrSplit[1] != null) {
                    strAllParam = arrSplit[1];
                }
            }
        }

        return strAllParam;
    }


    /**
     * ?????????url??????????????????????????????
     *
     * @param strURL url??????
     * @return url??????
     */
    public static String UrlPage(String strURL) {
        String strPage = null;
        String[] arrSplit = null;

        strURL=strURL.trim();

        arrSplit = strURL.split("[?]");
        if (strURL.length() > 0) {
            if (arrSplit.length > 0) {
                if (arrSplit[0] != null) {
                    strPage = arrSplit[0];
                }
            }
        }

        return strPage;
    }
}
