package com.tasirin.music

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tasirin.music.databinding.ItemCategoryBinding

/** Satu baris kategori: album, artis, folder, atau genre. */
data class CategoryItem(
    val key: String,
    val name: String,
    val sub: String,
    val count: Int,
    val albumId: Long = -1L
)

class CategoryAdapter(
    private val onClick: (CategoryItem) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.Holder>() {

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
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int = shown.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = shown[position]
        holder.binding.catName.text = item.name
        holder.binding.catSub.text =
            if (item.sub.isBlank()) "${item.count} lagu" else "${item.sub} • ${item.count} lagu"
        if (item.albumId >= 0L) {
            holder.binding.catArt.visibility = View.VISIBLE
            ArtworkLoader.loadInto(holder.itemView.context, item.albumId, holder.binding.catArt)
        } else {
            holder.binding.catArt.visibility = View.GONE
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)
}
