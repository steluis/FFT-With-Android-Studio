package com.example.audiofourier;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final int RECORDER_BPP=16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV=".wav";
    private static final String AUDIO_RECORDER_FOLDER="AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE="record_temp.raw";
    private static final int RECORDER_SAMPLERATE=44100;
    private static final int RECORDER_CHANNELS= AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING= AudioFormat.ENCODING_PCM_16BIT;
    short[] audioData;

    private AudioRecord recorder = null;  // Classe che gestisce le risorse audio
    private int bufferSize = 0 ;
    private Thread recordingThread = null; // Processo per la registrazione
    private boolean isRecording = false;
    Complex[] fftTempArray; //Array di Complex che è una classe con le proprietà
                            // Real e Img
    Complex[] fftArray;
    int[] bufferData;
    int bytesRecorded;
    double mPeakPos;

    final int mNumberOfFFTPoints = 8192;
    double[] absNormalizedSignal = new double[mNumberOfFFTPoints/2];
//    byte copiadati[] = new byte[1024];

    private LineChart mChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setButtonHandlers(); //Richiama il metodo che definisce i bottoni
        enableButtons(false); //Abilita/Disabilita i bottoni

        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING)*3;

        audioData = new short [bufferSize]; // Array nel quale sono memorizzati i dati PCM

        requestRecordAudioPermission();
        requestRecordFilePermission();

        mChart = findViewById(R.id.chart);
        mChart.setTouchEnabled(true);
        mChart.setPinchZoom(true);
        com.example.nineleaps.mpchartexample.MyMarkerView mv = new com.example.nineleaps.mpchartexample.MyMarkerView(getApplicationContext(), R.layout.custom_marker_view);
        mv.setChartView(mChart);
        mChart.setMarker(mv);
        renderData();

    }

    public void renderData() {
        LimitLine llXAxis = new LimitLine(10f, "Index 10");
        llXAxis.setLineWidth(4f);
        llXAxis.enableDashedLine(10f, 10f, 0f);
        llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        llXAxis.setTextSize(10f);

        XAxis xAxis = mChart.getXAxis();
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setAxisMaximum(2048f);  //Valore massimo asse X
        xAxis.setAxisMinimum(0f);
        xAxis.setDrawLimitLinesBehindData(true);

        LimitLine ll1 = new LimitLine(215f, "Maximum Limit");
        ll1.setLineWidth(4f);
        ll1.enableDashedLine(10f, 10f, 0f);
        ll1.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll1.setTextSize(10f);

        LimitLine ll2 = new LimitLine(70f, "Minimum Limit");
        ll2.setLineWidth(4f);
        ll2.enableDashedLine(10f, 10f, 0f);
        ll2.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        ll2.setTextSize(10f);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        leftAxis.addLimitLine(ll1);
        leftAxis.addLimitLine(ll2);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setDrawLimitLinesBehindData(false);

        mChart.getAxisRight().setEnabled(false);
        setData(true);

    }

    private void setData(boolean test1) {
        ArrayList<Entry> values = new ArrayList<>();
        int k;
        int travaso;
        for(k=1;k<(mNumberOfFFTPoints/4);k++){
            values.add(new Entry(k, (int)(absNormalizedSignal[k]*10)));
//              travaso = (int)copiadati[k];
  //            values.add(new Entry(k, travaso));
        }

/*        if (test1) {
            values.add(new Entry(1, 50));
            values.add(new Entry(2, 100));
            values.add(new Entry(3, 80));
            values.add(new Entry(4, 80));
            values.add(new Entry(5, 90));
            values.add(new Entry(7, 15));
            values.add(new Entry(8, 20));
            values.add(new Entry(9, 19));
        }
        else {
            values.add(new Entry(1, 10));
            values.add(new Entry(2, 2));
            values.add(new Entry(3, 5));
            values.add(new Entry(4, 8));
            values.add(new Entry(5, 11));
            values.add(new Entry(7, 18));
            values.add(new Entry(8, 32));
            values.add(new Entry(9, 43));
        }*/

        LineDataSet set1;
        if (mChart.getData() != null &&
                mChart.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet) mChart.getData().getDataSetByIndex(0);
            set1.setValues(values);
            mChart.getData().notifyDataChanged();
            mChart.notifyDataSetChanged();
        } else {
            set1 = new LineDataSet(values, "Sample Data");
          //  set1.setDrawIcons(false);
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.DKGRAY);
            set1.setCircleColor(Color.DKGRAY);
            set1.setLineWidth(1f);
            set1.setCircleRadius(3f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(9f);
            set1.setDrawFilled(true);
            set1.setFormLineWidth(1f);
            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set1.setFormSize(15.f);

            if (Utils.getSDKInt() >= 18) {
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_blue);
                set1.setFillDrawable(drawable);
            } else {
                set1.setFillColor(Color.DKGRAY);
            }
            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1);
            LineData data = new LineData(dataSets);
            mChart.setData(data);
        }
    }


    private void setButtonHandlers() {
        ((Button)findViewById(R.id.btStart)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.btStop)).setOnClickListener(btnClick);
    }

    // Funzione che dato l'ID del pulsante lo attiva o disattiva
    private void enableButton(int id,boolean isEnable){
        ((Button)findViewById(id)).setEnabled(isEnable);
    }

    // Funzione che attiva/disattiva i pulsanti di start e stop recording
    private void enableButtons(boolean isRecording){
        enableButton(R.id.btStart,!isRecording);
        enableButton(R.id.btStop,isRecording);
    }

    private String getFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
