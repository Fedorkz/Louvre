/*
 * Copyright (c) 2017. André Mion
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andremion.louvre.data;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.andremion.louvre.R;

import org.jetbrains.annotations.NotNull;

import static com.andremion.louvre.data.ImageMediaQuery.ALL_IMAGE_PROJECTION;
import static com.andremion.louvre.data.ImageMediaQuery.BUCKET_PROJECTION;
import static com.andremion.louvre.data.ImageMediaQuery.BUCKET_SORT_ORDER;
import static com.andremion.louvre.data.ImageMediaQuery.GALLERY_URI;
import static com.andremion.louvre.data.ImageMediaQuery.IMAGE_PROJECTION;
import static com.andremion.louvre.data.ImageMediaQuery.MEDIA_SORT_ORDER;
import static com.andremion.louvre.data.VideoAndImageMediaLoader.ARG_IDS;

/**
 * {@link Loader} for media and bucket data
 */
public class ImageMediaLoader implements LoaderManager.LoaderCallbacks<Cursor>, MediaLoader {

    private static final int TIME_LOADER = 0;
    private static final int BUCKET_LOADER = 1;
    private static final int MEDIA_LOADER = 2;

    public static final long ALL_MEDIA_BUCKET_ID = 0;
    private static final String BUCKET_ID = MediaStore.Images.Media.BUCKET_ID;

    private FragmentActivity mActivity;
    private MediaLoader.Callbacks mCallbacks;
    private String mTypeFilter;

    public ImageMediaLoader() {
        // 1 means all media type.
        mTypeFilter = "1";
    }

    @Override
    public final Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == TIME_LOADER) {
            return new CursorLoader(mActivity,
                    GALLERY_URI,
                    ALL_IMAGE_PROJECTION,
                    null,
                    null,
                    MEDIA_SORT_ORDER);
        }
        if (id == BUCKET_LOADER) {
            String selection = AlbumQuery.idsBundleToSelection(args);
            return new CursorLoader(mActivity,
                    GALLERY_URI,
                    BUCKET_PROJECTION,
                    selection,
                    null,
                    BUCKET_SORT_ORDER);
        }
        // id == MEDIA_LOADER
        return new CursorLoader(mActivity,
                GALLERY_URI,
                IMAGE_PROJECTION,
                String.format("%s=%s AND %s", MediaStore.Images.Media.BUCKET_ID, args.getLong(BUCKET_ID), mTypeFilter),
                null,
                MEDIA_SORT_ORDER);
    }

    @Override
    public final void onLoadFinished(@NonNull Loader<Cursor> loader, @Nullable Cursor data) {
        if (mCallbacks != null) {
            if (loader.getId() == BUCKET_LOADER) {
                mCallbacks.onBucketLoadFinished(data);
//                mCallbacks.onBucketLoadFinished(addAllMediaBucketItem(data));
            } else {
                mCallbacks.onMediaLoadFinished(data);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // no-op
    }

    @Override
    public void onAttach(@NonNull FragmentActivity activity, @NonNull MediaLoader.Callbacks callbacks) {
        mActivity = activity;
        mCallbacks = callbacks;
    }

    @Override
    public void onDetach() {
        mActivity = null;
        mCallbacks = null;
    }

    @Override
    public void setMediaTypes(@NonNull String[] mediaTypes) {
        StringBuilder filter = new StringBuilder();
        for (String type : mediaTypes) {
            if (filter.length() > 0) {
                filter.append(",");
            }
            filter.append(String.format("'%s'", type));
        }
        if (filter.length() > 0) {
            mTypeFilter = MediaStore.Images.ImageColumns.MIME_TYPE + " IN (" + filter + ")";
        }
    }

    @Override
    public void loadBuckets() {
        ensureActivityAttached();
        Bundle args = AlbumQuery.getAlbumFileIdsAsBundle(
                mActivity, false
        );

        mActivity.getSupportLoaderManager().restartLoader(BUCKET_LOADER, args, this);
    }

    @Override
    public void loadByBucket(@IntRange(from = 0) long bucketId) {
        ensureActivityAttached();
        if (ALL_MEDIA_BUCKET_ID == bucketId) {
            mActivity.getSupportLoaderManager().restartLoader(TIME_LOADER, null, this);
        } else {
            Bundle args = new Bundle();
            args.putLong(BUCKET_ID, bucketId);
            mActivity.getSupportLoaderManager().restartLoader(MEDIA_LOADER, args, this);
        }
    }

    /**
     * Ensure that a FragmentActivity is attached to this loader.
     */
    private void ensureActivityAttached() {
        if (mActivity == null) {
            throw new IllegalStateException("The FragmentActivity was not attached!");
        }
    }

    /**
     * Add "All Media" item as the first row of bucket items.
     *
     * @param cursor The original data of all bucket items
     * @return The data with "All Media" item added
     */
    private Cursor addAllMediaBucketItem(@Nullable Cursor cursor) {
        if (cursor == null || !cursor.moveToPosition(0)) {
            return null;
        }
        ensureActivityAttached();
        long id = ALL_MEDIA_BUCKET_ID;
        String label = mActivity.getString(R.string.activity_gallery_bucket_all_media);
        String data = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        MatrixCursor allMediaRow = new MatrixCursor(BUCKET_PROJECTION);
        allMediaRow.newRow()
                .add(id)
                .add(label)
                .add(data);
        return new MergeCursor(new Cursor[]{allMediaRow, cursor});
    }

}
