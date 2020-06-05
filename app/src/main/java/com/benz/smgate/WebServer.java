package com.benz.smgate;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.telephony.SmsManager;

import fi.iki.elonen.NanoHTTPD;

class WebServer extends NanoHTTPD {

    private Context context;
    private BroadcastReceiver sentReceiver;
    private boolean useIntent = false;
    private final StringBuilder log = new StringBuilder();
    private final SmsManager smsManager = SmsManager.getDefault();
    private static final String SENT = "SMS_SENT";


    WebServer(String ip, int port, boolean useIntent) {
        super(ip, port);
        this.useIntent = useIntent;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void start() throws IOException {
        super.start();

        sentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int code = getResultCode();
                switch (code) {
                    case Activity.RESULT_OK:
                        PrintLog("[" + intent.getStringExtra("num") + "] Message sent !");
                        break;
                    default:
                        PrintLog("[" + intent.getStringExtra("num") + "] Failed with code: " + code);
                        break;
                }
            }
        };
        context.registerReceiver(sentReceiver, new IntentFilter(SENT));
    }

    @Override
    public void stop() {
        super.stop();
        context.unregisterReceiver(sentReceiver);
    }

    @Override
    public Response serve(IHTTPSession session) {

        PrintLog(session.getHeaders());
        PrintLog("Got new client: " + session.getRemoteIpAddress());

        HashMap<String, String> files = new HashMap<>();
        String content = "<html><style> input { width: 100%; padding: 10px; } div { padding: 10px; } button { padding: 10px; }</style><script> document.addEventListener(\"DOMContentLoaded\", function (event) { document.getElementById(\"send\").addEventListener(\"click\", function (event) { var amt = Number.parseInt(document.getElementById(\"txtAmt\").value); var nums = []; for (var i = 0; i < amt; i++) { nums.push(document.getElementById(\"txtNum\").value); } var xhr = new XMLHttpRequest(); xhr.open(\"POST\", \"\", true); xhr.setRequestHeader(\"Content-Type\", \"application/json;charset=UTF-8\"); xhr.send(JSON.stringify({ \"phones\": nums, \"message\": document.getElementById(\"txtMsg\").value })); }); });</script><body> <p> SMGate server running ... <p> <div> <input id=\"txtNum\" placeholder=\"Phone number ..\" /> </div> <div> <input id=\"txtMsg\" placeholder=\"Message ..\" /> </div> <div> <input id=\"txtAmt\" placeholder=\"A number of messages to send ..\" value=\"1\" /> </div> <div> <button id=\"send\">Send</button> </div></body></html>";
        Method method = session.getMethod();
        if (Method.POST.equals(method)) {
            try {
                session.parseBody(files);
                if (!files.isEmpty()) {
                    content = "OK";
                    String postDataString = files.get("postData");
                    JSONObject postData = new JSONObject(postDataString);
                    String message = postData.getString("message");
                    JSONArray phones = postData.getJSONArray("phones");

                    PrintLog(postDataString);
                    for (int i = 0; i < phones.length(); i++) {
                        String phone = phones.getString(i);
                        PrintLog("[*] Sending: " + phone + " , " + message);
                        sendSms(phone, message);
                    }
                }
            } catch (IOException ioe) {
                content = ioe.getMessage();
            } catch (ResponseException re) {
                content = re.getStatus() + re.getMessage();
            } catch (JSONException e) {
                content = e.getMessage();
            }
        }
        return newFixedLengthResponse(WrapInto("SMGate", content));
    }


    public void sendSms(String num, String message) {
        // @TODO: remove this, its dumb.
        if (useIntent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(context);
                Intent sendIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + num));
                sendIntent.putExtra(Intent.EXTRA_TEXT, message);

                if (defaultSmsPackageName != null) {
                    sendIntent.setPackage(defaultSmsPackageName);
                }
                context.startActivity(sendIntent);
            } else // For early versions, do what worked for you before.
            {
                Intent smsIntent = new Intent(android.content.Intent.ACTION_VIEW);
                smsIntent.setType("vnd.android-dir/mms-sms");
                smsIntent.putExtra("address", num);
                smsIntent.putExtra("sms_body", message);
                context.startActivity(smsIntent);
            }
            return;
        }

        Intent intent = new Intent(SENT);
        intent.putExtra("num", num);

        PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        smsManager.sendTextMessage(num, null, message, sentIntent, null);
    }

    private void PrintLog(Map<String, String> data) {
        for (String key : data.keySet()) {
            PrintLog(key + ":" + data.get(key));
        }
    }

    private void PrintLog(final String data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                TextView textMainLog = ((Activity) context).findViewById(R.id.textMainLog);
                textMainLog.post(new Runnable() {
                    @Override
                    public void run() {
                        TextView textMainLog = ((Activity) context).findViewById(R.id.textMainLog);
                        textMainLog.setText(log.append(data).append("\n").toString());

                        int scrollAmount = textMainLog.getLayout().getLineTop(textMainLog.getLineCount()) - textMainLog.getHeight();
                        if (scrollAmount > 0)
                            textMainLog.scrollTo(0, scrollAmount);
                        else
                            textMainLog.scrollTo(0, 0);
                    }
                });
            }
        }).start();

    }

    private String WrapInto(String head, String content) {
        return "<html><body><h1>" + head + "</h1><p>" + content + "</p></body></html>";
    }
}
