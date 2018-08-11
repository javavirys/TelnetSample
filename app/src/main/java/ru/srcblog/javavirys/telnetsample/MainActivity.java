package ru.srcblog.javavirys.telnetsample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetNotificationHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements TelnetNotificationHandler {

    TelnetClient tc = new TelnetClient();
    boolean connected = false;

    String cmd = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tc.registerNotifHandler(this);


        new T1(5).start();

        findViewById(R.id.button1).setOnClickListener(v->{
            if(connected) {
                cmd = ((EditText)findViewById(R.id.write_data)).getText().toString();
                ((EditText)findViewById(R.id.write_data)).setText("");
                new T1(4).start();
            }
        });

        ((EditText)findViewById(R.id.write_data)).setOnKeyListener((view, i, keyEvent) -> {
            if(i == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN){
                if(connected) {
                    cmd = ((EditText)findViewById(R.id.write_data)).getText().toString();
                    ((EditText)findViewById(R.id.write_data)).setText("");
                    new T1(4).start();
                }
                return false;
            }
            return false;
        });

    }

    @Override
    protected void onDestroy() {
        new T1(3).start();
        tc.unregisterNotifHandler();
        super.onDestroy(); }

    @Override
    public void receivedNegotiation(int negotiation_code, int option_code) {
        System.out.println("negotiation_code: " + negotiation_code + " option_code: " + option_code);
        //TelnetNotificationHandler.RECEIVED_WONT

    }

    class T1 extends Thread {

        int state;

        public T1(int state) {
            this.state = state;
        }

        @Override
        public void run() {
            switch (state){
                case 1: // connect
                    connect();
                    break;
                case 2: // read
                    read();
                    break;
                case 3: // disconnect
                    disconnect();
                    break;
                case 4:
                    write();
                    break;
                case 5:
                    connect();
                    read();
                    break;
            }
        }

        void connect(){
            try {
                tc.connect("192.168.1.1");
                connected = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void read(){
            InputStream instr = tc.getInputStream();

            try
            {
                byte[] buff = new byte[1024];
                int ret_read;
                while (connected && (ret_read = instr.read(buff)) != -1){
                    StringBuffer sBuf = new StringBuffer();
                    sBuf.append(new String(buff,0,ret_read));
                    System.out.println(sBuf.toString());
                    runOnUiThread(() -> {
                        EditText edit = findViewById(R.id.editText);
                        if(edit.getText().length() > 10000) {
                            edit.getText().delete(0,edit.getText().length() - 5000);
                        }
                        edit.getText().append(sBuf.toString());
                    });
                }
            }
            catch (IOException e)
            {
                //e.printStackTrace();
                System.err.println("Exception while reading socket:" + e.getMessage());
            }
        }

        void write() {
            OutputStream os = tc.getOutputStream();
            try {
                os.write((cmd + '\r').getBytes());
                os.flush();
                //os.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        void disconnect(){
            connected = false;
            try {
                tc.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
