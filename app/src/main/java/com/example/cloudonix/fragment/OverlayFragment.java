package com.example.cloudonix.fragment;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.cloudonix.R;
import com.example.cloudonix.databinding.FragmentOverlayBinding;
import com.example.cloudonix.network.SendRequest;

public class OverlayFragment extends Fragment {
    public static final long TIME_TO_STAY_IN_MS = 3000L;

    private FragmentOverlayBinding binding;

    private TextView pleaseWait;
    private ProgressBar progressBar;
    private ImageView imageResult;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOverlayBinding.inflate(getLayoutInflater());

        pleaseWait = binding.textPleasewait;
        progressBar = binding.progressbar;
        imageResult = binding.imageResult;

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void showResultAndDismiss(SendRequest.ResponseResult responseResult) {
        // hide the please wait and progress bar
        pleaseWait.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        imageResult.setVisibility(View.VISIBLE);
        imageResult.setBackgroundResource(responseResult.isSuccess() ? R.drawable.green_success_checkmark : R.drawable.red_unsuccess_x);

        // wait for 1 second and then disappear
        CountDownTimer timer = new CountDownTimer(TIME_TO_STAY_IN_MS, 100) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                if (isAdded()) {
                    FragmentManager fragmentManager = getParentFragmentManager();
//                fragmentManager.popBackStack();
                    // Similarly, lets also pop with the same idea.
                    fragmentManager.popBackStackImmediate();
                }
            }
        }.start();
    }
}
