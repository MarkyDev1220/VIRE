package com.vire.android.android

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vire.android.R

class TournamentAdapter(
    initial: List<Tournament> = emptyList(),
    private val currentUid: String = "",
    private val onTournamentClick: (Tournament) -> Unit,
    private val onRegisterClick: (Tournament) -> Unit
) : ListAdapter<Tournament, TournamentAdapter.VH>(DIFF) {

    init {
        submitList(initial)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tournament, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tournamentName)
        private val tvStatus: TextView = itemView.findViewById(R.id.tournamentStatus)
        private val tvGameSystem: TextView = itemView.findViewById(R.id.tournamentGameSystem)
        private val tvFormat: TextView = itemView.findViewById(R.id.tournamentFormat)
        private val tvDateLoc: TextView = itemView.findViewById(R.id.tournamentDateLocation)
        private val tvFee: TextView = itemView.findViewById(R.id.tournamentEntryFee)
        private val tvPlayerCount: TextView = itemView.findViewById(R.id.tournamentPlayerCount)
        private val btnJoin: Button = itemView.findViewById(R.id.btnJoinTournament)

        fun bind(tournament: Tournament) {
            tvName.text = tournament.name
            tvStatus.text = tournament.status
            tvGameSystem.text = tournament.gameSystem
            tvFormat.text = tournament.format

            val dateLocStr = listOf(tournament.date, tournament.time, tournament.location)
                .filter { it.isNotBlank() }
                .joinToString(" • ")
            tvDateLoc.text = if (dateLocStr.isNotBlank()) "📅 $dateLocStr" else "📅 Schedule TBD"

            tvFee.text = "Fee: ${tournament.entryFee.ifBlank { "Free" }}"
            tvPlayerCount.text = "👥 ${tournament.participants.size} / ${tournament.maxParticipants} Players"

            val isRegistered = tournament.participants.contains(currentUid)
            if (isRegistered) {
                btnJoin.text = "Registered ✓"
                btnJoin.isEnabled = true
                btnJoin.setBackgroundColor(Color.parseColor("#455A64"))
            } else if (tournament.participants.size >= tournament.maxParticipants) {
                btnJoin.text = "Full"
                btnJoin.isEnabled = false
                btnJoin.setBackgroundColor(Color.parseColor("#9E9E9E"))
            } else {
                btnJoin.text = "Join Event"
                btnJoin.isEnabled = true
                btnJoin.setBackgroundColor(Color.parseColor("#1565C0"))
            }

            itemView.setOnClickListener { onTournamentClick(tournament) }
            btnJoin.setOnClickListener { onRegisterClick(tournament) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Tournament>() {
            override fun areItemsTheSame(oldItem: Tournament, newItem: Tournament): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Tournament, newItem: Tournament): Boolean =
                oldItem == newItem
        }
    }
}
