/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.barcodeeye.scan.result.supplement;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;

import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.widget.TextView;

public abstract class SupplementalInfoRetriever extends AsyncTask<Object,Object,Object> {

  private static final String TAG = "SupplementalInfo";

  private final WeakReference<TextView> textViewRef;
  private final Collection<Spannable> newContents;

  SupplementalInfoRetriever(TextView textView) {
    textViewRef = new WeakReference<TextView>(textView);
    newContents = new ArrayList<Spannable>();
  }

  @Override
  public final Object doInBackground(Object... args) {
    try {
      retrieveSupplementalInfo();
    } catch (IOException e) {
      Log.w(TAG, e);
    }
    return null;
  }

  @Override
  protected final void onPostExecute(Object arg) {
    TextView textView = textViewRef.get();
    if (textView != null) {
      for (CharSequence content : newContents) {
        textView.append(content);
      }
      textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
  }

  abstract void retrieveSupplementalInfo() throws IOException;

  final void append(String itemID, String source, String[] newTexts, String linkURL) {

    StringBuilder newTextCombined = new StringBuilder();

    if (source != null) {
      newTextCombined.append(source).append(' ');
    }

    int linkStart = newTextCombined.length();

    boolean first = true;
    for (String newText : newTexts) {
      if (first) {
        newTextCombined.append(newText);
        first = false;
      } else {
        newTextCombined.append(" [");
        newTextCombined.append(newText);
        newTextCombined.append(']');
      }
    }

    int linkEnd = newTextCombined.length();

    String newText = newTextCombined.toString();
    Spannable content = new SpannableString(newText + "\n\n");
    if (linkURL != null) {
      // Strangely, some Android browsers don't seem to register to handle HTTP:// or HTTPS://.
      // Lower-case these as it should always be OK to lower-case these schemes.
      if (linkURL.startsWith("HTTP://")) {
        linkURL = "http" + linkURL.substring(4);
      } else if (linkURL.startsWith("HTTPS://")) {
        linkURL = "https" + linkURL.substring(5);
      }
      content.setSpan(new URLSpan(linkURL), linkStart, linkEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    newContents.add(content);
  }

  static void maybeAddText(String text, Collection<String> texts) {
    if (text != null && !text.isEmpty()) {
      texts.add(text);
    }
  }

  static void maybeAddTextSeries(Collection<String> textSeries, Collection<String> texts) {
    if (textSeries != null && !textSeries.isEmpty()) {
      boolean first = true;
      StringBuilder authorsText = new StringBuilder();
      for (String author : textSeries) {
        if (first) {
          first = false;
        } else {
          authorsText.append(", ");
        }
        authorsText.append(author);
      }
      texts.add(authorsText.toString());
    }
  }

}
