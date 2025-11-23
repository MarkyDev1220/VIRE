package com.example.vire.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class TournamentAdapter(
    initial: List<TournamentRequest>,
    private val onAction: (TournamentRequest, Action) -> Unit
) : ListAdapter<TournamentRequest, TournamentAdapter.VH>(DIFF) {

    enum class Action { VIEW, EDIT, DELETE }

    init { submitList(initial) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tournament, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName = itemView.findViewById<TextView>(R.id.tournamentName)
        private val tvGame = itemView.findViewById<TextView>(R.id.tournamentGame)
        private val tvPlayers = itemView.findViewById<TextView>(R.id.tournamentPlayers)
        private val tvOrganizer = itemView.findViewById<TextView>(R.id.tournamentOrganizer)
        private val btnMore = itemView.findViewById<ImageButton>(R.id.tournamentMore)

        fun bind(item: TournamentRequest) {
            tvName.text = item.name
            tvGame.text = item.game
            tvPlayers.text = "Min players: ${item.minPlayers} (max 32)"
            tvOrganizer.text = "Organizer: ${item.organizer}"

            itemView.setOnClickListener { onAction(item, Action.VIEW) }

            btnMore.setOnClickListener { anchor ->
                try {
                    val popup = PopupMenu(itemView.context, anchor)
                    val MENU_EDIT = 1
                    val MENU_DELETE = 2
                    popup.menu.add(0, MENU_EDIT, 0, "Edit")
                    popup.menu.add(0, MENU_DELETE, 1, "Delete")
                    popup.setOnMenuItemClickListener { mi ->
                        when (mi.itemId) {
                            MENU_EDIT -> onAction(item, Action.EDIT)
                            MENU_DELETE -> onAction(item, Action.DELETE)
                        }
                        true
                    }
                    popup.show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    onAction(item, Action.VIEW)
                }
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TournamentRequest>() {
            override fun areItemsTheSame(old: TournamentRequest, new: TournamentRequest) = old.id == new.id
            override fun areContentsTheSame(old: TournamentRequest, new: TournamentRequest) = old == new
        }
    }
}