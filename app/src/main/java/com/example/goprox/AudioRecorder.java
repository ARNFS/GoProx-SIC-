package com.example.goprox;

import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;

public class AudioRecorder {
    private MediaRecorder recorder;
    private String outputFile;
    private boolean isRecording = false;
    private Handler handler;
    private AmplitudeListener listener;

    public interface AmplitudeListener {
        void onAmplitude(int amplitude);
    }

    public AudioRecorder() {
        handler = new Handler(Looper.getMainLooper());
    }

    public void startRecording(String filePath, AmplitudeListener listener) throws IOException {
        this.listener = listener;
        outputFile = filePath;

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioSamplingRate(44100);
        recorder.setAudioEncodingBitRate(128000);
        recorder.setOutputFile(filePath);

        recorder.prepare();
        recorder.start();
        isRecording = true;

        startAmplitudeMonitoring();
    }

    private void startAmplitudeMonitoring() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRecording && recorder != null && listener != null) {
                    try {
                        int amplitude = recorder.getMaxAmplitude();
                        listener.onAmplitude(amplitude);
                    } catch (Exception e) {
                        Log.e("AudioRecorder", "Error getting amplitude", e);
                    }
                    handler.postDelayed(this, 100);
                }
            }
        }, 100);
    }

    public void stopRecording() {
        isRecording = false;
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (Exception e) {
                Log.e("AudioRecorder", "Error stopping recorder", e);
            }
            recorder.release();
            recorder = null;
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    public boolean isRecording() {
        return isRecording;
    }
}