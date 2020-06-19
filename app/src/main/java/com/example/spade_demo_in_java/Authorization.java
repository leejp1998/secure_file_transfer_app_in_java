package com.example.spade_demo_in_java;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class Authorization extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_authorization);

        Button signinButton = (Button) findViewById(R.id.signinButton);
        signinButton.setOnClickListener((View.OnClickListener)(new View.OnClickListener() {
            public final void onClick(View it) {
                EditText userEditText = (EditText)Authorization.this.findViewById(R.id.userEditText);
                String userID = userEditText.getText().toString();
                EditText passwordEditText = (EditText)Authorization.this.findViewById(R.id.passwordEditText);
                String password = passwordEditText.getText().toString();
                Intent intent = new Intent((Context)Authorization.this, MainActivity.class);
                intent.putExtra("user_id", userID);
                intent.putExtra("user_password", password);
                Authorization.this.startActivity(intent);
                Authorization.this.finish();
            }
        }));
    }
}
