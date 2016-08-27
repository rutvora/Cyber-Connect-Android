package helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.harsu.developer.bias.LoginActivity;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT;

/**
 * Created by harsu on 27-08-2016.
 */

public class LoginController {

    static String loginURL = "http://172.16.0.30:8090/login.xml";


    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        SupplicantState supplicantState = wifiInfo.getSupplicantState();
        if (supplicantState == SupplicantState.COMPLETED) {
            if (netInfo != null && netInfo.isConnected())
                return true;

        }
        return false;
        /*if (netInfo != null && netInfo.isConnected()) {
            try {
                Runtime runtime = Runtime.getRuntime();
                Process proc = runtime.exec("ping -c 1 " + "google.com");
                proc.waitFor();
                int exitCode = proc.exitValue();
                if (exitCode == 0) {
                    Log.d("Ping", "Ping successful!");
                    return true;
                } else {
                    Log.d("Ping", "Ping unsuccessful.");
                    return false;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;*/
    }

    public static void login(Context context, @Nullable final ConnectionListener listener) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences(LoginActivity.CREDENTIALS, Context.MODE_PRIVATE);

        login(sharedPreferences.getString("username", ""), sharedPreferences.getString("password", ""), listener);
    }

    public static boolean containsData(Context context) {

        final SharedPreferences sharedPreferences = context.getSharedPreferences(LoginActivity.CREDENTIALS, Context.MODE_PRIVATE);
        return !sharedPreferences.getString("username", "").isEmpty();
    }

    public static void login(final String username, final String password, final ConnectionListener listener) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, loginURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                //password incorrect
                //<?xml version='1.0' ?><requestresponse><status><![CDATA[LOGIN]]></status><message><![CDATA[The system could not log you on. Make sure your password is correct]]></message><logoutmessage><![CDATA[You have successfully logged off]]></logoutmessage><state><![CDATA[]]></state></requestresponse>

                //success
                //<?xml version='1.0' ?><requestresponse><status><![CDATA[LIVE]]></status><message><![CDATA[You have successfully logged in]]></message><logoutmessage><![CDATA[You have successfully logged off]]></logoutmessage><state><![CDATA[]]></state></requestresponse>

                //data limit error contains
                //Your data transfer has been exceeded, Please contact the administrator

                Log.e("response", s);
                if (listener != null) {
                    if (s.contains("You have successfully logged in")) {
                        listener.success();
                    } else if (s.contains("Your data transfer has been exceeded")) {
                        listener.error(Error.DATA_LIMIT);
                    } else if (s.contains("Your credentials were incorrect")) {
                        listener.error(Error.WRONG_CREDENTIALS);
                    } else if (s.contains("Server is not responding.")) {
                        listener.error(Error.SERVER_ERRROR);
                    }
                }


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

                if (listener != null) {
                    listener.error(Error.WRONG_WIFI);
                }
            }
        }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> b = new HashMap<>();
                b.put("Content-Type", "application/x-www-form-urlencoded");
                return b;
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {

                Map<String, String> params = new HashMap<String, String>();
                params.put("mode", "191");
                params.put("username", username);
                params.put("password", password);
                params.put("a", String.valueOf(Calendar.getInstance().getTimeInMillis()));
                params.put("producttype", "0");

                return params;

            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(2000, 0, DEFAULT_BACKOFF_MULT));
        app.VolleySingleton.getInstance().getRequestQueue().add(stringRequest);

    }


    public interface ConnectionListener {
        public void success();

        public void error(int error);
    }

    public interface Error {
        int WRONG_WIFI = 0;
        int WRONG_CREDENTIALS = 1;
        int DATA_LIMIT = 2;
        int SERVER_ERRROR = 3;
    }
}
