package com.tasirin.music

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tasirin.music.databinding.ItemAlbumBinding

/** Kotak-kotak sampul album seperti tampilan grid Musicolet. */
class AlbumAdapter(
    private val onClick: (CategoryItem) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.Holder>() {

    private var all: List<CategoryItem> = emptyList()
    private var shown: List<CategoryItem> = emptyList()

    fun submit(items: List<CategoryItem>) {
        all = items
        shown = items
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val q = query.trim().lowercase()
        shown = if (q.isEmpty()) {
            all
        } else {
            all.filter { it.name.lowercase().contains(q) }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int = shown.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = shown[position]
        holder.binding.albumName.text = item.name
        holder.binding.albumArtist.text = item.sub
        ArtworkLoader.loadInto(holder.itemView.context, item.albumId, holder.binding.albumArt)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root)
}
