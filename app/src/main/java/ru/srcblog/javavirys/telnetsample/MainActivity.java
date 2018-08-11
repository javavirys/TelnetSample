package ru.srcblog.javavirys.telnetsample;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetNotificationHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements TelnetNotificationHandler,Runnable {

    public static final int STATE_IDLE = -1;
    public static final int STATE_CONNECT = 1;
    public static final int STATE_DISCONNECT = 3;

    public static final int STATE_READ = 2;
    public static final int STATE_WRITE = 4;
    public static final int STATE_CONNECT_AND_READ = 5;


    int state = STATE_IDLE;

    TelnetClient tc = new TelnetClient();
    boolean connected = false;

    String ip = "";
    String cmd = "";

    Thread tConnect = null,tDisconnect = null,tWrite = null,tRead = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        showHelp();

        findViewById(R.id.button1).setOnClickListener(v->sendCommand());

        findViewById(R.id.write_data).setOnKeyListener((view, i, keyEvent) -> {
            if(i == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN){
                sendCommand();
                return false;
            }
            return false;
        });

        findViewById(R.id.editText).setOnKeyListener((view, i, keyEvent) -> true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:
                Uri address = Uri.parse("http://srcblog.ru");
                Intent openlinkIntent = new Intent(Intent.ACTION_VIEW, address);
                startActivity(openlinkIntent);
                break;
            default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    void sendCommand(){
        cmd = ((EditText)findViewById(R.id.write_data)).getText().toString();
        ((EditText)findViewById(R.id.write_data)).setText("");

        if(cmd.equalsIgnoreCase("jvclose") && connected){
            ((EditText)findViewById(R.id.write_data)).getText().append("\nCommand jvClose\n");
            state = STATE_DISCONNECT;
            tDisconnect = new Thread(this);
            tDisconnect.start();
            return;
        } else if(cmd.toLowerCase().startsWith("jvcon") && !connected){
            String[] arr = cmd.split(" ");
            System.out.println("jvCon: " + Arrays.toString(arr));
            if(arr.length > 1){
                ip = arr[1];
                state = STATE_CONNECT_AND_READ;
                tConnect = new Thread(this);
                tConnect.start();
            }
            return;
        } else if(!connected || cmd.equalsIgnoreCase("jvhelp")){
            showHelp();
            return;
        }

        if(connected) {
            state = STATE_WRITE;
            if(tWrite != null) {
                try {
                    tWrite.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            tWrite = new Thread(this);
            tWrite.start();
        }
    }

    void showHelp(){
        EditText edit = findViewById(R.id.editText);
        edit.append("\n\t\t\t\t\t\t------ HELP ------\n");
        edit.append("jvHelp - showed help\n");
        edit.append("jvCon [ip] - connect to ip\n");
        edit.append("jvClose - disconnect telnet client\n");
        edit.append("My app: https://play.google.com/store/apps/details?id=com.javavirys.satinstructor\n");
        edit.append("My site: https://srcblog.ru\n");
        edit.append("\n");
    }

    @Override
    public void receivedNegotiation(int negotiation_code, int option_code) {
        System.out.println("negotiation_code: " + negotiation_code + " option_code: " + option_code);
        //TelnetNotificationHandler.RECEIVED_WONT

    }

    @Override
    public void run() {
        switch (state){
            case STATE_CONNECT: // connect
                connect();
                break;
            case STATE_READ: // read
                read();
                break;
            case STATE_DISCONNECT: // disconnect
                disconnect();
                break;
            case STATE_WRITE:
                write();
                break;
            case STATE_CONNECT_AND_READ:
                connect();
                read();
                break;
        }
    }

    void connect(){
        tc.registerNotifHandler(this);
        try {
            tc.connect(ip);
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
        tc.unregisterNotifHandler();
    }
}
