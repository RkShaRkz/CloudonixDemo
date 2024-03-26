package com.example.cloudonix;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
        btnSend.setOnClickListener(getButtonClickListener());
    }

    private View.OnClickListener getButtonClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("SHARK", "button clicked!");
                v.setClickable(false);
                String address = getifaddrs();
                SendRequest request = new SendRequest(new SendRequest.Params(address));
                AsyncTask<Void, Void, SendRequest.ResponseResult> networkingTask = new AsyncTask<Void, Void, SendRequest.ResponseResult>() {
                    CountDownTimer timer = new CountDownTimer(3000, 100) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            // we dont care about this
                        }

                        @Override
                        public void onFinish() {
                            // when 3 seconds are up, we need to raise a overlay
                            //TODO overlay
                        }
                    };

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        timer.start();
                    }

                    @Override
                    protected SendRequest.ResponseResult doInBackground(Void... voids) {
                        SendRequest.ResponseResult result = request.sendRequest();
                        return result;
                    }

                    @Override
                    protected void onPostExecute(SendRequest.ResponseResult responseResult) {
                        super.onPostExecute(responseResult);
                        v.setClickable(true);
                        //TODO in case the overlay was opened, close it here
                        Toast.makeText(getBaseContext(), "Result was success ? "+responseResult.isSuccess(), Toast.LENGTH_SHORT).show();
                        if (responseResult.isSuccess()) {
                            Log.d("SHARK", "responseBody was: " + responseResult.getResponseBody());
                        } else {
                            Log.d("SHARK", "request failed due to: "+responseResult.getFaultReason());
                        }
                    }
                }.execute();

            }
        };
    }

    /**
     * A native method that is implemented by the 'cloudonix' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public native String getifaddrs();
}
