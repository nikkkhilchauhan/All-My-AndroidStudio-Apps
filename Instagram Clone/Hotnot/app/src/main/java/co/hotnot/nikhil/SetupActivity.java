package co.hotnot.nikhil;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SetupActivity extends AppCompatActivity {

    private CircleImageView imgProfile;
    private Uri mainImageURI=null;
    private Button btnSave;
    private EditText txtUname;
    private ProgressBar progressBar;
    private String user_id;
    private boolean isChanged=false;

    private StorageReference storageReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        firebaseAuth= FirebaseAuth.getInstance();
        storageReference= FirebaseStorage.getInstance().getReference();
        firebaseFirestore= FirebaseFirestore.getInstance();

        imgProfile= findViewById(R.id.imgProfile);
        btnSave= findViewById(R.id.btnSave);
        txtUname= findViewById(R.id.txtUname);
        progressBar= findViewById(R.id.progressbar);

        user_id = firebaseAuth.getCurrentUser().getUid();

        btnSave.setEnabled(false);

        firebaseFirestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if (task.isSuccessful()){
                    //Checks If data is already there i.e pic and user_id
                    if (task.getResult().exists()){
                         String name = task.getResult().getString("name");
                         String image = task.getResult().getString("image");
                         mainImageURI = Uri.parse(image);

                         txtUname.setText(name);
                         RequestOptions placeholderRequest = new RequestOptions();
                         placeholderRequest.placeholder(R.mipmap.def_profile);
                         Glide.with(SetupActivity.this).setDefaultRequestOptions(placeholderRequest).load(image).into(imgProfile);

                        //Toast.makeText(SetupActivity.this, "Data Already Exists", Toast.LENGTH_SHORT).show();

                    }else {

                        Toast.makeText(SetupActivity.this, "Data Does't Exists", Toast.LENGTH_SHORT).show();

                    }

                }else {
                    String error= task.getException().getMessage();
                    Toast.makeText(SetupActivity.this, "FireStore Retrieve Error:"+error, Toast.LENGTH_SHORT).show();
                }

                progressBar.setVisibility(View.INVISIBLE);
                btnSave.setEnabled(true);

            }
        });

        //Profile Image is Tapped
        imgProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Asking User Permissions
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                  if (ContextCompat.checkSelfPermission(SetupActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                      //Toast.makeText(SetupActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                      ActivityCompat.requestPermissions(SetupActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
                  }else{
                      //Toast.makeText(SetupActivity.this, "You already have permissions", Toast.LENGTH_SHORT).show();
                      //Sending user to crop activity
                      BringImagePicker();
                  }
                }else{
                      BringImagePicker();
                }
            }
        });

        //Save button is Tapped
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String user_name=txtUname.getText().toString();

                if(isChanged){
              if (!TextUtils.isEmpty(user_name) && mainImageURI!=null) {

                  user_id = firebaseAuth.getCurrentUser().getUid();

                  progressBar.setVisibility(View.VISIBLE);
                  StorageReference image_path = storageReference.child("Profile_images").child(user_id + ".jpg");
                  image_path.putFile(mainImageURI).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                      @Override
                      public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                          if (task.isSuccessful()) {
                              //declared below
                              storeFirestore(task, user_name);
                              //Toast.makeText(SetupActivity.this, "Upload Successful", Toast.LENGTH_SHORT).show();

                          } else {
                              String error = task.getException().getMessage();
                              Toast.makeText(SetupActivity.this, "Image Error:" + error, Toast.LENGTH_SHORT).show();
                              progressBar.setVisibility(View.INVISIBLE);
                          }
                      }
                  });
                }
              }else{
                    storeFirestore(null,user_name);
                }
            }
        });
    }

    //--------------------------------------------------------------------->>>>>>


    //called above for saving changes
    private void storeFirestore(@NonNull Task<UploadTask.TaskSnapshot>task, String user_name){
        Uri download_uri;

        if (task!=null){
            download_uri = task.getResult().getDownloadUrl();
        }else {
            download_uri = mainImageURI;
        }

        Map<String,String> userMap=new HashMap<>();
        userMap.put("name",user_name);
        userMap.put("image",download_uri.toString());
        firebaseFirestore.collection("Users").document(user_id).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if(task.isSuccessful()){

                    Toast.makeText(SetupActivity.this, "User Settings are updated", Toast.LENGTH_SHORT).show();
                    Intent mainIntent=new Intent(SetupActivity.this,MainActivity.class);
                    startActivity(mainIntent);
                    finish();

                }else{

                    String error= task.getException().getMessage();
                    Toast.makeText(SetupActivity.this, "FireStore Error:"+error, Toast.LENGTH_SHORT).show();

                }
                progressBar.setVisibility(View.INVISIBLE);
            }
        });

    }

    //Called Above
    private void BringImagePicker(){
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                //Setting up cropping ratio
                .setAspectRatio(1,1)
                .start(SetupActivity.this);
    }

    //After image is cropped from library activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                mainImageURI = result.getUri();
                //setting cropped image to imgProfile that is stored in mainImageURI
                imgProfile.setImageURI(mainImageURI);
                isChanged=true;
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    //--------------------------------------------------------------------->>>>>>

}
