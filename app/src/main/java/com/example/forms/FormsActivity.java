package com.example.forms;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class FormsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // extends AppCompatActivity so make it fullscreen in code rather than manifest
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_forms);

        Intent intent = getIntent();
        host = intent.getStringExtra("address");
        port = intent.getIntExtra("port", 0);
        timeout = intent.getIntExtra("timeout", 0);

        editTextSerial = findViewById(R.id.editTextSerial);
        editTextSerial.requestFocus();
        editTextForm = findViewById(R.id.editTextForm);
        editTextForm.setText("1");

        findAllDataFields();
        if (!dataFields.isEmpty()) {
            setupImeActions();
        }
    }

    private void findAllDataFields() {
        dataFields = new ArrayList<>();
        LinearLayout dataFieldsLayout = findViewById(R.id.dataFieldsLayout);
        for (int i = 0; i < dataFieldsLayout.getChildCount(); i++) {
            View view = dataFieldsLayout.getChildAt(i);
            if (!(view instanceof TextInputLayout)) continue;

            TextInputLayout textInputLayout = (TextInputLayout) view;
            TextInputEditText editText = (TextInputEditText) textInputLayout.getEditText();
            dataFields.add(editText);
        }
    }

    private void setupImeActions() {
        for (TextInputEditText editText : dataFields) {
            editText.setOnEditorActionListener(new TextInputEditText.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        int idx = dataFields.indexOf((TextInputEditText) textView);
                        retrieveData(textView.getText().toString(), idx);
                        return true;
                    } else if (actionId == EditorInfo.IME_ACTION_DONE) {
                        int idx = dataFields.size() - 1;
                        retrieveData(textView.getText().toString(), idx);
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    private void retrieveData(String queryData, int fieldIndex) {
        StringBuilder query = new StringBuilder("Q1")
                .append(editTextForm.getText().toString())
                .append("\u0001")
                .append(editTextSerial.getText().toString())
                .append("\u0002");
        for (int i = 0; i < dataFields.size(); i++) {
            String field = (i == fieldIndex ? queryData + '\t' : String.valueOf('\t'));
            query.append(field);
        }
        query.append('\r');

        new RetrieveDataAsyncTask(this, fieldIndex).execute(host, String.valueOf(port), query.toString());
    }

    private static class RetrieveDataAsyncTask extends AsyncTask<String, Void, String> {
        // don't make activity leak if it gets destroyed before this task is finished
        private final WeakReference<FormsActivity> activity;
        private final int fieldIndex;
        private final AlertDialog dialog;
        private Exception exception;

        RetrieveDataAsyncTask(FormsActivity activity, int fieldIndex) {
            super();
            this.activity = new WeakReference<>(activity);
            this.fieldIndex = fieldIndex;
            dialog = new AlertDialog.Builder(this.activity.get())
                    .setMessage("Retrieving data...")
                    .setCancelable(false)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            cancel(true);
                        }
                    }).create();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.show();
        }

        @Override
        protected String doInBackground(String... connectionParameters) {
            StringBuilder response = new StringBuilder();
            try {
                Socket socket = new Socket(connectionParameters[0], Integer.parseInt(connectionParameters[1]));
                OutputStream out = socket.getOutputStream();
                out.write(connectionParameters[2].getBytes(StandardCharsets.US_ASCII));

                InputStream in = socket.getInputStream();
                byte[] receivedDataBuf = new byte[256];
                int bytesReceived = 0;
                while ((bytesReceived = in.read(receivedDataBuf)) != -1) {
                    String receivedString = new String(receivedDataBuf, 0, bytesReceived);
                    response.append(new String(receivedDataBuf, 0, bytesReceived));
                    // server won't close connection but it marks the end of response with an \r character
                    if (receivedString.charAt(receivedString.length() - 1) == '\r') break;
                }
                socket.close();
                out.close();
                in.close();
            } catch (Exception exception) {
                this.exception = exception;
                return exception.getMessage();
            }
            return response.toString();
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            // activity finished before task, nothing to do here
            if (activity.get() == null) return;
            else if (exception != null) {
                Toast.makeText(activity.get(), exception.getMessage(), Toast.LENGTH_LONG).show();
            }
            else {
                if (fieldIndex == (activity.get().dataFields.size() - 1)) {
                    int nextForm = Integer.parseInt(activity.get().editTextForm.getText().toString()) + 1;
                    activity.get().editTextForm.setText(String.valueOf(nextForm));
                    for (TextInputEditText editText : activity.get().dataFields) editText.getText().clear();
                    activity.get().dataFields.get(0).requestFocus();
                }
                else {
                    // stub
                    Toast.makeText(activity.get(), response, Toast.LENGTH_LONG).show();
                    //
                    activity.get().dataFields.get(fieldIndex + 1).requestFocus();
                }
            }
            dialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Quit forms")
                .setMessage("Return to connection screen and reset local form data?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int id) {
                        quit();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void quit() {
        super.onBackPressed();
    }

    private String host;
    private int port;
    private int timeout;

    private TextInputEditText editTextSerial;
    private TextInputEditText editTextForm;

    private ArrayList<TextInputEditText> dataFields;
}