//Creates a new File instance from a parent pathname string and a child pathname string.
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);
        if (!file.exists()){
            file.mkdirs();
        }
        return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    public double[] calculateFFT(byte[] signal){
//        final int mNumberOfFFTPoints = 1024;
        double mMaxFFTSample;
        double temp;
        Complex[] y;
        Complex[] complexSignal = new Complex[mNumberOfFFTPoints];
        double[] absSignal = new double[mNumberOfFFTPoints/2];
        for (int i=0; i < mNumberOfFFTPoints; i++){
            temp = (double)((signal[2*i]&0xFF) | (signal[2*i+1] << 8))/32768.0F;
            complexSignal[i] = new Complex(temp,0.0);
        }
        y = FFT.fft(complexSignal);

        mMaxFFTSample = 0.0;
        mPeakPos = 0;
        for(int i=0;i<(mNumberOfFFTPoints/2);i++){
            absSignal[i]=Math.sqrt(Math.pow(y[i].re(),2)+Math.pow(y[i].im(),2));
            if(absSignal[i] > mMaxFFTSample){
                mMaxFFTSample = absSignal[i];
                mPeakPos = i;
            }
        }
        return absSignal;
    }

    private String getTempFilename(){
        String filepath=Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);
        if (!file.exists()){
            file.mkdirs();
        }
        File tempFile=new File(filepath,AUDIO_RECORDER_TEMP_FILE);
        if (tempFile.exists())
            tempFile.delete();
        return (file.getAbsolutePath()+"/"+AUDIO_RECORDER_TEMP_FILE);
    }

    private void startRecording(){
        setData(false);
        mChart.notifyDataSetChanged(); // let the chart know it's data changed
        mChart.invalidate(); // refresh
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING,bufferSize);
        Log.d("CERCAMI: ","Prima dell'inizio");
        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e("CERCAMI: ", "AudioRecord could not be initialized. Exiting!");
            return;
        }
        recorder.startRecording();
        Log.d("CERCAMI: ","Registrazone partita");
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile();
            }
            },"Audio Recorder Thread");
        recordingThread.start();
    }

    private void writeAudioDataToFile(){
        byte data[] = new byte[bufferSize];
//        final int mNumberOfFFTPoints = 1024;
//        double[] absNormalizedSignal = new double[mNumberOfFFTPoints/2];
        String filename = getTempFilename();
        FileOutputStream os = null;
        Log.d("CERCAMI: ","Dentro writeAudioDataToFile : "+filename);
        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            Log.d("CERCAMI: ", "Errore nella creazione del file");
            e.printStackTrace();
        }

        Log.d("CERCAMI: ","isRecording vale : "+isRecording);

        int read=0;
        int jj=0;
        if(null != os){
            while(isRecording){
//                Log.d("CERCAMI: ", "Dentro is recording");
                    read = recorder.read(data, 0, bufferSize);
                if (read > 0){
                    if (jj==5) {
                        jj=0;
                        absNormalizedSignal = calculateFFT(data);
                        //                   Log.d("CERCAMI: ", "arr: " + Arrays.toString(absNormalizedSignal));
                        //                   Log.d("CERCAMI: ", "buffersize = " + bufferSize);
//                    Log.d("CERCAMI: ", "arr: " + Arrays.toString(copiadati));
                        AggiornaGrafico();
                    }
                    jj++;
                }
                if(AudioRecord.ERROR_INVALID_OPERATION != read){
                    try {
                        os.write(data);
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void AggiornaGrafico(){
        setData(false);
        mChart.notifyDataSetChanged(); // let the chart know it's data changed
        mChart.invalidate(); // refresh
        Log.d("CERCAMI: ", "Posizione picco "+mPeakPos); //116 = 10KHz, 58=5KHz, ....
    }

    private void stopRecording(){
        setData(true);
        mChart.notifyDataSetChanged(); // let the chart know it's data changed
        mChart.invalidate(); // refresh
        if(null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
        copyWaveFile(getTempFilename(),getFilename());
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }

    private void copyWaveFile(String inFilename, String outFilename){

    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch(v.getId()){
                case R.id.btStart:{
                    Log.d("CERCAMI: ","Inizio registrazione");
                    enableButtons(true);
                    startRecording();
                    break;
                }
                case R.id.btStop:{
                    Log.d("CERCAMI: ","FINE registrazione");
                    enableButtons(false);
                    stopRecording();
                    break;
                }
            }
        }
    };

    private void requestRecordAudioPermission() {
        //check API version, do nothing if API version < 23!
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion > android.os.Build.VERSION_CODES.LOLLIPOP){

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                }
            }
        }
    }

    private void requestRecordFilePermission() {
        //check API version, do nothing if API version < 23!
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion > android.os.Build.VERSION_CODES.LOLLIPOP){

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                }
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.d("CERCAMI: ", "Granted!");

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("CERCAMI: ", "Denied!");
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

}