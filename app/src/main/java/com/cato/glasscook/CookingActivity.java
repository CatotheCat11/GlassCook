package com.cato.glasscook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cato.glasscook.Timer.TimerService;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.google.android.glass.widget.Slider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CookingActivity extends Activity {

    private List<CardBuilder> mCards;
    private CardScrollView mCardScrollView;
    private ExampleCardScrollAdapter mAdapter;
    private ArrayList<Long> timers = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCards = new ArrayList<CardBuilder>();
        mCardScrollView = new CardScrollView(this);
        mAdapter = new ExampleCardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        createCards();
        mCardScrollView.activate();
        setupListeners();
        setContentView(mCardScrollView);
        if (getIntent().hasExtra("step")) {
            mCardScrollView.animate(getIntent().getIntExtra("step", 0), CardScrollView.Animation.NAVIGATION);
        }
    }

    private void createCards() {
        try {
            JSONArray stepsArray = new JSONObject(getIntent().getStringExtra("recipe")).getJSONArray("analyzedInstructions").getJSONObject(0).getJSONArray("steps");
            for (int i = 0; i < stepsArray.length(); i++) {
                mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT));
                JSONObject step = stepsArray.getJSONObject(i);
                JSONArray ingredients = step.getJSONArray("ingredients");
                JSONArray equipment = step.getJSONArray("equipment");
                String stepText = step.getString("step");
                for (int x = 0; x < ingredients.length(); x++) {
                    JSONObject ingredient = ingredients.getJSONObject(x);
                    String image = ingredient.getString("image");
                    if (image.startsWith("https://")) {
                        int finalI = i;
                        Glide
                            .with(this)
                            .asBitmap()
                            .load(image)
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                    mCards.get(finalI).addImage(resource);
                                    mAdapter.notifyDataSetChanged();
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {

                                }
                            });
                    }
                }
                for (int x = 0; x < equipment.length(); x++) {
                    JSONObject tool = equipment.getJSONObject(x);
                    String image = tool.getString("image");
                    if (image.startsWith("https://")) {
                        int finalI = i;
                        Glide
                                .with(this)
                                .asBitmap()
                                .load(image)
                                .into(new CustomTarget<Bitmap>() {
                                    @Override
                                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                        mCards.get(finalI).addImage(resource);
                                        mAdapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onLoadCleared(@Nullable Drawable placeholder) {

                                    }
                                });
                    }
                    if (tool.has("temperature")) {
                        // Get temperature details from the JSON
                        JSONObject temperature = tool.getJSONObject("temperature");
                        double tempNumber = temperature.getDouble("number");
                        String tempUnit = temperature.getString("unit");

                        // Load user preference
                        SharedPreferences sharedPref = CookingActivity.this.getSharedPreferences(
                                getString(R.string.prefs), Context.MODE_PRIVATE);
                        String tempPreference = sharedPref.getString(getString(R.string.temp), "Celsius");

                        // Convert temperature based on preference
                        if (tempPreference.equals("Celsius") && tempUnit.equalsIgnoreCase("Fahrenheit")) {
                            tempNumber = (tempNumber - 32) * 5 / 9; // Convert to Celsius
                            tempUnit = "°C";
                        } else if (tempPreference.equals("Fahrenheit") && tempUnit.equalsIgnoreCase("Celsius")) {
                            tempNumber = tempNumber * 9 / 5 + 32; // Convert to Fahrenheit
                            tempUnit = "°F";
                        }

                        // Replace temperature in stepText
                        stepText = stepText.replaceAll(
                                "\\b\\d+\\.?\\d*\\b\\s*(°?\\s*[Ff](ahrenheit)?|°?\\s*[Cc](elsius)?)",
                                Math.round(tempNumber) + " " + tempUnit
                        );
                    }

                }
                if (step.has("length")) {
                    // Get length details from the JSON
                    JSONObject length = step.getJSONObject("length");
                    double lengthNumber = length.getDouble("number");
                    String lengthUnit = length.getString("unit");
                    long durationMillis = 0;
                    switch (lengthUnit) {
                        case "minutes":
                            durationMillis = TimeUnit.MINUTES.toMillis(Math.round(lengthNumber));
                            break;
                        case "hours":
                            durationMillis = TimeUnit.HOURS.toMillis(Math.round(lengthNumber));
                            break;
                    }
                    timers.add(durationMillis);
                    if (Math.round(lengthNumber) == 1) {
                        mCards.get(i).setFootnote("Tap to start timer for " + Math.round(lengthNumber) + " " + lengthUnit.substring(0, lengthUnit.length() - 1));
                    } else {
                        mCards.get(i).setFootnote("Tap to start timer for " + Math.round(lengthNumber) + " " + lengthUnit);
                    }
                } else {
                    timers.add(0L);
                }
                mCards.get(i).setText(stepText);
            }
            mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                    .setText("Enjoy your meal!")
                    .setFootnote("Tap to close recipe."));
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

    private void setupListeners() {
        mCardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (position == mCards.size() - 1) {
                    am.playSoundEffect(Sounds.TAP);
                    finishAffinity();
                } else if (timers.get(position) != 0L) {
                    am.playSoundEffect(Sounds.TAP);
                    Intent timerIntent = new Intent(CookingActivity.this, TimerService.class);

                    timerIntent.setAction(TimerService.ACTION_START);
                    timerIntent.putExtra(
                            TimerService.EXTRA_DURATION_MILLIS, timers.get(position));
                    timerIntent.putExtra("recipe", getIntent().getStringExtra("recipe"));
                    timerIntent.putExtra("step", position);
                    startService(timerIntent);
                    finishAffinity();
                }
            }
        });
        mCardScrollView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == mCards.size() - 1) {
                    AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    am.playSoundEffect(Sounds.SUCCESS);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
}
