/*
 *     Copyright (C) 2023 AkaneWork Organization
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.symphonica.logic.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.widget.ImageView
import com.bumptech.glide.Glide
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.R
import org.akanework.symphonica.SymphonicaApplication.Companion.context
import org.akanework.symphonica.logic.data.Album
import org.akanework.symphonica.logic.data.Song

fun getAllAlbums(externalSongList: List<Song>): List<Album> {
    val albumsMap = mutableMapOf<String, MutableList<Song>>()

    for (song in externalSongList) {
        val albumKey = "${song.album}_${song.artist}"
        val albumSongs = albumsMap.getOrPut(albumKey) { mutableListOf() }
        albumSongs.add(song)
    }

    val albums = mutableListOf<Album>()

    for ((albumKey, songList) in albumsMap) {
        val (albumTitle, artist) = albumKey.split("_")
        val cover = null
        val album = Album(albumTitle, artist, cover, songList)
        albums.add(album)
    }

    return albums
}

/**
 * Since re-ordering the albumList at libraryAlbumDisplayFragment
 * is too slow, we offer this choice. While you enjoy your music,
 * we will re-order the albumList in the background. When re-ordering
 * is ready, we'll offer the re-ordered list. But before that, we'll
 * offer the original list. Don't blame me :)
 * This function used [countingSortSongsByTrackNumber] which is redis sorting.
 * should be fast anyways.
 * I'll leave the switch for re-ordering the albumList at the beginning.
 * Thank you for your understanding.
 */
fun sortAlbumListByTrackNumber(albumList: List<Album>): List<Album> {
    val maxTrackNumber = albumList
        .flatMap { album -> album.songList }
        .mapNotNull { song -> getTrackNumber(song.path)?.toInt() }
        .maxOrNull() ?: 0

    val sortedAlbumList = albumList.map { album ->
        val countingSortSongList = countingSortSongsByTrackNumber(album.songList, maxTrackNumber)
        album.copy(songList = countingSortSongList)
    }

    return sortedAlbumList
}

fun countingSortSongsByTrackNumber(songList: List<Song>, maxTrackNumber: Int): List<Song> {
    val count = IntArray(maxTrackNumber + 1) { 0 }

    for (song in songList) {
        val trackNumber = getTrackNumber(song.path)?.toInt() ?: 0
        count[trackNumber]++
    }

    for (i in 1 until count.size) {
        count[i] += count[i - 1]
    }

    val sortedSongList = MutableList(songList.size) { songList[it] }
    val output = MutableList(songList.size) { Song(0, "", "", "", 0, "", null) }

    for (i in songList.size - 1 downTo 0) {
        val trackNumber = getTrackNumber(sortedSongList[i].path)?.toInt() ?: 0
        val index = count[trackNumber] - 1
        output[index] = sortedSongList[i]
        count[trackNumber]--
    }

    return output
}


fun getAllSongs(context: Context): List<Song> {
    val selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0"
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.YEAR,
        MediaStore.Audio.Media.ALBUM_ID
    )
    val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"

    val songs = mutableListOf<Song>()
    val cursor = context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        null,
        sortOrder
    )

    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val pathColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

        while (it.moveToNext()) {
            val id = it.getLong(idColumn)
            val title = it.getString(titleColumn)
            val artist = it.getString(artistColumn)
            val album = it.getString(albumColumn)
            val duration = it.getLong(durationColumn)
            val path = it.getString(pathColumn)
            val albumId = it.getLong(albumIdColumn)

            val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
            val imgUri = ContentUris.withAppendedId(
                sArtworkUri,
                albumId
            )

            val song = Song(id, title, artist, album, duration, path, imgUri)
            songs.add(song)
        }
    }
    cursor?.close()

    return songs
}

fun getTrackNumber(songUri: String): String? {
    val projection = arrayOf(MediaStore.Audio.Media.TRACK)
    val selection = "${MediaStore.Audio.Media.DATA} = ?"
    val selectionArgs = arrayOf(songUri)
    val cursor = context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
    )

    var trackNumber: String? = null
    cursor?.use {
        if (cursor.moveToFirst()) {
            trackNumber =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK))
        }
    }

    cursor?.close()

    if (trackNumber != null && trackNumber.toString().length == 4) {
        return trackNumber!!.substring(1).trimStart('0')
    } else if (trackNumber != null) {
        return trackNumber!!.trimStart('0')
    }
    return null
}

fun getYear(songUri: String): String? {
    val projection = arrayOf(MediaStore.Audio.Media.YEAR)
    val selection = "${MediaStore.Audio.Media.DATA} = ?"
    val selectionArgs = arrayOf(songUri)
    val cursor = context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
    )

    var year: String? = null
    cursor?.use {
        if (cursor.moveToFirst()) {
            year =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR))
        }
    }

    cursor?.close()
    return year
}

fun fillSongCover(imgUri: Uri, songCover: ImageView) {
    Glide.with(context)
        .load(imgUri)
        .diskCacheStrategy(MainActivity.diskCacheStrategyCustom)
        .placeholder(R.drawable.ic_album_default_cover)
        .into(songCover)
}