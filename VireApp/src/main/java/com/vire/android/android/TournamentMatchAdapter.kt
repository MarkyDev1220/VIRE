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

class TournamentMatchAdapter(
    initial: List<TournamentMatch> = emptyList(),
    private val onReportScoreClick: (TournamentMatch) -> Unit
) : ListAdapter<TournamentMatch, TournamentMatchAdapter.VH>(DIFF) {

    init {
        submitList(initial)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tournament_match, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHeader: TextView = itemView.findViewById(R.id.matchHeader)
        private val tvStatus: TextView = itemView.findViewById(R.id.matchStatus)
        private val tvP1: TextView = itemView.findViewById(R.id.matchPlayer1)
        private val tvScore1: TextView = itemView.findViewById(R.id.matchScore1)
        private val tvP2: TextView = itemView.findViewById(R.id.matchPlayer2)
        private val tvScore2: TextView = itemView.findViewById(R.id.matchScore2)
        private val tvWinner: TextView = itemView.findViewById(R.id.matchWinner)
        private val btnScore: Button = itemView.findViewById(R.id.btnScoreMatch)

        fun bind(match: TournamentMatch) {
            tvHeader.text = "Round ${match.roundNumber} • Match ${match.matchNumber}"
            tvP1.text = match.player1Name
            tvP2.text = match.player2Name
            tvScore1.text = match.player1Score.toString()
            tvScore2.text = match.player2Score.toString()

            if (match.isCompleted) {
                tvStatus.text = "Completed ✓"
                tvStatus.setTextColor(Color.parseColor("#2E7D32"))

                val winnerName = when (match.winnerUid) {
                    match.player1Uid -> match.player1Name
                    match.player2Uid -> match.player2Name
                    else -> "Draw / Bye"
                }
                tvWinner.text = "🏆 Winner: $winnerName"
                tvWinner.visibility = View.VISIBLE
                btnScore.text = "Edit Score"
            } else {
                tvStatus.text = "Pending"
                tvStatus.setTextColor(Color.parseColor("#757575"))
                tvWinner.visibility = View.GONE
                btnScore.text = "Report Score"
            }

            btnScore.setOnClickListener { onReportScoreClick(match) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TournamentMatch>() {
            override fun areItemsTheSame(oldItem: TournamentMatch, newItem: TournamentMatch): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: TournamentMatch, newItem: TournamentMatch): Boolean =
                oldItem == newItem
        }
    }
}
