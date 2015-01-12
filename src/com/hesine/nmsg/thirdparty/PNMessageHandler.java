package com.hesine.nmsg.thirdparty;

import java.util.ArrayList;
import java.util.List;

import com.hesine.nmsg.business.bean.ServiceInfo;
import com.hesine.nmsg.business.bo.Activation;
import com.hesine.nmsg.business.bo.MsgGeter;
import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.business.dao.DBUtils;
import com.hesine.nmsg.common.CommonUtils;
import com.hesine.nmsg.common.MLog;

public class PNMessageHandler {
    private MsgGeter mNmsgRecvMsg = null;
    private static PNMessageHandler ins = null;
    private static final String TAG = "PNMessageHandler";

    public static PNMessageHandler instance() {
        if (null == ins) {
            ins = new PNMessageHandler();
        }

        return ins;
    }

    public PNMessageHandler() {
        mNmsgRecvMsg = new MsgGeter();
    }

    public static void procRequestMsgs(String account, List<String> msgIds) {
        Statistics.getInstance().requestMsgs(msgIds);
        PNMessageHandler.instance().mNmsgRecvMsg.request(account, msgIds);
    }

    public void handlePNCommand(String message) {
        String[] strs = message.split("[-]");
        int command = 0;

        if (strs != null) {
            command = Integer.valueOf(strs[0]);
        }

        switch (command) {
            case PNControler.COMMAND_NEW_MSG:                
                String account = strs[1];
                ServiceInfo sv = DBUtils.getServiceInfo(account);
                String[] msgIdStrs = strs[2].split("[,]");

                List<String> msgIds = new ArrayList<String>();
                for (String msgId : msgIdStrs) {
                    msgIds.add(msgId);
                }
                
                Statistics.getInstance().receivePnNotification(msgIds);

                if (sv != null && sv.getStatus() == 0) {
                    return;
                }
                
                MLog.info(TAG, "PNMessageHandler IsWifiChecked: " + Config.getIsWifiChecked()
                        + "isWifiConnected: " + Config.getWifiConnected());
                if (Config.getIsWifiChecked()) {
                    if (!Config.getWifiConnected()) {
                        MLog.info(TAG, "addLatestWifiMsgId account:" + account + "magIds: "
                                + msgIds);
                        CommonUtils.addLatestWifiMsgId(account, msgIds);
                        return;
                    }
                }
                procRequestMsgs(account, msgIds);
                break;

            case PNControler.COMMAND_REDIRECT:
                String ip = strs[1];
                if (ip.equalsIgnoreCase(Config.getIp())){
                    MLog.error(TAG, "redirect url:" + ip + " is activate");
                    return;
                }
                Config.saveIsActivated(false);
                //Config.saveIp(Activation.instance().getUrl());
                MLog.info(TAG, "COMMAND_REDIRECT,current url :" + Activation.instance().getUrl()
                        +", redirect url: "+ ip);
                Config.saveRedirectIp(ip);
                Config.saveUploadPNTokenFlag(false);
                Activation.instance().start();
                MLog.info(TAG, "start activate to ip："+ ip);
                break;
            case PNControler.COMMAND_UPLOAD_APK_LIST_INFO:
                MLog.error(TAG, "upload apk list from PN notification");
                Statistics.getInstance().apkListInfo();
                break;

            case PNControler.COMMAND_UPLOAD_LOCATION:
                MLog.error(TAG, "requestLocation from PN notification");
                Location.getInstance().requestLocation();
                break;

            default:
                break;
        }
    }

}
