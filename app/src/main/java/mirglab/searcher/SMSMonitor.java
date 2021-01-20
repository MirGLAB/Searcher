package mirglab.searcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SMSMonitor extends BroadcastReceiver {

    private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";
    static boolean have_sms = false;
    SharedPreferences sPref;
    public static BroadcastReceiver smsMonitor;
    DataBase db;
    String operationID;
    final String LOG_TAG = "____";

    @Override
    public void onReceive(Context context, Intent intent) {

        smsMonitor = this;

        db = new DataBase(context);
        db.open();

        try {
            if (intent != null && intent.getAction() != null && ACTION.compareToIgnoreCase(intent.getAction()) == 0) {

                //operationID = intent.getStringExtra("operationID");
                /*
                sPref = context.getSharedPreferences("SmsMonitor", Context.MODE_PRIVATE);
                String operationID = sPref.getString("currentOperation", "");
                SharedPreferences.Editor ed = sPref.edit();
                ed.putString("currentOperation", "");
                ed.apply();
                */
                operationID = CustomSharedPreferences.getDefaults("Current operation", context);

                Log.d(LOG_TAG, "SMS Monitor operationID " + operationID);

                Object[] pduArray = (Object[]) intent.getExtras().get("pdus");
                SmsMessage[] messages = new SmsMessage[pduArray.length];

                    for (int i = 0; i < pduArray.length; i++)
                    {
                        messages[i] = SmsMessage.createFromPdu((byte[]) pduArray[i]);
                        String msg_from = messages[i].getOriginatingAddress();
                        String msgBody = messages[i].getMessageBody();

                        String regexp = operationID;
                        Pattern pattern = Pattern.compile(regexp);
                        Matcher matcher = pattern.matcher(msgBody);
                        if (matcher.find()) {
                            //sPref = context.getSharedPreferences(operationID + "_sms", Context.MODE_PRIVATE);
                            //SharedPreferences.Editor ed = sPref.edit();
                            //ed.putString("smsText", msgBody);
                            //ed.putString("smsNum", msg_from);
                            //ed.apply();

                            //Шаблон СМС-сообщения
                            //22304|55.12345|37.213445|21:31:12
                            String[] messageParts = msgBody.split("\\|");
                            for(int j = 0; j < messageParts.length; j++)
                                Log.d(LOG_TAG, "Message parts: part " + j + " " + messageParts[j]);
                            String memberGroupName = messageParts[1];
                            String messageLat = messageParts[2].replace(",", ".");
                            String messageLng = messageParts[3].replace(",", ".");
                            String messageTime = messageParts[4];
                            String messageInfo = "-";
                            if(messageParts.length > 5)
                                messageInfo = messageParts[5];
                            Log.d("SMS!", "SMS пришло");
                            /*
                            String memberGroupName = "";
                            Cursor memberCursor = db.getFieldData("Members", "operation", operationID);
                            if(memberCursor.moveToFirst()) {
                                int numberColIndex = memberCursor.getColumnIndex("number");
                                do {
                                    String memberNumber = memberCursor.getString(numberColIndex);
                                    if(memberNumber.equals(msg_from)) {
                                        int groupNameColIndex = memberCursor.getColumnIndex("groupName");
                                        memberGroupName = memberCursor.getString(groupNameColIndex);
                                        break;
                                    }
                                } while(memberCursor.moveToNext());
                            } else {
                                Log.d(LOG_TAG, "Groups count - 0");
                            }
                            memberCursor.close();
                            */

                            ArrayList<String> message = new ArrayList<>(Arrays.asList(operationID, memberGroupName, msg_from, messageTime,
                                    messageLat, messageLng, messageInfo));
                            db.addRec("Markers", message);
                            Log.d("SMS!", "SMS загружено");
                        }

                        /*
                        //if(msgBody.charAt(0) == '&') {
                        if(msgBody.contains(operationID)) {

                            Log.d("SMS!", "SMS подходит");
                            //sPref = PreferenceManager.getDefaultSharedPreferences(context);
                            /*
                            sPref = context.getSharedPreferences("liza_alert_sms", Context.MODE_PRIVATE);
                            SharedPreferences.Editor ed = sPref.edit();
                            ed.putString("smsText", msgBody);
                            ed.putString("smsNum", msg_from);
                            ed.apply();

                            ArrayList<String> message = new ArrayList<>();
                            message.addAll(Arrays.asList(operationID, msg_from, messageText));
                            db.addRec();
                            Log.d("SMS!", "SMS загружено");
                        }*/

                        /*
                        if (msgBody.contains("&"))
                        {
                            have_sms = true;
                            Intent mIntent = new Intent(context, GetSmsService.class);
                            mIntent.putExtra("&", msgBody);
                            mIntent.putExtra("#", msg_from);
                            context.startService(mIntent);
                            abortBroadcast();
                        }
                        */
                    }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean check() {
        if(have_sms) {
            have_sms = false;
            return true;
        }
        else {
            return false;
        }
    }

}
