package mirglab.searcher;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.google.gson.Gson;

import java.nio.charset.Charset;
import java.util.ArrayList;



public class GroupsActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LoaderManager.LoaderCallbacks<Cursor> {

    DataBase db;
    final String LOG_TAG = "____";
    ExpandableListView expListGroups;
    String operationID;
    int groupCount;
    Button btnCreateGroups, btnDeleteGroup, btnDistributePlots;
    private static final int CM_DELETE_ID = 1;
    CustomAdapter adapter;
    ArrayList<Parent> arrayParents;
    ArrayList<String> arrayChildren;
    private Message messageMemberTelephone;
    private static final Gson gson = new Gson();
    private GoogleApiClient mGoogleApiClient;
    boolean canSendMessage = false;
    final Context context = this;
    boolean changeColor = false;
    private MessageListener subscribeMessageListener;

    private static final int TTL_IN_SECONDS = 300 * 60; // Three minutes.
    private static final Strategy PUB_SUB_STRATEGY = new Strategy.Builder()
            .setTtlSeconds(TTL_IN_SECONDS).build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups);

        db = new DataBase(this);
        db.open();

        buildGoogleApiClient();

        operationID = getIntent().getStringExtra("id");
        /*
        if(getIntent().getStringExtra("groupCount")!=null) {
            groupCount = Integer.parseInt(getIntent().getStringExtra("groupCount"));
            for (int i = 0; i < groupCount; i++)
                db.addRec("Groups", new ArrayList<String>(Arrays.asList(operationID, "Лиза")));
        }
        */
        expListGroups = findViewById(R.id.expListGroups);

        fillList();

        subscribeMessageListener = new MessageListener() {
            @Override
            public void onFound(Message message) {
                Log.d(LOG_TAG, "Message found: " + new String(message.getContent()));
                String messageFull = new String(message.getContent());
                if(messageFull.contains(operationID) && messageFull.contains("ok")) {
                    //Toast.makeText(context, "Получены данные о новом участнике операции", Toast.LENGTH_LONG).show();
                    messageFull = messageFull.substring(1, messageFull.length() - 1);
                    String[] messageParts = new String[3];
                    messageParts = messageFull.split("\\|");
                    String memberNumber = messageParts[1];
                    String memberGroup = messageParts[2];

                    Cursor memberCursor = db.getFieldData("Members", "operation", operationID);
                    if(memberCursor.moveToFirst()) {
                        int memberNumberColIndex = memberCursor.getColumnIndex("number");
                        int memberGroupColIndex = memberCursor.getColumnIndex("groupName");
                        do{
                            if(memberCursor.getString(memberNumberColIndex).equals(memberNumber)) {
                                if(memberCursor.getString(memberGroupColIndex).equals(memberGroup)) {
                                    db.modifyFieldData("Members", operationID, "number", memberNumber, "gotMessage", "true");
                                    adapter.notifyDataSetChanged();
                                }
                                break;
                            }
                        } while(memberCursor.moveToNext());
                    } else {
                        Log.d(LOG_TAG, "Присовоение группы участнику ошибка");
                    }
                    memberCursor.close();
                }
            }

            @Override
            public void onLost(Message message) {
                Log.d(LOG_TAG, "Lost sight of message: " + new String(message.getContent()));
            }
        };

        //sets the adapter that provides data to the list.
        adapter = new CustomAdapter(GroupsActivity.this, arrayParents);
        expListGroups.setAdapter(new CustomAdapter(GroupsActivity.this, arrayParents));

        //Button addMembers = (Button) findViewById(R.id.addMembers);

        expListGroups.setOnItemLongClickListener(new ExpandableListView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, final View view, final int groupPosition, long id) {

                //final String groupName = "Лиза-" + Integer.toString(groupPosition+1);

                if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {

                    final String groupName = "Лиза-" + Integer.toString(ExpandableListView.getPackedPositionGroup(id)+1);

                    AlertDialog.Builder builder = new AlertDialog.Builder(GroupsActivity.this);

                    final ArrayList<String> arrayMembers = new ArrayList<>();
                    Cursor cursor = db.getFieldData("Members", "operation", operationID);
                    if (cursor.moveToFirst()) {

                        int memberIdColIndex = cursor.getColumnIndex("_id");
                        int memberNameColIndex = cursor.getColumnIndex("name");
                        int memberInfoColIndex = cursor.getColumnIndex("info");
                        int memberGroupColIndex = cursor.getColumnIndex("groupName");

                        do {
                            if (cursor.getString(memberGroupColIndex).equals("-")) {
                                String memberId = cursor.getString(memberIdColIndex);
                                String memberName = cursor.getString(memberNameColIndex);
                                String memberInfo = cursor.getString(memberInfoColIndex);
                                arrayMembers.add(memberId + ") " + memberName + " - " + memberInfo);
                            }
                        } while (cursor.moveToNext());
                    } else
                        Log.d(LOG_TAG, "members - 0 rows");
                    cursor.close();

                    String[] membersString = new String[arrayMembers.size()];
                    for (int i = 0; i < arrayMembers.size(); i++)
                        membersString[i] = arrayMembers.get(i);

                    // Boolean array for initial selected items
                    final boolean[] checkedMembers = new boolean[arrayMembers.size()];

                    // Set multiple choice items for alert dialog
                    /*
                        AlertDialog.Builder setMultiChoiceItems(CharSequence[] items, boolean[]
                        checkedItems, DialogInterface.OnMultiChoiceClickListener listener)
                            Set a list of items to be displayed in the dialog as the content,
                            you will be notified of the selected item via the supplied listener.
                     */
                    /*
                        DialogInterface.OnMultiChoiceClickListener
                        public abstract void onClick (DialogInterface dialog, int which, boolean isChecked)

                            This method will be invoked when an item in the dialog is clicked.

                            Parameters
                            dialog The dialog where the selection was made.
                            which The position of the item in the list that was clicked.
                            isChecked True if the click checked the item, else false.
                     */
                    builder.setMultiChoiceItems(membersString, checkedMembers, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                            // Update the current focused item's checked status
                            checkedMembers[which] = isChecked;

                            // Get the current focused item
                            String currentItem = arrayMembers.get(which);

                            // Notify the current action
                            //Toast.makeText(getApplicationContext(),
                            //        currentItem + " " + isChecked, Toast.LENGTH_SHORT).show();
                        }
                    });

                    // Specify the dialog is not cancelable
                    builder.setCancelable(true);

                    // Set a title for alert dialog
                    builder.setTitle("Добавить участников в группу " + groupName);

                    // Set the positive/yes button click listener
                    builder.setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            int childPos = 0;
                            for (int i = 0; i < arrayMembers.size(); i++) {
                                if (checkedMembers[i]) {
                                    childPos = i;
                                    String id = arrayMembers.get(i).substring(0, arrayMembers.get(i).indexOf(")"));
                                    db.modifyFieldData("Members", operationID, "_id", id, "groupName", groupName);
                                    String memberTelephone = "";
                                    Cursor memberCursor = db.getFieldData("Members", "_id", Integer.parseInt(id));
                                    if (memberCursor.moveToFirst()) {
                                        memberTelephone = memberCursor.getString(memberCursor.getColumnIndex("number"));
                                    } else {
                                        Log.d(LOG_TAG, "Присовоение группы участнику ошибка");
                                    }
                                    memberCursor.close();

                                    String zones = "";
                                    Cursor groupCursor = db.getFieldData("Groups", "operation", operationID);
                                    if (groupCursor.moveToFirst()) {
                                        do {
                                            if(groupCursor.getString(groupCursor.getColumnIndex("name")).equals(groupName)) {
                                                zones = groupCursor.getString(groupCursor.getColumnIndex("zone"));
                                                break;
                                            }
                                        } while(groupCursor.moveToNext());
                                    } else {
                                        Log.d(LOG_TAG, "Присовоение группы участнику ошибка");
                                    }
                                    groupCursor.close();

                                    messageMemberTelephone = new Message(gson.toJson(memberTelephone + "|" +
                                            groupName + "|" + zones).getBytes(Charset.forName("UTF-8")));
                                    Log.d(LOG_TAG, messageMemberTelephone.toString());
                                    /*
                                    while(!canSendMessage) {
                                        try {
                                            wait(1);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    */
                                    if (canSendMessage) {
                                        publish();
                                    } else {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                                                .setMessage("Не удается передать и получить данные, проверьте подключение к интернету")
                                                .setCancelable(false)
                                                .setPositiveButton("Повторить", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        buildGoogleApiClient();
                                                    }
                                                })
                                                .setNeutralButton("Открыть настройки", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                                                    }
                                                });
                                        builder.create();
                                        builder.show();
                                    }
                                }
                            }
                            fillList();
                            adapter = new CustomAdapter(GroupsActivity.this, arrayParents);
                            expListGroups.setAdapter(new CustomAdapter(GroupsActivity.this, arrayParents));
                            //expListGroups.getChildAt(groupPosition).setBackgroundColor(Color.GREEN);
                            //changeColor = true;
                            //adapter.notifyDataSetChanged();
                        }
                    });

                    builder.setNeutralButton("Изменить", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            AlertDialog.Builder builder = new AlertDialog.Builder(GroupsActivity.this);

                            final ArrayList<String> arrayMembersInGroup = new ArrayList<>();
                            Cursor cursor = db.getFieldData("Members", "operation", operationID);
                            if (cursor.moveToFirst()) {

                                int memberIdColIndex = cursor.getColumnIndex("_id");
                                int memberNameColIndex = cursor.getColumnIndex("name");
                                int memberInfoColIndex = cursor.getColumnIndex("info");
                                int memberGroupColIndex = cursor.getColumnIndex("groupName");

                                do {
                                    if (cursor.getString(memberGroupColIndex).equals(groupName)) {
                                        String memberId = cursor.getString(memberIdColIndex);
                                        String memberName = cursor.getString(memberNameColIndex);
                                        String memberInfo = cursor.getString(memberInfoColIndex);
                                        arrayMembersInGroup.add(memberId + ") " + memberName + " - " + memberInfo);
                                    }
                                } while (cursor.moveToNext());
                            } else
                                Log.d(LOG_TAG, "members - 0 rows");
                            cursor.close();

                            String[] membersStringInGroup = new String[arrayMembersInGroup.size()];
                            for (int i = 0; i < arrayMembersInGroup.size(); i++)
                                membersStringInGroup[i] = arrayMembersInGroup.get(i);

                            // Boolean array for initial selected items
                            final boolean[] checkedMembersInGroup = new boolean[arrayMembersInGroup.size()];

                            builder.setMultiChoiceItems(membersStringInGroup, checkedMembersInGroup, new DialogInterface.OnMultiChoiceClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                                    // Update the current focused item's checked status
                                    checkedMembersInGroup[which] = isChecked;

                                    // Get the current focused item
                                    String currentItem = arrayMembersInGroup.get(which);

                                    // Notify the current action
                                    //Toast.makeText(getApplicationContext(),
                                    //        currentItem + " " + isChecked, Toast.LENGTH_SHORT).show();
                                }
                            });

                            // Specify the dialog is not cancelable
                            builder.setCancelable(true);

                            // Set a title for alert dialog
                            builder.setTitle("Удалить участников из группы " + groupName);

                            // Set the positive/yes button click listener
                            builder.setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    for (int i = 0; i < arrayMembersInGroup.size(); i++) {
                                        if (checkedMembersInGroup[i]) {
                                            String id = arrayMembersInGroup.get(i).substring(0, arrayMembersInGroup.get(i).indexOf(")"));
                                            db.modifyFieldData("Members", operationID, "_id", id, "groupName", "-");
                                            db.modifyFieldData("Members", operationID, "_id", id, "gotMessage", "false");
                                        }
                                    }
                                    fillList();
                                    adapter = new CustomAdapter(GroupsActivity.this, arrayParents);
                                    expListGroups.setAdapter(new CustomAdapter(GroupsActivity.this, arrayParents));
                                }
                            });


                            // Set the neutral/cancel button click listener
                            builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Do something when click the neutral button
                                }
                            });

                            AlertDialog dialogInGroup = builder.create();
                            // Display the alert dialog on interface
                            dialogInGroup.show();
                        }
                    });

                    // Set the neutral/cancel button click listener
                    builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do something when click the neutral button
                        }
                    });

                    AlertDialog dialog = builder.create();
                    // Display the alert dialog on interface
                    dialog.show();
                } else if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {


                    final int childPos = ExpandableListView.getPackedPositionChild(id);
                    int parentPos = ExpandableListView.getPackedPositionGroup(id);
                    //Log.d(LOG_TAG, "Child position " + Integer.toString(ExpandableListView.getPackedPositionChild(id)+1));
                    //Log.d(LOG_TAG, "Parent title" + arrayParents.get(parentPos).getArrayChildren().get(childPos));

                    final String groupName = arrayParents.get(parentPos).getTitle();
                    final String childName = arrayParents.get(parentPos).getArrayChildren().get(childPos);

                    AlertDialog.Builder builder = new AlertDialog.Builder(GroupsActivity.this);

                    // Specify the dialog is not cancelable
                    builder.setCancelable(true);

                    // Set a title for alert dialog]
                    builder.setTitle("Повторить отправку данных?");

                    // Set the positive/yes button click listener
                    builder.setPositiveButton("Повторить", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            String zones = "";
                            String memberTelephone = childName.substring(0, childName.indexOf("-") - 1);

                            Cursor cursor = db.getFieldData("Groups", "operation", operationID);
                            if (cursor.moveToFirst()) {
                                do {
                                    if(cursor.getString(cursor.getColumnIndex("name")).equals(groupName)) {
                                        zones = cursor.getString(cursor.getColumnIndex("zone"));
                                        break;
                                    }
                                } while(cursor.moveToNext());
                            } else {
                                Log.d(LOG_TAG, "Присовоение группы участнику ошибка");
                            }
                            cursor.close();

                            messageMemberTelephone = new Message(gson.toJson(memberTelephone + "|" +
                                    groupName + "|" + zones).getBytes(Charset.forName("UTF-8")));
                            Log.d(LOG_TAG, messageMemberTelephone.toString());

                            if (canSendMessage) {
                                publish();
                            } else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                                        .setMessage("Не удается передать и получить данные, проверьте подключение к интернету")
                                        .setCancelable(false)
                                        .setPositiveButton("Повторить", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                buildGoogleApiClient();
                                            }
                                        })
                                        .setNeutralButton("Открыть настройки", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                                            }
                                        });
                                builder.create();
                                builder.show();
                            }
                        }
                    });

                    // Set the neutral/cancel button click listener
                    builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do something when click the neutral button
                            dialog.dismiss();
                        }
                    });

                    AlertDialog dialog = builder.create();
                    // Display the alert dialog on interface
                    dialog.show();
                }
                return false;
            }
        });

        expListGroups.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                return false;
            }
        });

        btnCreateGroups = findViewById(R.id.btnCreateGroup);
        btnCreateGroups.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                /*
                db.addRec("Groups", new ArrayList<String>(Arrays.asList(operationID, "Лиза")));
                fillList();
                adapter = new CustomAdapter(GroupsActivity.this, arrayParents);
                expListGroups.setAdapter(new CustomAdapter(GroupsActivity.this, arrayParents));
                */
            }
        });

        /*
        btnDeleteGroup = (Button) findViewById(R.id.btnDeleteGroup);
        btnDeleteGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int groupCount = db.getFieldData("Groups", "operation", operationID).getCount();

                Cursor cursor = db.getFieldData("Members", "operation", operationID);
                if(cursor.moveToFirst()) {
                    int memberGroupColIndex = cursor.getColumnIndex("groupName");
                    int memberIdColIndex = cursor.getColumnIndex("_id");
                    do {
                        if(cursor.getString(memberGroupColIndex).equals("Лиза-" + Integer.toString(groupCount))) {
                            String memberId = cursor.getString(memberIdColIndex);
                            db.modifyFieldData("Members", "_id", memberId, "groupName", "-");
                        }
                    } while (cursor.moveToNext());
                } else
                    Log.d(LOG_TAG, "members - 0 rows");
                cursor.close();

                db.deleteFiledData("Groups", "_id", Integer.toString(groupCount));
                fillList();
                adapter = new CustomAdapter(GroupsActivity.this, arrayParents);
                expListGroups.setAdapter(new CustomAdapter(GroupsActivity.this, arrayParents));

            }
        });
        */

        btnDistributePlots = findViewById(R.id.btnDistributePlots);
        btnDistributePlots.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                setResult(2, intent);
                finish();
                /*
                Intent intent = new Intent(GroupsActivity.this, DistributeGroupsActivity.class);
                intent.putExtra("id", operationID);
                //startActivity(intent);
                startActivityForResult(intent, 2);
                */
            }
        });
    }

    private void buildGoogleApiClient() {
        if (mGoogleApiClient != null) {
            return;
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == resultCode) {
            Intent intent = new Intent();
            setResult(1, intent);
            finish();
        }
    }

    public void fillList() {

        arrayParents = new ArrayList<Parent>();
        arrayChildren = new ArrayList<String>();

        Cursor groupCursor = db.getFieldData("Groups", "operation", operationID);
        if(groupCursor.moveToFirst()) {

            // определяем номера столбцов по имени в выборке
            int nameColIndex = groupCursor.getColumnIndex("name");

            do {
                String groupName = groupCursor.getString(nameColIndex);
                Parent parent = new Parent();
                parent.setTitle(groupName);
                arrayChildren = new ArrayList<String>();

                //Cursor childrenCursorOperation = db.getFieldData("Members", "operation", operationID);
                Cursor childrenCursor = db.getFieldData("Members", "operation", operationID);

                if(childrenCursor.moveToFirst()) {

                    //int memberIdColIndex = childrenCursor.getColumnIndex("_id");
                    int memberNumberColIndex = childrenCursor.getColumnIndex("number");
                    int memberNameColIndex = childrenCursor.getColumnIndex("name");
                    int memberInfoColIndex = childrenCursor.getColumnIndex("info");
                    int memberGroupColIndex = childrenCursor.getColumnIndex("groupName");
                    Log.d(LOG_TAG, Integer.toString(memberNameColIndex) + " " + Integer.toString(memberInfoColIndex) +
                        " " + Integer.toString(memberGroupColIndex));

                    do {
                        //String memberName = childrenCursor.getString(memberNameColIndex);
                        //Log.d(LOG_TAG, memberName);
                        String memberGroup = childrenCursor.getString(memberGroupColIndex);
                        //Log.d(LOG_TAG, memberGroup);
                        if(memberGroup.equals(groupName)) {
                            //String memberId = childrenCursor.getString(memberIdColIndex);
                            String memberNumber = childrenCursor.getString(memberNumberColIndex);
                            String memberName = childrenCursor.getString(memberNameColIndex);
                            String memberInfo = childrenCursor.getString(memberInfoColIndex);
                            //arrayChildren.add(memberId + ") " + memberName + " - " + memberInfo);
                            arrayChildren.add(memberNumber + " - " + memberName + " - " + memberInfo);
                        }
                    } while (childrenCursor.moveToNext());
                } else
                    Log.d(LOG_TAG, "children - 0 rows");
                childrenCursor.close();

                parent.setArrayChildren(arrayChildren);
                arrayParents.add(parent);

                // переход на следующую строку
                // а если следующей нет (текущая - последняя), то false - выходим из цикла
            } while (groupCursor.moveToNext());
        } else
            Log.d(LOG_TAG, "group - 0 rows");
        groupCursor.close();
    }

    @Override
    public void onStop() {
        if(messageMemberTelephone != null)
            Nearby.getMessagesClient(this).unpublish(messageMemberTelephone);
        if(subscribeMessageListener != null)
            Nearby.getMessagesClient(this).unsubscribe(subscribeMessageListener);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if(messageMemberTelephone != null)
            Nearby.getMessagesClient(this).unpublish(messageMemberTelephone);
        if(subscribeMessageListener != null)
            Nearby.getMessagesClient(this).unsubscribe(subscribeMessageListener);
        db.close();
        super.onDestroy();
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
        return null;
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {

    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

    private void publish() {
        Log.d(LOG_TAG, "Publishing");
        PublishOptions options = new PublishOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new PublishCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.d(LOG_TAG, "No longer publishing");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            }
                        });
                    }
                }).build();

        Nearby.Messages.publish(mGoogleApiClient, messageMemberTelephone, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            //Toast.makeText(context, "Данные операции отправлены", Toast.LENGTH_LONG).show();
                            Log.d(LOG_TAG, "Published successfully.");
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                                    .setMessage("Не удается передать и получить данные, проверьте подключение к интернету")
                                    .setCancelable(false)
                                    .setPositiveButton("Повторить", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            buildGoogleApiClient();
                                        }
                                    })
                                    .setNeutralButton("Открыть настройки", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                                        }
                                    });
                            builder.create();
                            builder.show();
                            //Toast.makeText(context, "Для отправки данных требуется подключение к интернету", Toast.LENGTH_LONG).show();
                            Log.d(LOG_TAG,"Could not publish, status = " + status);
                        }
                    }
                });
    }

    private void subscribe() {
        Log.i(LOG_TAG, "Subscribing");
        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new SubscribeCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.d(LOG_TAG, "No longer subscribing");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            }
                        });
                    }
                }).build();

        Nearby.Messages.subscribe(mGoogleApiClient, subscribeMessageListener, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            //Toast.makeText(context, "Данные волонтеров принимаются", Toast.LENGTH_LONG).show();
                            Log.d(LOG_TAG, "Subscribed successfully.");
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                                    .setMessage("Не удается передать и получить данные, проверьте подключение к интернету")
                                    .setCancelable(false)
                                    .setPositiveButton("Повторить", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            buildGoogleApiClient();
                                        }
                                    })
                                    .setNeutralButton("Открыть настройки", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                                        }
                                    });
                            builder.create();
                            builder.show();
                            //Toast.makeText(context, "Для получения данных волонтеров требуется подключение к интернету", Toast.LENGTH_LONG).show();
                            Log.d(LOG_TAG,"Could not subscribe, status = " + status);
                        }
                    }
                });
    }

    private void unsubscribe() {
        Log.d(LOG_TAG, "Unsubscribing.");
        if((mGoogleApiClient != null) && (subscribeMessageListener != null)) {
            Nearby.Messages.unsubscribe(mGoogleApiClient, subscribeMessageListener);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //publish();
        subscribe();
        canSendMessage = true;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setMessage("Не удается передать и получить данные, проверьте подключение к интернету")
                .setCancelable(false)
                .setPositiveButton("Повторить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        buildGoogleApiClient();
                    }
                })
                .setNeutralButton("Открыть настройки", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                    }
                });
        builder.create();
        builder.show();
    }

    public class Parent {

        private String mTitle;

        private ArrayList<String> mArrayChildren;

        public String getTitle() {
            return mTitle;
        }

        public void setTitle(String title) {
            mTitle = title;
        }

        public ArrayList<String> getArrayChildren() {
            return mArrayChildren;
        }

        public void setArrayChildren(ArrayList<String> arrayChildren) {
            mArrayChildren = arrayChildren;
        }
    }

    public class CustomAdapter extends BaseExpandableListAdapter {

        public LayoutInflater inflater;
        public ArrayList<Parent> mParent;

        public CustomAdapter(Context context, ArrayList<Parent> parent){
            mParent = parent;
            inflater = LayoutInflater.from(context);
        }

        @Override
        //counts the number of group/parent items so the list knows how many times calls getGroupView() method
        public int getGroupCount() {
            return mParent.size();
        }

        @Override
        //counts the number of children items so the list knows how many times calls getChildView() method
        public int getChildrenCount(int i) {
            return mParent.get(i).getArrayChildren().size();
        }

        @Override
        //gets the title of each parent/group
        public Object getGroup(int i) {
            return mParent.get(i).getTitle();
        }

        @Override
        //gets the name of each item
        public Object getChild(int i, int i1) {
            return mParent.get(i).getArrayChildren().get(i1);
        }

        @Override
        public long getGroupId(int i) {
            return i;
        }

        @Override
        public long getChildId(int i, int i1) {
            return i1;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        //in this method you must set the text to see the parent/group on the list
        public View getGroupView(int groupPosition, boolean b, View view, ViewGroup viewGroup) {

            ViewHolder holder = new ViewHolder();
            holder.groupPosition = groupPosition;

            if (view == null) {
                view = inflater.inflate(R.layout.list_item_parent, viewGroup,false);
            }

            TextView textView = view.findViewById(R.id.list_item_text_view);
            textView.setText(getGroup(groupPosition).toString());

            view.setTag(holder);

            //return the entire view
            return view;
        }

        @Override
        //in this method you must set the text to see the children on the list
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, ViewGroup viewGroup) {

            /*
            if(view == null)
                Log.d(LOG_TAG, "Now wee'll see: " + "1) groupPosition " + Integer.toString(groupPosition) + "; 2) childPosition " +
                    Integer.toString(childPosition) + "; 3) isLastChild " + isLastChild + "; 4) view null" +
                    "; 5) viewGroup " + viewGroup.toString());
            else
                Log.d(LOG_TAG, "Now wee'll see: " + "1) groupPosition " + Integer.toString(groupPosition) + "; 2) childPosition " +
                        Integer.toString(childPosition) + "; 3) isLastChild " + isLastChild + "; 4) view " + view.toString() +
                        "; 5) viewGroup " + viewGroup.toString());
                        */

            ViewHolder holder = new ViewHolder();
            holder.childPosition = childPosition;
            holder.groupPosition = groupPosition;

            if (view == null) {
                view = inflater.inflate(R.layout.list_item_child, viewGroup,false);
            }

            TextView textView = view.findViewById(R.id.list_item_text_child);
            textView.setText(mParent.get(groupPosition).getArrayChildren().get(childPosition));

            view.setTag(holder);

            String memberNumber = mParent.get(groupPosition).getArrayChildren().get(childPosition).substring(0,
                    mParent.get(groupPosition).getArrayChildren().get(childPosition).indexOf("-") - 1);
            Log.d(LOG_TAG, "Member telephone [" + memberNumber + "]");
            //Boolean greenBackground = false;
            Cursor memberCursor = db.getFieldData("Members", "operation", operationID);
            if(memberCursor.moveToFirst()) {
                int memberNumberColIndex = memberCursor.getColumnIndex("number");
                int memberGotMessageColIndex = memberCursor.getColumnIndex("gotMessage");
                do{
                    if(memberCursor.getString(memberNumberColIndex).equals(memberNumber)) {
                        if(memberCursor.getString(memberGotMessageColIndex).equals("true")) {
                            Log.d(LOG_TAG, "Green background");
                            //greenBackground = true;
                            view.setBackgroundColor(Color.GREEN);
                        }
                        break;
                    }
                } while(memberCursor.moveToNext());
            } else {
                Log.d(LOG_TAG, "Присовоение группы участнику ошибка");
            }
            memberCursor.close();

            /*
            if(greenBackground) {
                Log.d(LOG_TAG, "Why in a hell you did this?!");
                //view.setBackgroundColor(Color.GREEN);
                greenBackground = false;
            }
            */

            //return the entire view
            return view;
        }

        /*
        public void getChildViewCustom(int groupPosition, int childPosition, View viewGroup) {

            Log.d(LOG_TAG, "Custom get child view");
            ViewHolder holder = new ViewHolder();
            holder.childPosition = childPosition;
            holder.groupPosition = groupPosition;
            View view = inflater.inflate(R.layout.list_item_child, (ViewGroup) viewGroup, false);

            TextView textView = (TextView) view.findViewById(R.id.list_item_text_child);
            textView.setText("");

            view.setTag(holder);
            view.setBackgroundColor(Color.GREEN);
        }
        */

        @Override
        public boolean isChildSelectable(int i, int i1) {
            return true;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            /* used to make the notifyDataSetChanged() method work */
            super.registerDataSetObserver(observer);
        }

// Intentionally put on comment, if you need on click deactivate it
/*  @Override
    public void onClick(View view) {
        ViewHolder holder = (ViewHolder)view.getTag();
        if (view.getId() == holder.button.getId()){

           // DO YOUR ACTION
        }
    }*/

        protected class ViewHolder {
            protected int childPosition;
            protected int groupPosition;
            protected Button button;
        }
    }


}
