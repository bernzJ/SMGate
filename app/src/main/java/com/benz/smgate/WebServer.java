package com.benz.smgate;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.telephony.SmsManager;

import fi.iki.elonen.NanoHTTPD;

class WebServer extends NanoHTTPD {

    private Context context;
    private final StringBuilder log = new StringBuilder();
    private static final String SENT = "SMS_SENT";
    private final SmsManager smsManager = SmsManager.getDefault();
    private BroadcastReceiver sentReceiver;

    WebServer(String ip, int port) {
        super(ip, port);
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
        String content = "SMGate server running ...";
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

    private void PrintLog(String data) {
        TextView textMainLog = ((Activity) context).findViewById(R.id.textMainLog);
        textMainLog.setText(log.append(data).append("\n").toString());

        int scrollAmount = textMainLog.getLayout().getLineTop(textMainLog.getLineCount()) - textMainLog.getHeight();
        if (scrollAmount > 0)
            textMainLog.scrollTo(0, scrollAmount);
        else
            textMainLog.scrollTo(0, 0);
    }

    private String WrapInto(String head, String content) {
        return "<html><body><h1>" + head + "</h1><p>" + content + "</p></body></html>";
    }
}
