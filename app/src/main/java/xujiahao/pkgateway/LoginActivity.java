package xujiahao.pkgateway;

import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import xujiahao.pkgateway.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {
    ActivityLoginBinding B;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        B = DataBindingUtil.setContentView(this, R.layout.activity_login);
        // if there are ID and password in preference, set it in editText.
        SharedPreferences preferences = getSharedPreferences(KEY.MAIN, MODE_PRIVATE);
        String studentID = preferences.getString(KEY.STUDENT_ID,"");
        String password = preferences.getString(KEY.PASSWORD,"");
        if(!studentID.isEmpty()&&!password.isEmpty()){
            B.etPassword.setText(password);
            B.etStudentID.setText(studentID);
        }
    }

    public void clickSignIn(View view){
        SharedPreferences preferences = getSharedPreferences(KEY.MAIN, MODE_PRIVATE);
        preferences
                .edit()
                .putString(KEY.STUDENT_ID,B.etStudentID.getText().toString())
                .putString(KEY.PASSWORD,B.etPassword.getText().toString())
        .apply();

        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }
}
