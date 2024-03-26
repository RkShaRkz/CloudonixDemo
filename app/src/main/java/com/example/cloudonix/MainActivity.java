package com.example.cloudonix;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cloudonix.databinding.ActivityMainBinding;
import com.example.cloudonix.fragment.OverlayFragment;
import com.example.cloudonix.network.SendRequest;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'cloudonix' library on application startup.
    static {
        System.loadLibrary("cloudonix");
    }

    private ActivityMainBinding binding;

    // Prevent caching of this value by setting to volatile
    private volatile OverlayFragment overlayFragment = null;

    private TextView resultText;
    private ImageView resultImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(getifaddrs());

        resultText = binding.resultText;
        resultImage = binding.resultImage;

        Button btnSend = binding.buttonSendRequest;
        btnSend.setOnClickListener(getButtonClickListener());

        Log.d("SHARK", "List of all addresses: "+getifaddrsAll());
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
                            showOverlay();
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
                        // Cancel the timer if it's still ticking
                        timer.cancel();

                        // The overlay fragment was made to be a 'fire and forget' kind of overlay, so just
                        // show the result and get rid of it. It'll pop itself from the stack after a second.
                        if (overlayFragment != null) {
                            overlayFragment.showResultAndDismiss(responseResult);
                        }
                        showResult(responseResult);
                        overlayFragment = null;

                        if (responseResult.isSuccess()) {
                            Log.d("SHARK", "responseBody was: " + responseResult.getResponseBody());
                        } else {
                            Log.d("SHARK", "request failed due to: " + responseResult.getFaultReason());
                        }
                    }
                }.execute();
            }
        };
    }

    private void showOverlay() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        overlayFragment = new OverlayFragment();
        fragmentTransaction
                .add(R.id.fragment_container, overlayFragment)
                .setReorderingAllowed(true)
                .addToBackStack(OverlayFragment.class.getCanonicalName())
//                .commitNow();
                .commit();
        // In case the user backs out to Home, this commit can actually cause problems... so let's use
        // a safer alternative that doesn't care if the overlay shows up or not.
    }

    private void showResult(SendRequest.ResponseResult result) {
        if (result.isSuccess()) {
            // Check whether result was ok, and act accordingly
            StringBuilder sb = new StringBuilder();
            sb.append(getifaddrs()).append("\n").append(result.getResponseBody());

            int drawableResId;
            if (result.isResponseOK()) {
                drawableResId = R.drawable.green_success_checkmark;
            } else {
                drawableResId = R.drawable.red_unsuccess_x;
            }

            resultText.setText(sb.toString());
            resultImage.setBackgroundResource(drawableResId);
        } else {
            // just say that there was a failure in sending the request
            resultText.setText("Whoops, something went wrong!");
            resultImage.setBackgroundResource(R.drawable.red_unsuccess_x);
        }
        resultText.setVisibility(View.VISIBLE);
        resultImage.setVisibility(View.VISIBLE);
    }

    /**
     * A native method that is implemented by the 'cloudonix' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public native String getifaddrs();

    public native List<String> getifaddrsAll();
}
