package com.example.alias.client;



import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText edit_fname;
    private Button btn_upload;
    private Button btn_stop;
    private Button btn_pick;
    private ProgressBar pgbar;
    private TextView txt_result;

    private UploadHelper upHelper;
    private boolean flag = true;
    int currentlength=0;
    int previouslength=0;

    int index=0;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if( msg.what == 1)
            {
                TextView sp= (TextView) findViewById(R.id.speed);
                int spe=(currentlength-previouslength)/1024;
                String s = String.valueOf(spe);
                sp.setText("传输速度："+s+"KB/S");

                TextView ti= (TextView) findViewById(R.id.time);
                if(spe==0)return;
               int  Remainder=(pgbar.getMax()-currentlength)/(currentlength-previouslength);

                String t = String.valueOf(Remainder);
                ti.setText("预计剩余时间："+t+"S");
                previouslength=currentlength;
                return;
            }
            currentlength=msg.getData().getInt("length");
            pgbar.setProgress(currentlength);
            float num = (float) pgbar.getProgress() / (float) pgbar.getMax();
            int result = (int) (num * 100);
            txt_result.setText(result + "%");
            if (pgbar.getProgress() == pgbar.getMax()) {
                Toast.makeText(MainActivity.this, "上传成功", Toast.LENGTH_SHORT).show();
            }

        }
    };
    private Timer timer = new Timer();
    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            Message message = new Message();
            message.what = 1;
            handler.sendMessage(message);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        //延时1s，每隔500毫秒执行一次run方法
        upHelper = new UploadHelper(this);
    }

    private void bindViews() {
        edit_fname = (EditText) findViewById(R.id.edit_fname);
        btn_upload = (Button) findViewById(R.id.btn_upload);
        btn_stop = (Button) findViewById(R.id.btn_stop);
        pgbar = (ProgressBar) findViewById(R.id.pgbar);
        txt_result = (TextView) findViewById(R.id.txt_result);
        btn_pick=(Button) findViewById(R.id.btn_pick);

        btn_upload.setOnClickListener(this);
        btn_stop.setOnClickListener(this);
        btn_pick.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_upload:
                String filename = edit_fname.getText().toString();
                flag = true;
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    File file = new File(Environment.getExternalStorageDirectory(), filename);
                    if (file.exists()) {
                        currentlength=0;
                        previouslength=0;
                        pgbar.setMax((int) file.length());
                        uploadFile(file);
                        if(index==0)
                        {
                            timer.schedule(timerTask,0,1000);
                            index=1;
                        }

                    } else {
                        Toast.makeText(MainActivity.this, "文件名不存在！", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "SD卡不存在或者不可用！", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_stop:
                flag = false;
                break;

            case R.id.btn_pick:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");//设置类型
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent,1);

                break;

        }
    }

    private void uploadFile(final File file) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String sourceid = upHelper.getBindId(file);
                    Socket socket = new Socket("172.19.53.120", 10000);
                    OutputStream outStream = socket.getOutputStream();
                    String head = "Content-Length=" + file.length() + ";filename=" + file.getName()
                            + ";sourceid=" + (sourceid != null ? sourceid : "") + "\r\n";
                    outStream.write(head.getBytes());

                    PushbackInputStream inStream = new PushbackInputStream(socket.getInputStream());
                    String response = StreamTool.readLine(inStream);
                    String[] items = response.split(";");
                        String responseSourceid = items[0].substring(items[0].indexOf("=") + 1);
                        String position = items[1].substring(items[1].indexOf("=") + 1);
                        if (sourceid == null) {//如果是第一次上传文件，在数据库中不存在该文件所绑定的资源id
                            upHelper.save(responseSourceid, file);
                    }
                    RandomAccessFile fileOutStream = new RandomAccessFile(file, "r");
                    fileOutStream.seek(Integer.valueOf(position));
                    byte[] buffer = new byte[1024];
                    int len = -1;
                    int length = Integer.valueOf(position);
                    while (flag && (len = fileOutStream.read(buffer)) != -1) {
                        outStream.write(buffer, 0, len);
                        length += len;//累加已经上传的数据长度
                        Message msg = new Message();
                        msg.getData().putInt("length", length);
                        msg.what=0;
                        handler.sendMessage(msg);
                    }
                    if (length == file.length()) upHelper.delete(file);
                    fileOutStream.close();
                    outStream.close();
                    inStream.close();
                    socket.close();
                } catch (Exception e) {

                    System.out.println();

                }
            }
        }).start();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {//是否选择，没选择就不会继续
            Uri uri = data.getData();//得到uri，后面就是将uri转化成file的过程。
           String _uri=uri.toString();

                  String[] strarray=_uri.split("A",2);
              String name=strarray[1];
            edit_fname = (EditText) findViewById(R.id.edit_fname);
            edit_fname.setText(name);

            Toast.makeText(MainActivity.this, "文件选取成功", Toast.LENGTH_SHORT).show();

            pgbar.setProgress(0);
            txt_result.setText("0%");

        }
    }

}