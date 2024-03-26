package com.example.cloudonix.network;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SendRequest {

    public static final class JSON_KEYS {
        public static final String JSON_FIELD_NAT = "nat";
    }

    private final Params params;

    public SendRequest(Params params) {
        this.params = params;
    }

    public ResponseResult sendRequest() {
        ResponseResult retVal = null;
        try {
            // Send POST request
            retVal = sendPostRequest(params.jsonString);
            if (retVal.isSuccess()) {
                System.out.println("Response succesful! Is NAT: " + retVal.isResponseOK());
            } else {
                System.out.println("Response was unsuccessful and request was failed to be sent");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return retVal;
    }

    private ResponseResult sendPostRequest(String jsonBody) throws IOException {
        // URL of the server
        String url = "https://s7om3fdgbt7lcvqdnxitjmtiim0uczux.lambda-url.us-east-2.on.aws/";

        // Create URL object
        URL obj = new URL(url);

        // Create connection
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // Set request method
        con.setRequestMethod("POST");

        // Set request headers
        con.setRequestProperty("Content-Type", "application/json");

        // Enable input and output streams
        con.setDoOutput(true);
        con.setDoInput(true);

        // Write JSON data to the output stream
        try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.writeBytes(jsonBody);
            wr.flush();
        }

        // Get response code
        int responseCode = con.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Read response body
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                // Parse JSON response
                String jsonResponse = response.toString();
                boolean isResponseOK = false;
                // Convert to actual json
                try {
                    JSONObject json = new JSONObject(jsonResponse);
                    isResponseOK = json.optBoolean(JSON_KEYS.JSON_FIELD_NAT, false);
                } catch (JSONException e) {
                    Log.wtf("SHARK", "Exception happened during sending of POST request! Exception " + e);
                }
                // Assuming JSON response looks like {"nat":true} or {"nat":false}
                return new ResponseResult(true, isResponseOK);
            }
        } else {
            // Handle HTTP error response
            System.out.println("HTTP error: " + responseCode);
            return new ResponseResult(false, null);
        }
    }

    public static class Params {
        private final String jsonString;

        private static final String JSON_FIELD_ADDRESS = "address";

        public Params(String address) {
            // Generate a map of params
            Map<String, String> paramMap = new HashMap<>();

            paramMap.put(JSON_FIELD_ADDRESS, address);
            jsonString = mapToJson(paramMap);
        }

        private String mapToJson(Map<String, String> map) {
            StringBuilder json = new StringBuilder();
            json.append("{");
            for (Map.Entry<String, String> entry : map.entrySet()) {
                json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\",");
            }
            json.deleteCharAt(json.length() - 1); // Remove trailing comma
            json.append("}");
            return json.toString();
        }
    }

    public static class ResponseResult {
        private final boolean sentOK;
        private final Boolean responseOK;

        public ResponseResult(boolean sentOK, Boolean responseOK) {
            this.sentOK = sentOK;
            this.responseOK = responseOK;
        }

        public boolean isSuccess() {
            return isSentOK();
        }

        /**
         * There are no responses for failed requests, so always check {@link ResponseResult#isSuccess()}
         * before calling this.
         * <p>
         * If the {@link #isSuccess()} returns <i>false</i>, this method throws an {@link IllegalStateException}
         * Otherwise, it returns whether the response was OK or not.
         *
         * @return whether the {@link SendRequest} contained an OK response or not.
         */
        public boolean isResponseOK() {
            if (!sentOK) {
                throw new IllegalStateException("Cannot get response from a failed response");
            } else {
                return responseOK;
            }
        }

        public boolean isSentOK() {
            return sentOK;
        }
    }
}
