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

class CommunityAdapter(
    initial: List<Community> = emptyList(),
    private val currentUid: String = "",
    private val onCommunityClick: (Community) -> Unit,
    private val onJoinToggleClick: (Community) -> Unit
) : ListAdapter<Community, CommunityAdapter.VH>(DIFF) {

    init {
        submitList(initial)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_community, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.communityName)
        private val tvCategory: TextView = itemView.findViewById(R.id.communityCategory)
        private val tvDesc: TextView = itemView.findViewById(R.id.communityDescription)
        private val tvMembers: TextView = itemView.findViewById(R.id.communityMemberCount)
        private val btnJoin: Button = itemView.findViewById(R.id.btnJoinCommunity)

        fun bind(c: Community) {
            tvName.text = c.name
            tvCategory.text = c.category
            tvDesc.text = c.description
            tvMembers.text = "👥 ${c.members.size} Members"

            val isMember = c.members.contains(currentUid)
            if (isMember) {
                btnJoin.text = "Joined ✓"
                btnJoin.setBackgroundColor(Color.parseColor("#455A64"))
            } else {
                btnJoin.text = "Join"
                btnJoin.setBackgroundColor(Color.parseColor("#1565C0"))
            }

            itemView.setOnClickListener { onCommunityClick(c) }
            btnJoin.setOnClickListener { onJoinToggleClick(c) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Community>() {
            override fun areItemsTheSame(oldItem: Community, newItem: Community): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Community, newItem: Community): Boolean =
                oldItem == newItem
        }
    }
}
