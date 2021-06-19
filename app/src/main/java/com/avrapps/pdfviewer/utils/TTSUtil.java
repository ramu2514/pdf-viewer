package com.avrapps.pdfviewer.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.avrapps.pdfviewer.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;


public class TTSUtil implements TextToSpeech.OnInitListener {

    Activity activity;
    Locale ttSLocale = Locale.ENGLISH;
    ImageView btnSpeek;
    private TextToSpeech mTts;
    private int mStatus = 0;
    private MediaPlayer mMediaPlayer;
    private boolean mProcessed = false;
    private ProgressDialog mProgressDialog;

    public TTSUtil(Activity activity) {
        this.activity = activity;
    }

    public void setTts(TextToSpeech tts) {
        this.mTts = tts;

        this.mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {

            @Override
            public void onDone(String utteranceId) {
                mProcessed = true;
                initializeMediaPlayer();
                playMediaPlayer(0);
            }

            @Override
            public void onError(String utteranceId) {
            }

            @Override
            public void onStart(String utteranceId) {
            }
        });

    }

    public void showNoteWithSpeaker(String text) {

        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_text_to_speech, null);

        final LinedEditText textView = dialogView.findViewById(R.id.edit_text);
        textView.setText(text);

        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        alert.setView(dialogView);
        alert.setNegativeButton(R.string.dismiss, (dialog, which) -> dialog.dismiss());
        alert.setOnDismissListener(dialog -> shutdownTts());
        alert.setCancelable(false);
        alert.show();

        dialogView.findViewById(R.id.locale).setOnClickListener(v -> showRadioButtonDialog());

        mTts = new TextToSpeech(activity, this);
        btnSpeek = dialogView.findViewById(R.id.resume);
        mProgressDialog = new ProgressDialog(activity);
        mMediaPlayer = new MediaPlayer();
        mProgressDialog.setCancelable(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        mProgressDialog.setMessage("Please wait ...");

        OnClickListener btnClickListener = v -> {
            if (mStatus == TextToSpeech.SUCCESS) {
                btnSpeek.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_baseline_pause_48));
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    playMediaPlayer(1);
                    btnSpeek.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_baseline_play_arrow_48));
                    return;
                }
                mProgressDialog.show();
                HashMap<String, String> myHashRender = new HashMap<>();
                String utteranceID = "wpta";
                myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceID);
                String fileName = new File(activity.getCacheDir(), "tts.wav").getAbsolutePath();
                if (!mProcessed) {
                    mTts.synthesizeToFile(text, myHashRender, fileName);
                } else {
                    playMediaPlayer(0);
                }
            } else {
                String msg = "TextToSpeech Engine is not initialized";
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
            }
        };

        btnSpeek.setOnClickListener(btnClickListener);
        mProcessed = false;
        mMediaPlayer.reset();
        btnSpeek.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_baseline_play_arrow_48));
        OnCompletionListener mediaPlayerCompletionListener = mp -> btnSpeek.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_baseline_play_arrow_48));
        mMediaPlayer.setOnCompletionListener(mediaPlayerCompletionListener);
    }

    void shutdownTts() {
        mTts.stop();
        mTts.shutdown();
        mMediaPlayer.stop();
        mMediaPlayer.release();
    }

    @Override
    public void onInit(int status) {
        mStatus = status;
        setTts(mTts);
    }

    private void initializeMediaPlayer() {
        String fileName = new File(activity.getCacheDir(), "tts.wav").getAbsolutePath();
        Uri uri = Uri.parse("file://" + fileName);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mMediaPlayer.setDataSource(activity, uri);
            mMediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playMediaPlayer(int status) {
        mProgressDialog.dismiss();
        if (status == 0) {
            mMediaPlayer.start();
        }
        if (status == 1) {
            mMediaPlayer.pause();
        }
    }

    private void showRadioButtonDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
        dialog.setTitle(R.string.choose_language);
        Set<Locale> availableLocales;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            availableLocales = mTts.getAvailableLanguages();
        } else {
            availableLocales = getSupportedLanguagesLegacy();
        }
        CharSequence[] temp = new CharSequence[availableLocales.size()];
        ArrayList<Locale> localesArray = new ArrayList<>();
        int counter = 0, checked = 0;
        for (Locale language : availableLocales) {
            localesArray.add(language);
            temp[counter] = language.getDisplayLanguage();
            if (ttSLocale.equals(language))
                checked = counter;
            counter++;
        }
        dialog.setSingleChoiceItems(temp, checked, (dialog1, which) -> {
            ttSLocale = localesArray.get(which);
            mTts.setLanguage(ttSLocale);
            Log.d("TTSUtils", ttSLocale.toString());
            mMediaPlayer.stop();
            btnSpeek.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_baseline_play_arrow_24));
            dialog1.dismiss();
        });
        dialog.show();
    }

    private Set<Locale> getSupportedLanguagesLegacy() {
        Set<Locale> supportedLocales = new HashSet<>();
        Locale[] allLocales = Locale.getAvailableLocales();
        for (Locale locale : allLocales) {
            try {
                int res = mTts.isLanguageAvailable(locale);
                boolean hasVariant = locale.getVariant().length() > 0;
                boolean hasCountry = locale.getCountry().length() > 0;

                boolean isLocaleSupported =
                        !hasVariant && !hasCountry && res == TextToSpeech.LANG_AVAILABLE ||
                                !hasVariant && hasCountry && res == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                                res == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE;

                Log.d("TTSUtils", "TextToSpeech Engine isLanguageAvailable " + locale + " (supported=" + isLocaleSupported + ",res=" + res + ", country=" + locale.getCountry() + ", variant=" + locale.getVariant() + ")");
                if (isLocaleSupported) {
                    supportedLocales.add(locale);
                }
            } catch (Exception ex) {
                Log.e("TTSUtils", "Error checking if language is available for TTS (locale=" + locale + "): " + ex.getClass().getSimpleName() + "-" + ex.getMessage());
            }
        }
        return supportedLocales;
    }

}