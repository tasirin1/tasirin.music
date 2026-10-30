package com.tasirin.music

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tasirin.music.databinding.ItemSongBinding

class SongAdapter(
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<SongAdapter.Holder>() {

    private var allSongs: List<Song> = emptyList()
    private var shown: List<Song> = emptyList()
    private var currentSongId: Long = -1L

    fun submit(songs: List<Song>) {
        allSongs = songs
        shown = songs
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val q = query.trim().lowercase()
        shown = if (q.isEmpty()) {
            allSongs
        } else {
            allSongs.filter {
                it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
            }
        }
        notifyDataSetChanged()
    }

    /** Id lagu-lagu yang sedang tampil, sesuai urutan tampil (untuk konteks putar). */
    fun shownIds(): LongArray = LongArray(shown.size) { shown[it].id }

    fun songAt(shownPosition: Int): Song? = shown.getOrNull(shownPosition)

    fun setCurrentId(songId: Long) {
        if (songId == currentSongId) return
        val oldPos = shown.indexOfFirst { it.id == currentSongId }
        currentSongId = songId
        if (oldPos >= 0) notifyItemChanged(oldPos)
        val newPos = shown.indexOfFirst { it.id == songId }
        if (newPos >= 0) notifyItemChanged(newPos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int = shown.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val song = shown[position]
        holder.binding.songTitle.text = song.title
        holder.binding.songArtist.text =
            if (song.album.isBlank()) song.artist else "${song.artist} • ${song.album}"
        holder.binding.songDuration.text = formatDuration(song.durationMs)
        holder.binding.songArtist.setTextColor(0xFFBDBDBD.toInt())
        holder.binding.songDuration.setTextColor(0xFF9E9E9E.toInt())
        ArtworkLoader.loadInto(holder.itemView.context, song.albumId, holder.binding.songArt)
        val isCurrent = song.id == currentSongId
        holder.binding.songTitle.typeface =
            if (isCurrent) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        holder.binding.songTitle.setTextColor(
            if (isCurrent) 0xFFB388FF.toInt() else 0xFFF5F5F5.toInt()
        )
        holder.itemView.setOnClickListener { onClick(position) }
    }

    class Holder(val binding: ItemSongBinding) : RecyclerView.ViewHolder(binding.root)
}
