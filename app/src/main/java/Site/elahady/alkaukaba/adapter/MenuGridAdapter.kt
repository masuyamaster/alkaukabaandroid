package site.elahady.alkaukaba.adapter

import site.elahady.alkaukaba.databinding.ItemMenuGridBinding
import site.elahady.alkaukaba.model.MenuItem
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class MenuGridAdapter(
    private val items: List<MenuItem>,
    private val onItemClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuGridAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMenuGridBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMenuGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.ivMenuIcon.setImageResource(item.iconRes)
        holder.binding.ivMenuIcon.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                holder.itemView.context.getColor(item.iconBackgroundTint)
            )
        holder.binding.ivMenuIcon.imageTintList =
            android.content.res.ColorStateList.valueOf(
                holder.itemView.context.getColor(item.iconTint)
            )
        holder.binding.tvMenuLabel.text = item.label
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
