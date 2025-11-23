package com.example.vire.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ChallengeAdapter(
    initial: List<ChallengeRequest>,
    private val onAction: (ChallengeRequest, Action) -> Unit
) : ListAdapter<ChallengeRequest, ChallengeAdapter.VH>(DIFF) {

    enum class Action { VIEW, ACCEPT, DELETE, EDIT }

    init { submitList(initial) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_challenge, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle = itemView.findViewById<TextView>(R.id.challengeTitle)
        private val tvGame = itemView.findViewById<TextView>(R.id.challengeGame)
        private val tvChallenger = itemView.findViewById<TextView>(R.id.challengeChallenger)
        private val tvOpponent = itemView.findViewById<TextView>(R.id.challengeOpponent)
        private val tvDesc = itemView.findViewById<TextView>(R.id.challengeDesc)
        private val btnAccept = itemView.findViewById<Button>(R.id.btnAccept)
        private val btnMore = itemView.findViewById<ImageButton>(R.id.btnDelete) // three-dot button in layout

        fun bind(item: ChallengeRequest) {
            tvTitle.text = item.title
            tvGame.text = item.game
            tvChallenger.text = "Challenger: ${item.challenger}"
            tvOpponent.text = "Opponent: ${item.opponent ?: "Open"}"
            tvDesc.text = item.safeDescription()


            itemView.setOnClickListener { onAction(item, Action.VIEW) }

            btnAccept.isEnabled = item.status == ChallengeRequest.Status.OPEN
            btnAccept.setOnClickListener { onAction(item, Action.ACCEPT) }

            // Popup menu
            val MENU_EDIT = 1
            val MENU_DELETE = 2

            btnMore?.setOnClickListener { view ->
                val popup = PopupMenu(itemView.context, view)
                popup.menu.add(0, MENU_EDIT, 0, "Edit")
                popup.menu.add(0, MENU_DELETE, 1, "Delete")
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        MENU_EDIT -> onAction(item, Action.EDIT)
                        MENU_DELETE -> onAction(item, Action.DELETE)
                    }
                    true
                }
                popup.show()
            }
        }

    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ChallengeRequest>() {
            override fun areItemsTheSame(old: ChallengeRequest, new: ChallengeRequest) = old.id == new.id
            override fun areContentsTheSame(old: ChallengeRequest, new: ChallengeRequest) = old == new
        }
    }
}