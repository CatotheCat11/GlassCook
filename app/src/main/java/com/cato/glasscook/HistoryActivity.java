package com.cato.glasscook;

import static com.cato.glasscook.ImageRequest.makeImageRequest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;

public class HistoryActivity extends Activity {
    private List<CardBuilder> mCards;
    private CardScrollView mCardScrollView;
    private ExampleCardScrollAdapter mAdapter;

    JSONArray recipeArray;

    OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CustomTrust customTrust = new CustomTrust(getApplicationContext());
        client = customTrust.getClient();

        SharedPreferences sharedPref = HistoryActivity.this.getSharedPreferences(
                getString(R.string.prefs), Context.MODE_PRIVATE);
        try {
            recipeArray = new JSONArray(sharedPref.getString("key", "[]"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        mCardScrollView = new CardScrollView(this);
        mAdapter = new ExampleCardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        createCards();
        mCardScrollView.activate();
        setupClickListener();
        setContentView(mCardScrollView);
    }

    private void createCards() {
        mCards = new ArrayList<CardBuilder>();
        try {
            if (recipeArray.length() == 0) {
                mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                        .setText("Nothing here yet. Scan a recipe to get started!"));
            } else {
                for (int i = recipeArray.length() - 1; i >= 0; i--) {
                    FileInputStream fis = null;
                    File recipeDir = new File(HistoryActivity.this.getFilesDir()+"/recipes");
                    try {
                        fis = new FileInputStream(new File(recipeDir, i + ".json").getAbsolutePath());
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    InputStreamReader inputStreamReader =
                            new InputStreamReader(fis, StandardCharsets.UTF_8);
                    StringBuilder stringBuilder = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                        String line = reader.readLine();
                        while (line != null) {
                            stringBuilder.append(line).append('\n');
                            line = reader.readLine();
                        }
                        JSONObject recipeObj = new JSONObject(stringBuilder.toString());
                        mCards.add(new CardBuilder(HistoryActivity.this, CardBuilder.Layout.CAPTION)
                                .setText(recipeObj.getString("title"))
                                .setFootnote(recipeObj.getString("sourceName")));
                        int finalI = i;
                        try {
                            makeImageRequest(HistoryActivity.this, recipeObj.getString("image"), client, new ImageRequest.ImageCallback() {
                                @Override
                                public void onImageLoaded(Bitmap bitmap) {
                                    mCards.get(recipeArray.length() - 1 - finalI).addImage(bitmap);
                                    runOnUiThread(() -> mAdapter.notifyDataSetChanged());
                                }
                            });
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
                Intent objIntent = new Intent(HistoryActivity.this, RecipeActivity.class);
                try {
                    objIntent.putExtra("url", recipeArray.getString(recipeArray.length() - 1 - position));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                startActivity(objIntent);
            }
        });
    }
}
