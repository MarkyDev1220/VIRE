package com.example.vire.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LeaderboardAdapter(
    private var players: List<Player>
) : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rankNumber: TextView = view.findViewById(R.id.tvRankNumber)
        val username: TextView = view.findViewById(R.id.tvUsername)
        val points: TextView = view.findViewById(R.id.tvPoints)
        val gameName: TextView = view.findViewById(R.id.tvGameName)
        val avatar: ImageView = view.findViewById(R.id.imgPlayerAvatar)
        val wlt: TextView = view.findViewById(R.id.tvWLT) // Wins/Losses/Ties
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard_player, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val player = players[position]
        holder.rankNumber.text = (position + 1).toString()
        holder.username.text = player.username
        holder.points.text = "${player.points} pts"
        holder.gameName.text = "Game: ${player.game}"
        holder.avatar.setImageResource(R.drawable.ic_profile_placeholder)
        holder.wlt.text = "W: ${player.wins} | L: ${player.losses} | T: ${player.ties}" // display stats
    }

    override fun getItemCount(): Int = players.size

    /** Update leaderboard dynamically */
    fun updateLeaderboard(newPlayers: List<Player>) {
        players = newPlayers
        notifyDataSetChanged()
    }
}
