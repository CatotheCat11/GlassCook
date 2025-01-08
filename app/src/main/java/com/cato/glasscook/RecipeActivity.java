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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RecipeActivity extends Activity {
    String url = "";
    JSONObject recipeObj;
    File recipeDir;
    String infoText = "";
    String title = "";

    private List<CardBuilder> mCards;
    private CardScrollView mCardScrollView;
    private ExampleCardScrollAdapter mAdapter;
    private Slider mSlider;
    private Slider.Indeterminate mIndeterminate;

    OkHttpClient client;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCards = new ArrayList<CardBuilder>();

        mCardScrollView = new CardScrollView(this);
        mAdapter = new ExampleCardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mSlider = Slider.from(mCardScrollView);
        mIndeterminate = mSlider.startIndeterminate();
        mCardScrollView.activate();
        setContentView(mCardScrollView);

        CustomTrust customTrust = new CustomTrust(getApplicationContext());
        client = customTrust.getClient();

        recipeDir = new File(RecipeActivity.this.getFilesDir()+"/recipes");
        url = getIntent().getStringExtra("url");

        if(!recipeDir.exists() && !recipeDir.isDirectory())
        {
            // create empty directory
            if (recipeDir.mkdirs())
            {
                Log.d("CreateDir","Recipe dir created");
            }
            else
            {
                Log.e("CreateDir","Unable to create app dir!");
            }
        }
        else
        {
            Log.d("CreateDir","Recipe dir already exists");
        }
        SharedPreferences sharedPref = RecipeActivity.this.getSharedPreferences(
                getString(R.string.prefs), Context.MODE_PRIVATE);
        try {
            boolean matchFound = false;
            Log.d("Recipe Array", "Starting search");
            JSONArray recipeArray = new JSONArray(sharedPref.getString("key", "[]"));
            if (recipeArray.isNull(0)) {
                Log.d("Recipe Array", "No recipes found");
                new recipeTask().execute(-1);
            } else {
                for (int i = 0; i < recipeArray.length(); i++) {
                    Log.d("Recipe Array", "Looking at value " + i);
                    Log.d("Recipe Array", recipeArray.getString(i));
                    if (recipeArray.getString(i).equals(url)) {
                        Log.d("Recipe Array", "Match found");
                        new recipeTask().execute(i);
                        matchFound = true;
                        break;
                    } else {
                        Log.d("Recipe Array", "Not a match");
                    }
                }
                if (!matchFound) {
                    Log.d("Recipe Array", "No match found");
                    new recipeTask().execute(-1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public class recipeTask extends AsyncTask<Integer, Void, Void> {
        @Override
        protected Void doInBackground(Integer... file) {
            SharedPreferences sharedPref = RecipeActivity.this.getSharedPreferences(
                    getString(R.string.prefs), Context.MODE_PRIVATE);
            if (file[0] == -1) {
                JSONArray recipeArray;
                try {
                    recipeArray = new JSONArray(sharedPref.getString("key", "[]"));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                OkHttpClient client = new OkHttpClient();
                Request request = null;
                if (url.startsWith("http")) {
                    request = new Request.Builder()
                            .url("https://api.spoonacular.com/recipes/extract?analyze=true&url=" + url + "&apiKey=" + sharedPref.getString(getString(R.string.apikey), ""))
                            .build();
                } else {
                    request = new Request.Builder()
                            .url("https://api.spoonacular.com/recipes/" + url + "/information?includeNutrition=false&addWinePairing=false&addTasteData=false&apiKey=" + sharedPref.getString(getString(R.string.apikey), ""))
                            .build();
                }
                try (Response response = client.newCall(request).execute()) {
                    if (response.code() == 200) {
                        recipeObj = new JSONObject(response.body().string());
                        if (!url.startsWith("http")) {
                            request = new Request.Builder()
                                    .url("https://api.spoonacular.com/recipes/" + url + "/analyzedInstructions?apiKey=" + sharedPref.getString(getString(R.string.apikey), ""))
                                    .build();
                            try (Response instructions = client.newCall(request).execute()) {
                                if (instructions.code() == 200) {
                                    recipeObj.put("analyzedInstructions", new JSONArray(instructions.body().string()));
                                    Log.d("Recipe Obj", recipeObj.toString());
                                    try (FileOutputStream fos = new FileOutputStream(new File(recipeDir, recipeArray.length() + ".json").getAbsolutePath())) {
                                        fos.write(recipeObj.toString().getBytes());
                                    }
                                    recipeArray.put(url);
                                    SharedPreferences.Editor editor = sharedPref.edit();
                                    editor.putString("key", recipeArray.toString());
                                    System.out.println(recipeArray.toString());
                                    editor.commit();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            createCards();
                                        }
                                    });
                                } else {
                                    AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                                    am.playSoundEffect(Sounds.ERROR);
                                    Log.e("Recipe Obj", "Failed to get recipe");
                                    Log.e("Recipe Obj", String.valueOf(instructions.code()));
                                    Log.e("Recipe Obj", instructions.body().string());
                                    finish();
                                }

                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            Log.d("Recipe Obj", recipeObj.toString());
                            try (FileOutputStream fos = new FileOutputStream(new File(recipeDir, recipeArray.length() + ".json").getAbsolutePath())) {
                                fos.write(recipeObj.toString().getBytes());
                            }
                            recipeArray.put(url);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString("key", recipeArray.toString());
                            System.out.println(recipeArray.toString());
                            editor.commit();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    createCards();
                                }
                            });
                        }
                    } else {
                        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        am.playSoundEffect(Sounds.ERROR);
                        Log.e("Recipe Obj", "Failed to get recipe");
                        Log.e("Recipe Obj", String.valueOf(response.code()));
                        Log.e("Recipe Obj", response.body().string());
                        finish();
                    }
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(new File(recipeDir, file[0] + ".json").getAbsolutePath());
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
                    recipeObj = new JSONObject(stringBuilder.toString());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            createCards();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }
    }
    private void createCards() {
        infoText = "";
        try {
            mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)
                    .setText(recipeObj.getString("title"))
                    .setFootnote(recipeObj.getString("sourceName")));
            title = recipeObj.getString("title");
            if (recipeObj.getInt("readyInMinutes") != -1) {
                infoText += "Ready in " + recipeObj.getString("readyInMinutes") + " minutes\n";
            }
            if (recipeObj.getInt("servings") != -1) {
                infoText += "Serves " + recipeObj.getString("servings") + "\n";
            }
            mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                    .setText(infoText));
            mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                    .setText("Prepare ingredients")
                    .setIcon(R.drawable.ic_prepare_ingredients));
            mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                    .setText("Start cooking")
                    .setIcon(R.drawable.ic_cook));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        setupClickListener();

        try {
            makeImageRequest(RecipeActivity.this, recipeObj.getString("image"), client, new ImageRequest.ImageCallback() {
                @Override
                public void onImageLoaded(Bitmap bitmap) {
                    mCards.get(0).addImage(bitmap);
                    runOnUiThread(() -> mAdapter.notifyDataSetChanged());
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (mIndeterminate != null) {
            mIndeterminate.hide();
            mIndeterminate = null;
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
                if (position == 2) {
                    Intent objIntent = new Intent(RecipeActivity.this, PreparationActivity.class);
                    try {
                        objIntent.putExtra("recipe", recipeObj.toString());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    startActivity(objIntent);
                } if (position == 3) {
                    Intent objIntent = new Intent(RecipeActivity.this, CookingActivity.class);
                    try {
                        objIntent.putExtra("recipe", recipeObj.toString());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    startActivity(objIntent);
                }
            }
        });
    }

}
