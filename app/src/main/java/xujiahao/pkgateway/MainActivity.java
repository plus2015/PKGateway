package xujiahao.pkgateway;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;

// use date binding lib to access any View to be a replacement of GetViewById system.
// warning: please use  Upper CamelCase
import xujiahao.pkgateway.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    //  enum =============================================
    //  the operator types we have supported.
    public enum OperatorType {CONNECT_FREE,CONNECT_GLOBAL, DISCONNECT}
    public enum GatewayStatus{UNCLEAR,CONNECTED_FREE,CONNECTED_GLOBAL,DISCONNECTED,CONNECTING};

    // const ============================================
    public static final String HTTP_CODE = "GB2312";

    // var ==============================================
    private String studentID;
    private String password;

    ActivityMainBinding B;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        B = DataBindingUtil.setContentView(this, R.layout.activity_main);
        // update studentID and password
        SharedPreferences preferences = getSharedPreferences(KEY.MAIN, MODE_PRIVATE);
        studentID = preferences.getString(KEY.STUDENT_ID,"");
        password = preferences.getString(KEY.PASSWORD,"");
        if(studentID.isEmpty()&&password.isEmpty()){
            toLoginActivity();
        }
    }

    public void clickFreeIP(View view) {
        operator(OperatorType.CONNECT_FREE);
    }

    public void clickGlobalIP(View view) {
        operator(OperatorType.CONNECT_GLOBAL);
    }

    public void clickBreakAll(View view) {
        operator(OperatorType.DISCONNECT);
    }

    public void clickSetting(View view) {
        toLoginActivity();
    }

    private void toLoginActivity(){
        // intend to LoginActivity
        Intent intend = new Intent(this, LoginActivity.class);
        startActivity(intend);
    }

    public void operator(OperatorType type){
        // the function to do real activity about gateway.
        // XJH: get this API from
        // https://github.com/pku-birdmen/pkuwebmaster.git
        // the var to format this string: studentID, password, range, operation.
        final String urlTemplate = "https://its.pku.edu.cn:5428/ipgatewayofpku?uid=%s&password=%s&timeout=1&range=%d&operation=%s";

        // check network
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            B.tvInfo.append(getString(R.string.noNetwork));
            return;
        }
        // make GET request text.
        int range = (type == OperatorType.CONNECT_GLOBAL) ? 1:2;
        String operation =(type == OperatorType.DISCONNECT) ? "disconnectall" : "connect";
        String url = String.format(Locale.getDefault(),urlTemplate,studentID,password,range,operation);

        new DownloadWebpageTask().execute(url);
    }

    public void show(String str){
        // append text in tvInfo
        B.tvInfo.append("\n"+str);
    }

    public class DownloadWebpageTask extends AsyncTask<String, Void, String> {
        // XJH: this function and all of the functions it call is copied from
        // https://developer.android.com/training/basics/network-ops/connecting.html
        // Uses AsyncTask to create a task away from the main UI thread. This task takes a
        // URL string and uses it to create an HttpUrlConnection. Once the connection
        // has been established, the AsyncTask downloads the contents of the webpage as
        // an InputStream. Finally, the InputStream is converted into a string, which is
        // displayed in the UI by the AsyncTask's onPostExecute method.
        private final int len = 4096;
        private final static String LOG_TAG_NET = "NET";
        @Override
        protected String doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            try {
                Log.d(LOG_TAG_NET,"GET " + urls[0]);
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                return getString(R.string.netError);
            }
        }

        // onPostExecute handle the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            HashMap<String,String> connectInfo = getInfoFromResponse(result);
            for(String key : connectInfo.keySet()){
                show(key+" : "+connectInfo.get(key));
            }
        }
        private HashMap<String, String> getInfoFromResponse(String response){
            // get info from the http page.
            String TAG = "GET_INFO_FROM_RESPONSE";
            Log.d(TAG,response);
            String[] info = response.split("<!--IPGWCLIENT_START ")[1].split(" IPGWCLIENT_END-->")[0].split(" ");

            HashMap<String,String> connectInfo = new HashMap<>();
            for (String anInfo : info) {
                String[] tmp = anInfo.split("=");
                if(tmp.length==2) {
                    tmp[0] = tmp[0].trim();
                    tmp[1] = tmp[1].trim();
                    if (!tmp[0].isEmpty() && !tmp[1].isEmpty()) {
                        connectInfo.put(tmp[0], tmp[1]);
                    }
                }
            }
            return connectInfo;
        }
        private String downloadUrl(String myUrl) throws IOException {
            // Given a URL, establishes an HttpUrlConnection and retrieves
            // the web page content as a InputStream, which it returns as
            // a string.
            InputStream is = null;
            // Only display the first 500 characters of the retrieved
            // web page content.

            try {
                URL url = new URL(myUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                int response = conn.getResponseCode();
                Log.d("NET", "The response is: " + response);
                is = conn.getInputStream();

                // Convert the InputStream into a string
                return readIt(is, len);

                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }

        public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
            // Reads an InputStream and converts it to a String.
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream,HTTP_CODE));
            StringBuilder result = new StringBuilder();
            String line;
            while((line = reader.readLine())!=null) result.append(line);
            return result.toString();
        }
    }
}
