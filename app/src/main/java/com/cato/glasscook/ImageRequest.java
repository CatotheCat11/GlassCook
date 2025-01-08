package com.cato.glasscook;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import org.conscrypt.Conscrypt;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.Security;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;

public class ImageRequest {
    private static final int CACHE_SIZE = (int) (Runtime.getRuntime().maxMemory() / 8); // Use 1/8th of available memory
    private static final LruCache<String, Bitmap> memoryCache = new LruCache<String, Bitmap>(CACHE_SIZE) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getByteCount();
        }
    };
    static OkHttpClient okHttpClient = null;
    static ExecutorService executor = Executors.newFixedThreadPool(2);
    public static void makeImageRequest(Context context, String url, OkHttpClient client, ImageCallback callback) {
        executor.execute(() -> {
            Bitmap cachedBitmap = memoryCache.get(url);
            if (cachedBitmap != null) {
                callback.onImageLoaded(cachedBitmap);
                return;
            }
            if (okHttpClient == null) {
                okHttpClient = client;
            }
            Security.insertProviderAt(Conscrypt.newProvider(), 1); // Enable Conscrypt
            RequestOptions requestOptions = new RequestOptions()
                    .format(DecodeFormat.PREFER_RGB_565)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false);

            Glide.with(context)
                    .asBitmap()
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .apply(requestOptions)
                    .override(640, 360)
                    .into(new CustomTarget<Bitmap>() { // Use CustomTarget to handle the Bitmap directly
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            memoryCache.put(url, resource);
                            callback.onImageLoaded(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // Clean up resources if needed
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            super.onLoadFailed(errorDrawable);
                            callback.onImageLoaded(null);
                        }
                    });
        });
    }

    public interface ImageCallback {
        void onImageLoaded(Bitmap bitmap);
    }

    @GlideModule
    private static class CustomGlideModule extends AppGlideModule {

        @Override
        public void registerComponents(Context context, Glide glide, Registry registry) {
            OkHttpUrlLoader.Factory factory = new OkHttpUrlLoader.Factory(okHttpClient);
            glide.getRegistry().replace(GlideUrl.class, InputStream.class, factory);
        }
    }
}
