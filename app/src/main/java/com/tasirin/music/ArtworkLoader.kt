package com.tasirin.music

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import android.widget.ImageView
import java.util.concurrent.Executors

/**
 * Memuat sampul album dari MediaStore secara asinkron dengan cache kecil.
 * Dipakai daftar lagu (gambar kecil) dan PlayerService (ikon notifikasi).
 */
object ArtworkLoader {

    private val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")

    private val cache = LruCache<Long, Bitmap>(64)
    private val executor = Executors.newFixedThreadPool(2)

    fun loadBitmap(context: Context, albumId: Long): Bitmap? {
        if (albumId <= 0L) return null
        cache.get(albumId)?.let { return it }
        val uri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId)
        val bitmap = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        }.getOrNull()
        if (bitmap != null) {
            cache.put(albumId, bitmap)
        }
        return bitmap
    }

    fun loadInto(context: Context, albumId: Long, imageView: ImageView) {
        if (albumId <= 0L) {
            imageView.tag = null
            imageView.setImageResource(R.drawable.ic_music_note)
            return
        }
        cache.get(albumId)?.let {
            imageView.tag = albumId
            imageView.setImageBitmap(it)
            return
        }
        imageView.tag = albumId
        imageView.setImageResource(R.drawable.ic_music_note)
        val appContext = context.applicationContext
        executor.execute {
            val bitmap = loadBitmap(appContext, albumId)
            if (bitmap != null) {
                imageView.post {
                    // Cegah gambar lama menimpa baris yang sudah didaur ulang.
                    if (imageView.tag == albumId) {
                        imageView.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }
}
