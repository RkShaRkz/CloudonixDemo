package com.example.cloudonix;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.cloudonix.databinding.ActivityMainBinding;
import com.example.cloudonix.network.SendRequest;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'cloudonix' library on application startup.
    static {
        System.loadLibrary("cloudonix");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(getifaddrs());

        Button btnSend = binding.buttonSendRequest;
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = getifaddrs();
                SendRequest request = new SendRequest(new SendRequest.Params(address));
                AsyncTask<Void, Void, SendRequest.ResponseResult> networkingTask = new AsyncTask<Void, Void, SendRequest.ResponseResult>() {
                    @Override
                    protected SendRequest.ResponseResult doInBackground(Void... voids) {
                        SendRequest.ResponseResult result = request.sendRequest();
                        return result;
                    }
                }.execute();

            }
        });
    }

    /**
     * A native method that is implemented by the 'cloudonix' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public native String getifaddrs();
}
