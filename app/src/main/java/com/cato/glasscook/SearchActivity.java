package com.cato.glasscook;

import static com.cato.glasscook.ImageRequest.makeImageRequest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.Nullable;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.google.android.glass.widget.Slider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchActivity extends Activity {
    private List<CardBuilder> mCards;
    private CardScrollView mCardScrollView;
    private ExampleCardScrollAdapter mAdapter;
    private Slider mSlider;
    private Slider.Indeterminate mIndeterminate;
    private ArrayList<Long> idList = new ArrayList<>();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCardScrollView = new CardScrollView(this);
        mAdapter = new ExampleCardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mSlider = Slider.from(mCardScrollView);
        mIndeterminate = mSlider.startIndeterminate();
        mCards = new ArrayList<CardBuilder>();
        new search().execute();
        mCardScrollView.activate();
        setupClickListener();
        setContentView(mCardScrollView);
    }
    public class search extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            String search = getIntent().getStringExtra("search");
            SharedPreferences sharedPref = SearchActivity.this.getSharedPreferences(
                    getString(R.string.prefs), Context.MODE_PRIVATE);
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://api.spoonacular.com/recipes/complexSearch?query=" + search + "&apiKey=" + sharedPref.getString(getString(R.string.apikey), ""))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 200) {
                    JSONArray results = new JSONObject(response.body().string()).getJSONArray("results");
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject resultObj = results.getJSONObject(i);
                        mCards.add(new CardBuilder(SearchActivity.this, CardBuilder.Layout.CAPTION)
                                .setText(resultObj.getString("title")));
                        idList.add(resultObj.getLong("id"));
                        int finalI = i;
                        try {
                            makeImageRequest(SearchActivity.this, resultObj.getString("image"), client, new ImageRequest.ImageCallback() {
                                @Override
                                public void onImageLoaded(Bitmap bitmap) {
                                    mCards.get(finalI).addImage(bitmap);
                                    runOnUiThread(() -> mAdapter.notifyDataSetChanged());
                                }
                            });
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (mIndeterminate != null) {
                        mIndeterminate.hide();
                        mIndeterminate = null;
                    }
                } else {
                    AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    am.playSoundEffect(Sounds.ERROR);
                    Log.e("Recipe Obj", "Failed to get recipe");
                    Log.e("Recipe Obj", String.valueOf(response.code()));
                    Log.e("Recipe Obj", response.body().string());
                    finish();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
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
                Intent objIntent = new Intent(SearchActivity.this, RecipeActivity.class);
                objIntent.putExtra("url", idList.get(position).toString());
                startActivity(objIntent);
            }
        });
    }
}
