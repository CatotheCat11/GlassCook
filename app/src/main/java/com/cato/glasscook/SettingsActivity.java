package com.cato.glasscook;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends Activity {

    private List<CardBuilder> mCards;
    private CardScrollView mCardScrollView;
    private ExampleCardScrollAdapter mAdapter;
    String amountPreference;
    String tempPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createCards();

        mCardScrollView = new CardScrollView(this);
        mAdapter = new ExampleCardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        setupClickListener();
        setContentView(mCardScrollView);
    }

    private void createCards() {
        mCards = new ArrayList<CardBuilder>();

        SharedPreferences sharedPref = SettingsActivity.this.getSharedPreferences(
                getString(R.string.prefs), Context.MODE_PRIVATE);
        amountPreference = sharedPref.getString(getString(R.string.amount), "Metric");
        tempPreference = sharedPref.getString(getString(R.string.temp), "Celsius");

        if (amountPreference.equals("Metric")) {
            mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                    .setText("Amounts: Metric")
                    .setFootnote("Tap to change to imperial"));
        } else {
            mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                    .setText("Amounts: Imperial")
                    .setFootnote("Tap to change to metric"));
        }
        if (tempPreference.equals("Celsius")) {
            mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                    .setText("Temperature: Celsius")
                    .setFootnote("Tap to change to Fahrenheit"));
        } else {
            mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                    .setText("Temperature: Fahrenheit")
                    .setFootnote("Tap to change to Celsius"));
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
                am.playSoundEffect(Sounds.SUCCESS);
                SharedPreferences sharedPref = SettingsActivity.this.getSharedPreferences(
                        getString(R.string.prefs), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                if (position == 0) {
                    if (amountPreference.equals("Metric")) {
                        editor.putString(getString(R.string.amount), "Imperial");
                        editor.apply();
                    } else {
                        editor.putString(getString(R.string.amount), "Metric");
                        editor.apply();
                    }
                }
                if (position == 1) {
                    if (tempPreference.equals("Celsius")) {
                        editor.putString(getString(R.string.temp), "Fahrenheit");
                        editor.apply();
                    } else {
                        editor.putString(getString(R.string.temp), "Celsius");
                        editor.apply();
                    }
                }
                refreshCards(position);
            }
        });
    }
    private void refreshCards(Integer position) {
        mCards.clear();
        createCards();
        mAdapter.notifyDataSetChanged();
        mCardScrollView.setSelection(position);
    }
}
