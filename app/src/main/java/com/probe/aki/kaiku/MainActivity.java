package com.probe.aki.kaiku;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final int RECORDER_SAMPLERATE = 32768;

    private int buffer_size = 1000;

    private Thread recordingThread = null;
    private AudioRecord recorder = null;
    private boolean isRecording = false;

    private AudioTrack at;

    private short sData[];
    private SeekBar simpleSeekBar;
    private ProgressBar progressBar;
    private Button send;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        simpleSeekBar = (SeekBar)findViewById(R.id.simpleSeekBar);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        textView = (TextView)findViewById(R.id.textView);
        send = (Button) findViewById(R.id.button);
        sData = new short[buffer_size];

        if(savedInstanceState != null) {
            isRecording = savedInstanceState.getBoolean("recordon");
            buffer_size = savedInstanceState.getInt("buffer_size");
            if(isRecording) {
                send.setText("Stop");
                simpleSeekBar.setEnabled(false);
                sData = new short[buffer_size];
                startRecording();
                textView.setText((1000 * buffer_size / RECORDER_SAMPLERATE) + " ms");
            }
        }

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isRecording) {
                    send.setText("Start");
                    stopRecording();
                    progressBar.setProgress(0);
                } else {
                    send.setText("Stop");
                    startRecording();
                }
                simpleSeekBar.setEnabled(!isRecording);
            }
        });

        simpleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                buffer_size = progressChangedValue;
                sData = new short[buffer_size];
                textView.setText((1000 * buffer_size / RECORDER_SAMPLERATE) + " ms");
            }
        });
    }

    private void startRecording() {

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, buffer_size*2);

        recorder.startRecording();

        at = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE,
                AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,
                buffer_size*2, AudioTrack.MODE_STREAM);
        at.play();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {
            public void run() {
                while (isRecording) {
                    recorder.read(sData, 0, buffer_size);
                    at.write(sData, 0, buffer_size);
                    short max_value = 0;
                    for(int i=0;i<buffer_size;i++) {
                        if(sData[i] > max_value)
                            max_value = sData[i];
                    }
                    progressBar.setProgress(max_value/128);
                }
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder && isRecording) {
            isRecording = false;

            recorder.stop();
            recorder.release();

            at.stop();
            at.release();

            recorder = null;
            recordingThread = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("recordon",isRecording);
        savedInstanceState.putInt("buffer_size",buffer_size);
    }

    protected void onStop () {
        super.onStop();
        stopRecording();
    }
}
