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
package com.andremion.louvre

import android.app.Activity
import android.net.Uri
import androidx.annotation.IntRange
import androidx.annotation.StringDef
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.andremion.louvre.home.GalleryActivity
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * A small customizable image picker. Useful to handle an image pick action built-in
 */
class Louvre {
    companion object {
        const val IMAGE_TYPE_BMP = "image/bmp"
        const val IMAGE_TYPE_JPEG = "image/jpeg"
        const val IMAGE_TYPE_PNG = "image/png"
        const val IMAGE_TYPE_VIDEO = "VIDEO/*"
        @JvmField
        val IMAGE_TYPES = arrayOf(IMAGE_TYPE_BMP, IMAGE_TYPE_JPEG, IMAGE_TYPE_PNG, IMAGE_TYPE_VIDEO)
        @JvmStatic
        fun init(activity: Activity): Louvre {
            return Louvre(activity)
        }

        @JvmStatic
        fun init(fragment: Fragment): Louvre {
            return Louvre(fragment)
        }

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    @StringDef(IMAGE_TYPE_BMP, IMAGE_TYPE_JPEG, IMAGE_TYPE_PNG, IMAGE_TYPE_VIDEO)
    @Retention(RetentionPolicy.SOURCE)
    internal annotation class MediaType

    private var mActivity: Activity? = null
    private var mFragment: Fragment? = null
    private var mRequestCode: Int
    private var mMaxSelection = 0
    private var mSelection: List<Uri>? = null
    private var mMediaTypeFilter: Array<String> = arrayOf(*IMAGE_TYPES)

    private constructor(activity: Activity) {
        mActivity = activity
        mRequestCode = -1
    }

    private constructor(fragment: Fragment) {
        mFragment = fragment
        mRequestCode = -1
    }

    /**
     * Set the request code to return on [Activity.onActivityResult]
     */
    fun setRequestCode(requestCode: Int): Louvre {
        mRequestCode = requestCode
        return this
    }

    /**
     * Set the max images allowed to pick
     */
    fun setMaxSelection(@IntRange(from = 0) maxSelection: Int): Louvre {
        mMaxSelection = maxSelection
        return this
    }

    /**
     * Set the current selected items
     */
    fun setSelection(selection: List<Uri>?): Louvre {
        mSelection = selection
        return this
    }

    /**
     * Set the media type to filter the query with a combination of one of these types: [.IMAGE_TYPE_BMP], [.IMAGE_TYPE_JPEG], [.IMAGE_TYPE_PNG]
     */
    fun setMediaTypeFilter(@MediaType vararg mediaTypeFilter: String): Louvre {
        mMediaTypeFilter = mediaTypeFilter.toList().toTypedArray()

        return this
    }

    fun open() {
        require(mRequestCode != -1) { "You need to define a request code in setRequestCode(int) method" }
        if (mActivity != null) {
            GalleryActivity.startActivity(mActivity!!, mRequestCode, mMaxSelection, mSelection, false, *mMediaTypeFilter)
        } else {
            GalleryActivity.startActivity(mFragment!!, mRequestCode, mMaxSelection, mSelection, false, *mMediaTypeFilter)
        }
    }
}