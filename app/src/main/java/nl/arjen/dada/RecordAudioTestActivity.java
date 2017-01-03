package nl.arjen.dada;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RecordAudioTestActivity extends Activity {

    private static final String LOG_TAG = "RecordAudioTestActivity";
    private static int ALL_PERMISSIONS = 0;
    private static String mFileName = null;

    private Button btnRecord;
    private Button btnPlay;

    private boolean recording = false;
    private boolean playing = false;
    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;

    public RecordAudioTestActivity() {
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/audiorecordtest.3gp";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_audio_test);

        if (shouldShowRequestPermissionRationale()) {
            new AlertDialog.Builder(this)
                    .setTitle("Rechten")
                    .setMessage("Om geluid op te kunnen nemen heeft DaDa toegang nodig tot de microfoon en opslag.")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            checkPermissions();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();


        }else{
            checkPermissions();
        }

        btnRecord = (Button) this.findViewById(R.id.btn_record);
        btnPlay = (Button) this.findViewById(R.id.btn_play);

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (recording) {
                    stopRecording();
                } else {
                    startRecording();
                }

                recording = !recording;
                btnRecord.setText(recording ? getString(R.string.btn_stop) : getString(R.string.btn_record));
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playing) {
                    stopPlaying();
                } else {
                    startPlaying();
                }

                playing = !playing;
                btnPlay.setText(playing ? getString(R.string.btn_stop) : getString(R.string.btn_play));
            }
        });
    }

    private boolean shouldShowRequestPermissionRationale(){
        return ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO) || ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();

        if(!hasPermission(Manifest.permission.RECORD_AUDIO))
            permissions.add(Manifest.permission.RECORD_AUDIO);

        if(!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        ActivityCompat.requestPermissions(this,
                permissions.toArray(new String[permissions.size()]),
                ALL_PERMISSIONS);
    }

    private boolean hasPermission(String permission) {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                permission);

        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == ALL_PERMISSIONS) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length == 0 || hasDeniedPermissions(grantResults)) {
                new AlertDialog.Builder(this)
                        .setTitle("Rechten")
                        .setMessage("Je hebt DaDa niet alle rechten gegeven, hierdoor is het niet mogelijk om audio op te nemen.")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                               //TODO terug naar menu of zo
                            }
                        })
                        .setNegativeButton(R.string.btn_retry_persmissions, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkPermissions();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            } else {
                //permissions oke
            }
            return;
        }

    }

    private boolean hasDeniedPermissions(int[] grantResults){
        for(int i=0; i< grantResults.length ; i++){
            if(grantResults[i] == PackageManager.PERMISSION_DENIED)
                return true;
        }
        return false;
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }
}
