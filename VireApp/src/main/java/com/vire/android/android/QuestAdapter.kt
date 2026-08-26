package com.vire.android.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vire.android.R

class QuestAdapter(
    initial: List<QuestRequest>,
    private val onAction: (QuestRequest, Action) -> Unit
) : ListAdapter<QuestRequest, QuestAdapter.VH>(DIFF) {

    enum class Action { VIEW, EDIT, DELETE }

    init { submitList(initial) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_quest, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle = itemView.findViewById<TextView>(R.id.questTitle)
        private val tvType = itemView.findViewById<TextView>(R.id.questType)
        private val tvGame = itemView.findViewById<TextView>(R.id.questGame)
        private val tvPoster = itemView.findViewById<TextView>(R.id.questPoster)
        private val btnMore = itemView.findViewById<ImageButton>(R.id.questMore)

        fun bind(item: QuestRequest) {
            tvTitle.text = item.title
            tvType.text = item.type.name
            tvGame.text = item.game
            tvPoster.text = "By: ${item.poster}"

            itemView.setOnClickListener { onAction(item, Action.VIEW) }

            btnMore.setOnClickListener { anchor ->
                try {
                    val popup = PopupMenu(itemView.context, anchor)
                    val EDIT = 1
                    val DELETE = 2
                    popup.menu.add(0, EDIT, 0, "Edit")
                    popup.menu.add(0, DELETE, 1, "Delete")
                    popup.setOnMenuItemClickListener { mi ->
                        when (mi.itemId) {
                            EDIT -> onAction(item, Action.EDIT)
                            DELETE -> onAction(item, Action.DELETE)
                        }
                        true
                    }
                    popup.show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<QuestRequest>() {
            override fun areItemsTheSame(old: QuestRequest, new: QuestRequest) = old.id == new.id
            override fun areContentsTheSame(old: QuestRequest, new: QuestRequest) = old == new
        }
    }
}