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
    private var currentIndex: Int = -1

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

    fun originalIndexOf(shownPosition: Int): Int = allSongs.indexOf(shown[shownPosition])

    fun setCurrent(index: Int) {
        val oldShown = shownPositionOf(currentIndex)
        currentIndex = index
        if (oldShown >= 0) notifyItemChanged(oldShown)
        val newShown = shownPositionOf(index)
        if (newShown >= 0) notifyItemChanged(newShown)
    }

    private fun shownPositionOf(allIndex: Int): Int {
        if (allIndex !in allSongs.indices) return -1
        return shown.indexOf(allSongs[allIndex])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int = shown.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val song = shown[position]
        holder.binding.songTitle.text = song.title
        holder.binding.songArtist.text = song.artist
        holder.binding.songDuration.text = formatDuration(song.durationMs)
        val isCurrent = allSongs.indexOf(song) == currentIndex
        holder.binding.songTitle.typeface =
            if (isCurrent) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        holder.binding.songTitle.setTextColor(
            if (isCurrent) 0xFF5E35B1.toInt() else 0xFF212121.toInt()
        )
        holder.itemView.setOnClickListener { onClick(position) }
    }

    class Holder(val binding: ItemSongBinding) : RecyclerView.ViewHolder(binding.root)
}
