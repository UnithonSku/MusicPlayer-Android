package com.edge.weather.musicpalyer;

import android.Manifest;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ListActivity {
    private static final int MY_PERMISSION_REQUEST_STORAGE=1;
    private static final int UPDATE_FREQUENCY=500;
    private static final int STEP_VALUE=4000;
    private MediaCursorAdapter mediaAdapter;
    private TextView selectedFile=null;
    private SeekBar seekBar=null;
    private MediaPlayer player;
    private ImageButton playButton=null;
    private ImageButton prevButton=null;
    private ImageButton nextButton=null;

    private boolean isStarted=true;
    private String currentFile="";
    private boolean isMoveingSeekBar=false;
    private final Handler handler=new Handler();
    private  final Runnable updatePositionRunnable=new Runnable() {
        @Override
        public void run() {
            updatePosition();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();

        selectedFile=(TextView)findViewById(R.id.selectedfile);
        seekBar=(SeekBar)findViewById(R.id.seekbar);
        playButton=(ImageButton)findViewById(R.id.play);
        prevButton=(ImageButton)findViewById(R.id.prev);
        nextButton=(ImageButton)findViewById(R.id.next);

        player=new MediaPlayer();
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopPlay();

            }
        });


        player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                return false;
            }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(isMoveingSeekBar){
                    player.seekTo(i);

                    Log.i("OnSeekBarChangeListener","onProgressChanged");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isMoveingSeekBar=true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isMoveingSeekBar=false;
            }
        });

        Cursor cursor=getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,null,null,null,null);

        if(null!=cursor){
            cursor.moveToFirst();
            mediaAdapter= new MediaCursorAdapter(this,R.layout.listitem,cursor);
            setListAdapter(mediaAdapter);

            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(player.isPlaying()){
                        handler.removeCallbacks(updatePositionRunnable);
                        player.pause();
                        playButton.setImageResource(android.R.drawable.ic_media_play);
                    }else{
                        if(isStarted){
                            player.start();
                            playButton.setImageResource(android.R.drawable.ic_media_pause);

                            updatePosition();
                        }else{
                            startPlay(currentFile);
                        }
                    }

                }
            });

            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int seekto=player.getCurrentPosition()+STEP_VALUE;

                    if(seekto>player.getDuration()){
                        seekto=player.getDuration();
                    }
                    player.pause();
                    player.seekTo(seekto);
                    player.start();
                }
            });

            prevButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int seekto=player.getCurrentPosition()+STEP_VALUE;

                    if(seekto<0){
                        seekto=0;
                    }
                    player.pause();
                    player.seekTo(seekto);
                    player.start();
                }
            });
        }



    }

    private void checkPermission(){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSION_REQUEST_STORAGE);
            }else{
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},MY_PERMISSION_REQUEST_STORAGE);
            }
        }else{

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Log.e("","동의선택");
                }else{
                    Toast.makeText(this,"권한사용 동의 해주세요.",Toast.LENGTH_SHORT).show();

                }
                return;
        }
    }

    private void startPlay(String file){
        Log.i("Selected",file);

        selectedFile.setText(file);
        seekBar.setProgress(0);

        player.stop();
        player.reset();

        try {
            player.setDataSource(file);
            player.prepare();
            player.start();
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }catch (IllegalStateException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

        seekBar.setMax(player.getDuration());
        playButton.setImageResource(android.R.drawable.ic_media_pause);

        updatePosition();

        isStarted=true;

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        super.onListItemClick(l, v, position, id);

        currentFile=(String)v.getTag();
        startPlay(currentFile);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updatePositionRunnable);
        player.stop();
        player.reset();
        player.release();
        player=null;
    }



    private void stopPlay(){
        player.stop();
        player.reset();
        playButton.setImageResource(android.R.drawable.ic_media_play);
        handler.removeCallbacks(updatePositionRunnable);
        seekBar.setProgress(0);

        isStarted=false;
    }

    private void updatePosition(){
        handler.removeCallbacks(updatePositionRunnable);

        seekBar.setProgress(player.getCurrentPosition());

        handler.postDelayed(updatePositionRunnable,UPDATE_FREQUENCY);
    }

    private class MediaCursorAdapter extends SimpleCursorAdapter{
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView title=(TextView)view.findViewById(R.id.title);
            TextView name=(TextView)view.findViewById(R.id.displayname);
            TextView duration=(TextView)view.findViewById(R.id.duration);

            name.setText(cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)));
            title.setText(cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)));
            long durationInMs=Long.parseLong(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)));

            double durationInMin=((double)durationInMs/1000.0)/60.0;

            durationInMin=new BigDecimal(Double.toString(durationInMin)).setScale(2,BigDecimal.ROUND_UP).doubleValue();

            duration.setText(""+durationInMin);

            view.setTag(cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA)));



        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater=LayoutInflater.from(context);
            View  v=inflater.inflate(R.layout.listitem,parent,false);

            bindView(v,context,cursor);

            return v;
        }

        public MediaCursorAdapter(Context context, int layout, Cursor cursor){
            super(context,layout,cursor,new String[]{MediaStore.MediaColumns.DISPLAY_NAME,MediaStore.MediaColumns.TITLE,MediaStore.Audio.AudioColumns.DURATION},
                    new int[]{R.id.displayname,R.id.title,R.id.duration});


        }
    }


}

