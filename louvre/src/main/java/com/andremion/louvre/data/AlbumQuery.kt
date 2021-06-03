package com.andremion.louvre.data

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import java.util.*

object AlbumQuery {
    @JvmStatic
    operator fun get(context: Context, includeVideo: Boolean): List<Album> {
        val output = HashMap<String, Album>()

        val contentUri = MediaStore.Files.getContentUri("external")

        val bucketOrderBy = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.BUCKET_ID, "_data", MediaStore.Images.Media.DATE_MODIFIED, MediaStore.Files.FileColumns.MEDIA_TYPE)

        val typeSelector = if (includeVideo) {
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}) OR (${MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO} )"
        } else {
            "${MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}"
        }

        context.contentResolver.query(
                contentUri, projection,
                "_data IS NOT NULL AND ( $typeSelector ) ", null, bucketOrderBy
        ).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
//                val columnBucketName = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val columnBucketId = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val columnId = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
//                val columnData = cursor.getColumnIndexOrThrow("_data")

                do {
                    val bucketId = cursor.getString(columnBucketId)
                    val id = cursor.getString(columnId)

                    if (bucketId != null && id != null) {
//                    val bucketName = cursor.getString(columnBucketName)
//                    val data = cursor.getString(columnData)

                        if (!output.containsKey(bucketId)) {
//                        val count = getCount(context, contentUri, bucketId)
                            val album = Album(id)
                            output[bucketId] = album
                        }
                    }
                } while (cursor.moveToNext())
            }
        }

        return output.values.toList()
    }

    private fun getCount(context: Context, contentUri: Uri, bucketId: String): Int {
        context.contentResolver.query(contentUri, null, MediaStore.Images.Media.BUCKET_ID + "=?", arrayOf(bucketId), null)
                .use { cursor ->
                    return if (cursor == null || !cursor.moveToFirst()) 0 else cursor.count
                }
    }


    private fun getCover(context: Context, contentUri: Uri, bucketId: String): Int {
        context.contentResolver.query(contentUri, null, MediaStore.Images.Media.BUCKET_ID + "=?", arrayOf(bucketId), null)
                .use { cursor ->
                    return if (cursor == null || !cursor.moveToFirst()) 0 else cursor.count
                }
    }

    data class Album(
//            val buckedId: String,
//            val bucketName: String,
//            val count: Int,
//            val data: String,
            val fileId: String?
    )

    @JvmStatic
    fun getAlbumFileIdsAsBundle(context: Context, includeVideo: Boolean): Bundle {
        val albums = get(context, includeVideo)
        return Bundle().apply {
            putStringArrayList(
                    ARG_IDS, ArrayList(albums.mapNotNull { it.fileId })
            )
        }
    }

    @JvmStatic
    fun idsBundleToSelection(args: Bundle?): String {
        val ids = args?.getStringArrayList(ARG_IDS) ?: emptyList<String>()

        return ids.map { _id ->
            "(${MediaStore.MediaColumns._ID} == $_id)"
        }.joinToString(" OR ")
    }

    const val ARG_IDS = "ARG_IDS"
}