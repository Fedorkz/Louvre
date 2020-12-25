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
import android.provider.MediaStore
import androidx.annotation.IntRange
import androidx.fragment.app.FragmentActivity
import androidx.loader.app.LoaderManager

interface MediaLoader // 1 means all media type.
    : LoaderManager.LoaderCallbacks<Cursor?> {

    interface Callbacks {
        fun onBucketLoadFinished(data: Cursor?)
        fun onMediaLoadFinished(data: Cursor?)
    }

    fun onAttach(activity: FragmentActivity, callbacks: Callbacks)

    fun onDetach()

    fun setMediaTypes(mediaTypes: Array<String?>)

    fun loadBuckets()

    fun loadByBucket(@IntRange(from = 0) bucketId: Long)

    companion object {
        const val ALL_MEDIA_BUCKET_ID: Long = 0
        private const val BUCKET_ID = MediaStore.Video.Media.BUCKET_ID
    }

}