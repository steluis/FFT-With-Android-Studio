# FFT-With-Android-Studio
FFT of microphone audio signal with live display.

The purpose of this project is the creation of an application on the Android Studio platform, which allows to perform the spectral analysis of the audio signal coming from the smartphone microphone. This analysis takes place in real time and the spectrum is displayed graphically.
It is possible to extract some characteristics of the spectrum, such as the frequency of the harmonic with the maximum amplitude.

The chart is displayed using MPAndroidChart library.

All the file of the project are available for download.
The application has been tested with Android Studio 4.0.

ANALISYS OF MainActivity.java
Refer to activity_main.xml to see the layout. 
Under onCreate buttons instances are created, note also the call to the functions requestRecordAudioPermission() and requestRecordFilePermission() to authorize audio recording and file writing. 
mChart is an object LineChart, for details of charts types you can refer to MPAndroidChart library.

renderData()
This function defines xAxis and yAxis size, extension and labels.

setData()
Here is the data entry of the points to show in the graph.

View.OnClickListener
Definition of the functions to call when buttons are pushed. 

startRecording()
The thread writeAudioDataToFile() is launched. 

<b>writeAudioDataToFile()</b>
This is an indipendent thread. Core of this function is the loop while(isRecording) which remains active until Stop button in pressed.
Recorded audio data are transferred to the buffer data[]. The size of this buffer is the variable bufferSize that is calculated in relation to PCM sample rate and is equal to 21504. Inside while() cycle, FFT is calculated and the absolute values of the FFT are stored in the array absNormalizedSignal[]. The size of this array is defined by the variable mNumberOfFFTPoints that I fixed to 8192 (must be a multiple of 2) so as you can see it has nothing to do with bufferSize.
Every time a new set of data is read and FFT calculated, function AggiornaGrafico() is called to refresh the graph in the display with the new data.
