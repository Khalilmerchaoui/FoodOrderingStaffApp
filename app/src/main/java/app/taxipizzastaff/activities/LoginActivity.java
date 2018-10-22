package app.taxipizzastaff.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import app.taxipizzastaff.R;
import app.taxipizzastaff.Utils.Config;
import app.taxipizzastaff.models.Staff;
import dmax.dialog.SpotsDialog;

public class LoginActivity extends AppCompatActivity {

    FirebaseDatabase database;
    DatabaseReference staff;

    EditText edtName, edtPassword;
    Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtName = findViewById(R.id.edtName);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);

        database = FirebaseDatabase.getInstance();
        staff = database.getReference("Staff");

        if(Config.getCurrentUser(getApplicationContext()) != null) {
            Intent main = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(main);
            finish();
        }
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (Config.isConnectedToInternet(getApplicationContext())) {
                    staff.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            //Get User Information
                            if (dataSnapshot.child(edtName.getText().toString()).exists()) {
                                Staff staff = dataSnapshot.child(edtName.getText().toString()).getValue(Staff.class);
                                staff.setName(edtName.getText().toString());
                                if (staff.getPassword().equals(edtPassword.getText().toString())) {
                                    Config.setCurrentUser(getBaseContext(), staff);

                                    final AlertDialog waitingDialog = new SpotsDialog(LoginActivity.this, R.style.Custom);
                                    waitingDialog.show();
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            waitingDialog.dismiss();
                                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                            finish();
                                        }
                                    }, 2100);
                                } else
                                    Snackbar.make(v, "Le Téléphone ou le mot de passe est incorrect", Snackbar.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Snackbar.make(v, "Une erreur s'est produite, Veuillez réessayer", Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
                else {
                    Config.NetworkAlert(LoginActivity.this);
                }
            }
        });
    }
}
