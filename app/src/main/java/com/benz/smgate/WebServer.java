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
import java.util.Random;

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
        String content = "<html><style>@import url(https://fonts.googleapis.com/css?family=Josefin+Sans:300,400);body{padding:0;background-color:#fff;-webkit-font-smoothing:antialiased;-webkit-backface-visibility:hidden}h1{font-size:48px;font-family:'Josefin Sans',sans-serif;font-weight:300;text-align:center;margin-bottom:60px}.form-row{display:block;border:2px solid #aaa;transform:scale(.75)}.form-row>:first-child{border-left:0}.form-row>*{box-sizing:border-box;width:33.33%}button,input,textarea{display:inline-block;padding:40px 50px;font-size:28px;font-family:'Josefin Sans',sans-serif;-webkit-font-smoothing:antialiased;color:#444;background:#fff;margin:0;border:0;border-color:#aaa;border-style:solid;border-width:0;border-left-width:2px;outline:0}input{display:inline-block;font-weight:400;position:relative;transition:all .25s}input:hover{-moz-box-shadow:inset 0 0 0 5px rgba(0,0,0,.05);box-shadow:inset 0 0 0 5px rgba(0,0,0,.05)}input:focus{background-color:#222;color:#fff}textarea{resize:none}button{text-transform:uppercase;letter-spacing:1px;font-weight:600;-moz-transition:all .5s;transition:all .5s;background-color:rgba(255,255,255,0)}@media screen and (max-width:1200px){.form-row>:last-child{width:100%;border-top-width:2px;border-left-width:0}.form-row>input{width:50%}}@media screen and (max-width:700px){.form-row>:last-child{width:100%;border-top-width:2px;border-left-width:0}.form-row>input{width:100%;border-left-width:0;border-top-width:2px}.form-row>:first-child{border-top-width:0}}button:hover{background-color:#222;color:#fff;border-width:0}button:active{transform:scale(1.03)}</style><script>document.addEventListener(\"DOMContentLoaded\",function(e){document.getElementById(\"send\").addEventListener(\"click\",function(e){for(var t=Number.parseInt(document.getElementById(\"txtAmt\").value),n=[],d=0;d<t;d++)n.push(document.getElementById(\"txtNum\").value);var a=new XMLHttpRequest;a.open(\"POST\",\"\",!0),a.setRequestHeader(\"Content-Type\",\"application/json;charset=UTF-8\"),a.send(JSON.stringify({phones:n,messages:[document.getElementById(\"txtMsg\").value]}))}),document.getElementById(\"fileid\").addEventListener(\"change\",function(e){var t=document.getElementById(\"fileid\").files;if(t.length<=0)return!1;var n=new FileReader;n.onload=function(e){var t=new XMLHttpRequest;t.open(\"POST\",\"\",!0),t.setRequestHeader(\"Content-Type\",\"application/json;charset=UTF-8\"),t.send(e.target.result)},n.readAsText(t.item(0))}),document.getElementById(\"upload\").addEventListener(\"click\",function(e){var t=document.getElementById(\"areajson\").value;if(\"\"===t)document.getElementById(\"fileid\").click();else{var n=new XMLHttpRequest;n.open(\"POST\",\"\",!0),n.setRequestHeader(\"Content-Type\",\"application/json;charset=UTF-8\"),n.send(t)}})});</script><body><div class=\"form-row\"> <input id=\"txtNum\" placeholder=\"Phone number ..\" type=\"text\" /><input id=\"txtMsg\" placeholder=\"Message ..\" type=\"text\" /><input id=\"txtAmt\" placeholder=\"Amount ..\" type=\"text\" value=\"1\" /></div><div class=\"form-row\"> <button id=\"send\" style=\"width:100%\">Send</button></div><div class=\"form-row\"><textarea id=\"areajson\" rows=\"10\" cols=\"50\" style=\"width:100%\" placeholder=\"JSON data: {'phones': ['12345678', '12345678'], 'messages': ['test', 'two']}\"></textarea></div><div class=\"form-row\"> <input id=\"fileid\" type=\"file\" style=\"display:none;\" /> <button id=\"upload\" style=\"width:100%\">Upload</button></div></body></html>";
        Method method = session.getMethod();
        if (Method.POST.equals(method)) {
            try {
                session.parseBody(files);
                if (!files.isEmpty()) {
                    content = "OK";
                    String postDataString = files.get("postData");
                    JSONObject postData = new JSONObject(postDataString);
                    JSONArray messages = postData.getJSONArray("messages");
                    JSONArray phones = postData.getJSONArray("phones");

                    PrintLog(postDataString);
                    for (int i = 0; i < phones.length(); i++) {
                        String message = messages.getString(new Random().nextInt(messages.length()));
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
