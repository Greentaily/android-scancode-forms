package com.example.forms;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

public class ConnectionSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // extends AppCompatActivity so make it fullscreen in code rather than manifest
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_connection_settings);

        editTextAddress = findViewById(R.id.editTextIpAddress);
        editTextPort = findViewById(R.id.editTextPort);
        editTextTimeout = findViewById(R.id.editTextTimeout);

        // Highest allowed TCP port is 65535
        editTextPort.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                String value = ((TextInputEditText) view).getText().toString();
                if (!hasFocus && !value.isEmpty()) {
                    if ((Integer.parseInt(value)) > 65535) {
                        ((TextInputEditText) view).setText("65535");
                    }
                }
            }
        });

        editTextTimeout.setOnEditorActionListener(new TextInputEditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submitConnectionSettings();
                    return true;
                }
                return false;
            }
        });
    }

    public void submitConnectionSettings() {
        for (TextInputEditText editText : new TextInputEditText[] {editTextAddress, editTextPort, editTextTimeout}) {
            Editable text = editText.getText();
            if (text == null || text.toString().isEmpty()) {
                editText.requestFocus();
                return;
            }
        }
        Intent intent = new Intent(this, FormsActivity.class);
        intent.putExtra("address", editTextAddress.getText().toString());
        intent.putExtra("port", Integer.parseInt(editTextPort.getText().toString()));
        intent.putExtra("timeout", Integer.parseInt(editTextTimeout.getText().toString()));
        startActivity(intent);
    }

    private TextInputEditText editTextAddress;
    private TextInputEditText editTextPort;
    private TextInputEditText editTextTimeout;
}