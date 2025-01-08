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
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PreparationActivity extends Activity {
    private List<CardBuilder> mCards;
    private CardScrollView mCardScrollView;
    private ExampleCardScrollAdapter mAdapter;

    private ArrayList<String> ingredientList = new ArrayList<>();
    private ArrayList<Boolean> preparedIngredients = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
            JSONArray ingredientsArray = new JSONObject(getIntent().getStringExtra("recipe")).getJSONArray("extendedIngredients");
            for (int i = 0; i < ingredientsArray.length(); i++) {
                JSONObject ingredient = ingredientsArray.getJSONObject(i);
                String ingredientText = ingredient.getString("nameClean") + "\n";
                for (int x = 0; x < ingredient.getJSONArray("meta").length(); x++) {
                    String meta = ingredient.getJSONArray("meta").getString(x);
                    if (!ingredientText.contains(meta)) {
                        ingredientText += ingredient.getJSONArray("meta").getString(x) + "\n";
                    }
                }
                SharedPreferences sharedPref = PreparationActivity.this.getSharedPreferences(
                        getString(R.string.prefs), Context.MODE_PRIVATE);
                String amountPreference = sharedPref.getString(getString(R.string.amount), "Metric");
                if (amountPreference.equals("Metric")) {
                    JSONObject measure = ingredient.getJSONObject("measures").getJSONObject("metric");
                    String unitLong = measure.getString("unitLong");
                    Long amount = Math.round(measure.getDouble("amount"));
                    if (unitLong.endsWith("s") && amount == 1) {
                        unitLong = unitLong.substring(0, unitLong.length() - 1);
                    }
                    ingredientText += amount + " " + unitLong;
                } else {
                    JSONObject measure = ingredient.getJSONObject("measures").getJSONObject("us");
                    String unitLong = measure.getString("unitLong");
                    Long amount = Math.round(measure.getDouble("amount"));
                    if (unitLong.endsWith("s") && amount == 1) {
                        unitLong = unitLong.substring(0, unitLong.length() - 1);
                    }
                    ingredientText += amount + " " + unitLong;
                }
                ingredientList.add(ingredientText);
                preparedIngredients.add(false);
                mCards.add(new CardBuilder(this, CardBuilder.Layout.COLUMNS)
                        .setText(ingredientText)
                        .setFootnote("Tap to mark as prepared"));
                int finalI = i;
                Glide
                    .with(this)
                    .asBitmap()
                    .load("https://img.spoonacular.com/ingredients_250x250/" + ingredient.getString("image"))
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

        } catch (JSONException e) {
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
                if (position == mCards.size() - 1 ) {
                    am.playSoundEffect(Sounds.SUCCESS);
                    mCards.get(position).setTimestamp("✔");
                    mCards.get(position).setFootnote("Tap to unmark");
                    preparedIngredients.set(position, true);
                    mAdapter.notifyDataSetChanged();
                    checkPrepared();
                } else {
                    if (preparedIngredients.get(position)) {
                        am.playSoundEffect(Sounds.TAP);
                        mCards.get(position).setTimestamp("");
                        mCards.get(position).setFootnote("Tap to mark as prepared");
                        preparedIngredients.set(position, false);
                        mAdapter.notifyDataSetChanged();
                    } else {
                        am.playSoundEffect(Sounds.SUCCESS);
                        mCards.get(position).setTimestamp("✔");
                        mCards.get(position).setFootnote("Tap to unmark");
                        preparedIngredients.set(position, true);
                        mAdapter.notifyDataSetChanged();
                        if (!preparedIngredients.get(position + 1)) {
                            mCardScrollView.setSelection(position + 1);
                            mCardScrollView.startAnimation(AnimationUtils.makeInAnimation(mCardScrollView.getContext(), false));
                        } else {
                            checkPrepared();
                        }
                    }
                }
            }
        });
    }

    private void checkPrepared() {
        boolean allPrepared = true;
        for (int i = 0; i < preparedIngredients.size(); i++) {
            if (!preparedIngredients.get(i)) {
                allPrepared = false;
                mCardScrollView.animate(i, CardScrollView.Animation.NAVIGATION);
                break;
            }
        }
        if (allPrepared) {
            Intent objIntent = new Intent(PreparationActivity.this, CookingActivity.class);
            try {
                objIntent.putExtra("recipe", getIntent().getStringExtra("recipe"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            startActivity(objIntent);
            finish();
        }
    }
}
