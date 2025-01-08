package com.cato.glasscook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.github.barcodeeye.scan.CaptureActivity;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private static final int API_KEY_REQUEST = 0;
    private static final int RECIPE_REQUEST = 1;
    private static final int SPEECH_REQUEST = 2;
    String apiKey = "";

    private TextToSpeech tts;
    private boolean initialized = false;
    private String queuedText;

    private List<CardBuilder> mCards;
    private CardScrollView mCardScrollView;
    private ExampleCardScrollAdapter mAdapter;

    boolean History;
    boolean ttsEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tts = new TextToSpeech(this, this);

        SharedPreferences sharedPref = MainActivity.this.getSharedPreferences(
                getString(R.string.prefs), Context.MODE_PRIVATE);
        apiKey = sharedPref.getString(getString(R.string.apikey), "");
        try {
            History = new JSONArray(sharedPref.getString("key", "[]")).length() != 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        createCards();
        mCardScrollView = new CardScrollView(this);
        mAdapter = new ExampleCardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        setupClickListener();
        setContentView(mCardScrollView);

        if (apiKey.isEmpty()) {
            Intent objIntent = CaptureActivity.newIntent(this, true);
            objIntent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivityForResult(objIntent, API_KEY_REQUEST);
            speak("Scan a QR code with your spoon acular API key");
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == API_KEY_REQUEST && resultCode == RESULT_OK) {
            tts.stop();
            String result = data.getStringExtra("result");
            SharedPreferences sharedPref = MainActivity.this.getSharedPreferences(
                    getString(R.string.prefs), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(getString(R.string.apikey), result);
            editor.apply();
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.playSoundEffect(Sounds.SUCCESS);
        }
        if (requestCode == RECIPE_REQUEST && resultCode == RESULT_OK) {
            tts.stop();
            Intent objIntent = new Intent(MainActivity.this, RecipeActivity.class);
            objIntent.putExtra("url", data.getStringExtra("result"));
            startActivity(objIntent);
        }
        if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            Intent objIntent = new Intent(MainActivity.this, SearchActivity.class);
            objIntent.putExtra("search", spokenText);
            startActivity(objIntent);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void speak(String text) {
        // If not yet initialized, queue up the text.
        if (!initialized) {
            queuedText = text;
            return;
        }
        queuedText = null;
        // Before speaking the current text, stop any ongoing speech.
        tts.stop();
        // Speak the text.
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            initialized = true;
            tts.setLanguage(Locale.ENGLISH); //TODO: support other languages?

            if (queuedText != null) {
                speak(queuedText);
            }
        }
    }

    private void createCards() {
        mCards = new ArrayList<CardBuilder>();

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Scan recipe QR")
                .setIcon(R.drawable.ic_scan));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Search")
                .setIcon(R.drawable.ic_search));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("History")
                .setIcon(R.drawable.ic_history));

        if (!History) {
            mCards.get(1).setFootnote("No recipes scanned yet");
        }

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Set units")
                .setIcon(R.drawable.ic_set_units));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Change API Key")
                .setIcon(R.drawable.ic_api));

    }

    private class ExampleCardScrollAdapter extends CardScrollAdapter {

        @Override
        public int getPosition(Object item) {
            return mCards.indexOf(item);
        }

        @Override
        public int getCount() {
            return mCards.size();
        }

        @Override
        public Object getItem(int position) {
            return mCards.get(position);
        }

        @Override
        public int getViewTypeCount() {
            return CardBuilder.getViewTypeCount();
        }

        @Override
        public int getItemViewType(int position){
            return mCards.get(position).getItemViewType();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mCards.get(position).getView(convertView, parent);
        }
    }
    private void setupClickListener() {
        mCardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(Sounds.TAP);
                if (position == 0) {
                    Intent objIntent = CaptureActivity.newIntent(MainActivity.this, true);
                    objIntent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                    startActivityForResult(objIntent, RECIPE_REQUEST);
                    speak("Scan a recipe QR code.");
                } else if (position == 1) {
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Search for a recipe");
                    startActivityForResult(intent, SPEECH_REQUEST);
                } else if (position == 2 && History) {
                    Intent objIntent = new Intent(MainActivity.this, HistoryActivity.class);
                    startActivity(objIntent);
                } else if (position == 3) {
                    Intent objIntent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(objIntent);
                } else if (position == 4) {
                    Intent objIntent = CaptureActivity.newIntent(MainActivity.this, true);
                    objIntent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                    startActivityForResult(objIntent, API_KEY_REQUEST);
                    speak("Scan a QR code with your spoon acular API key");
                } else {
                    am.playSoundEffect(Sounds.DISALLOWED);
                }
            }
        });
    }
}
