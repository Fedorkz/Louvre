//package com.andremion.louvre.data
//
//import android.content.Context
//import android.provider.MediaStore
//import java.util.*
//import kotlin.collections.LinkedHashMap
//
//fun getAlbums(context: Context): List<Album> {
//        val contentUri = MediaStore.Files.getContentUri("external")
//
//        val bucketOrderBy = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
//
//        val albumsMap = LinkedHashMap<Long, Album>()
//        val albums = LinkedList<Album>()
//        val c = context.contentResolver.query(contentUri, albumColumns, "_data IS NOT NULL AND ${MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}", null, bucketOrderBy)
//        if (c != null && c.moveToFirst()) {
//            do {
//                val album = Album.getAlbumFromCursor(c)
//
//                if (!albumsMap.containsKey(album.id)) {
//                    albumsMap[album.id] = album
//                } else {
//                    val curAlbum = albumsMap[album.id]
//                    curAlbum?.let {
//                        it.count++
//                    }
//                }
//            } while (c.moveToNext())
//        }
//
//        c?.close()
//        albums.addAll(albumsMap.values)
//
//        return albums
//    }