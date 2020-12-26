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
package com.andremion.louvre.data

import android.database.Cursor
import android.database.MatrixCursor
import android.database.MergeCursor
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.IntRange
import androidx.fragment.app.FragmentActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.andremion.louvre.R
import com.andremion.louvre.data.VideoAndImageMediaQuery.ALL_IMAGE_PROJECTION
import com.andremion.louvre.data.VideoAndImageMediaQuery.BUCKET_PROJECTION
import com.andremion.louvre.data.VideoAndImageMediaQuery.BUCKET_SORT_ORDER
import com.andremion.louvre.data.VideoAndImageMediaQuery.FILE_GALLERY_URI
import com.andremion.louvre.data.VideoAndImageMediaQuery.GALLERY_URI
import com.andremion.louvre.data.VideoAndImageMediaQuery.IMAGE_PROJECTION
import com.andremion.louvre.data.VideoAndImageMediaQuery.MEDIA_SORT_ORDER
import java.lang.IllegalArgumentException

/**
 * [Loader] for media and bucket data
 */
class VideoAndImageMediaLoader // 1 means all media type.
    : LoaderManager.LoaderCallbacks<Cursor?>, MediaLoader {

    private var mActivity: FragmentActivity? = null
    private var mCallbacks: MediaLoader.Callbacks? = null
    private var mTypeFilter = "1"

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor?> {

        val bucketIdCol = MediaStore.MediaColumns.BUCKET_ID
        val mediaTypeCol = MediaStore.Files.FileColumns.MEDIA_TYPE

        val mediaTypeValImage = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
        val mediaTypeValVideo = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO

        val bucketId = args?.getLong(BUCKET_ID)

        return when (id) {
            TIME_LOADER -> CursorLoader(mActivity!!,
                    GALLERY_URI,
                    ALL_IMAGE_PROJECTION,
                    mTypeFilter,
                    null,
                    MEDIA_SORT_ORDER)

            BUCKET_LOADER -> {
                val ids = args?.getStringArrayList(ARG_IDS) ?: emptyList()

                val idSelector = ids.mapNotNull { _id ->
                    "(${MediaStore.MediaColumns._ID} == $_id)"
                }.joinToString(" OR ")

                CursorLoader(mActivity!!,
                        FILE_GALLERY_URI,
                        BUCKET_PROJECTION,
                        idSelector,
                        null,
                        BUCKET_SORT_ORDER
                )
            }

//            VIDEO_BUCKET_LOADER -> CursorLoader(mActivity!!,
//                    VIDEO_GALLERY_URI,
//                    BUCKET_PROJECTION, String.format("%s AND %s", mTypeFilter, BUCKET_SELECTION),
//                    null,
//                    BUCKET_SORT_ORDER)

            MEDIA_LOADER -> {
                CursorLoader(mActivity!!,
                        FILE_GALLERY_URI,
                        IMAGE_PROJECTION,
                        "$bucketIdCol=$bucketId " +
                                "AND ($mediaTypeCol=$mediaTypeValImage OR $mediaTypeCol=$mediaTypeValVideo) " +
                                "AND $mTypeFilter",
                        null,
                        MEDIA_SORT_ORDER)
            }

            else -> throw IllegalArgumentException("WRONG LOADER ID")
        }
    }

    private val lock = Object()

    private var videoBucketsCursor: Cursor? = null
    private var imageBucketsCursor: Cursor? = null

    override fun onLoadFinished(loader: Loader<Cursor?>, data: Cursor?) {
        synchronized(lock) {
            if (mCallbacks == null) return

            when (loader.id) {
                BUCKET_LOADER -> {
                    imageBucketsCursor = data
                    mCallbacks?.onBucketLoadFinished(data)
//                    mCallbacks?.onBucketLoadFinished(addAllMediaBucketItem(data))
                }

//                VIDEO_BUCKET_LOADER -> {
//                    videoBucketsCursor = data
//                    checkBuckets()
//                }

                TIME_LOADER -> mCallbacks?.onMediaLoadFinished(data)

                MEDIA_LOADER -> mCallbacks?.onMediaLoadFinished(data)

                else -> {
                }
            }
        }
//
//        if (loader.id == BUCKET_LOADER) {
//            mCallbacks!!.onBucketLoadFinished(addAllMediaBucketItem(data))
//        } else {
//            mCallbacks!!.onMediaLoadFinished(data)
//        }
    }

    override fun onLoaderReset(loader: Loader<Cursor?>) {
        // no-op
    }

    override fun onAttach(activity: FragmentActivity, callbacks: MediaLoader.Callbacks) {
        mActivity = activity
        mCallbacks = callbacks
    }

    override fun onDetach() {
        mActivity = null
        mCallbacks = null
    }

    override fun setMediaTypes(mediaTypes: Array<String?>) {
        val filter = StringBuilder()
        for (type in mediaTypes) {
            if (filter.isNotEmpty()) {
                filter.append(",")
            }
            filter.append(String.format("'%s'", type))
        }
        if (filter.isNotEmpty()) {
            mTypeFilter = MediaStore.Images.ImageColumns.MIME_TYPE + " IN (" + filter + ")"
        }
    }

    override fun loadBuckets() {
        ensureActivityAttached()
        synchronized(lock) {
            imageBucketsCursor = null
            videoBucketsCursor = null
            val albums = AlbumQuery.get(mActivity!!, true)
            val args = Bundle().apply {
                putStringArrayList(
                        ARG_IDS, ArrayList(albums.mapNotNull { it.fileId })
                )
            }
            mActivity!!.supportLoaderManager.restartLoader(BUCKET_LOADER, args, this)
        }
    }

    override fun loadByBucket(@IntRange(from = 0) bucketId: Long) {
        ensureActivityAttached()
        if (ALL_MEDIA_BUCKET_ID == bucketId) {
            mActivity?.supportLoaderManager?.restartLoader(TIME_LOADER, null, this)
        } else {
            val args = Bundle()
            args.putLong(BUCKET_ID, bucketId)
            mActivity?.supportLoaderManager?.restartLoader(MEDIA_LOADER, args, this)
        }
    }

    /**
     * Ensure that a FragmentActivity is attached to this loader.
     */
    private fun ensureActivityAttached() {
        checkNotNull(mActivity) { "The FragmentActivity was not attached!" }
    }

    /**
     * Add "All Media" item as the first row of bucket items.
     *
     * @param cursor The original data of all bucket items
     * @return The data with "All Media" item added
     */
    private fun addAllMediaBucketItem(cursor: Cursor?): Cursor? {
        if (cursor == null || !cursor.moveToPosition(0)) {
            return null
        }

        ensureActivityAttached()
        val id = ALL_MEDIA_BUCKET_ID
        val label = mActivity!!.getString(R.string.activity_gallery_bucket_all_media)
        val data = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
        val allMediaRow = MatrixCursor(BUCKET_PROJECTION)
        allMediaRow.newRow()
                .add(id)
                .add(label)
                .add(data)

        return MergeCursor(arrayOf(allMediaRow, cursor))
    }

    companion object {
        private const val TIME_LOADER = 0
        private const val BUCKET_LOADER = 1
        private const val MEDIA_LOADER = 2

        private const val VIDEO_BUCKET_LOADER = 5

        const val ALL_MEDIA_BUCKET_ID: Long = 0
        private const val BUCKET_ID = MediaStore.Video.Media.BUCKET_ID

        const val ARG_IDS = "ARG_IDS"
    }

}