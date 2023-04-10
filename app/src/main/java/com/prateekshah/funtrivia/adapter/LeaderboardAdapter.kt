package com.prateekshah.funtrivia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.funtrivia.R
import com.example.funtrivia.databinding.LeaderboardItemBinding
import com.prateekshah.funtrivia.firestore.Player

class LeaderboardAdapter(private val playerList: ArrayList<Player>): RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LeaderboardAdapter.LeaderboardViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return LeaderboardViewHolder(
            LeaderboardItemBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: LeaderboardAdapter.LeaderboardViewHolder, position: Int) {
        val player: Player = playerList[position]
        holder.pName.text = holder.binding.root.resources.getString(R.string.player_name_2, player.Name)
        holder.pScore.text = holder.binding.root.resources.getString(R.string.score, player.Score)
        holder.pAvgTimePerQuestion.text = holder.binding.root.resources.getString(R.string.avg_time_per_question, player.AvgTimePerQuestion)
        holder.pRank.text = (position+1).toString()
    }

    override fun getItemCount(): Int {
        return playerList.size
    }

    class LeaderboardViewHolder(
        var binding: LeaderboardItemBinding
    ): RecyclerView.ViewHolder(binding.root) {

        val pName = binding.playerName
        val pScore = binding.score
        val pAvgTimePerQuestion = binding.avgTimePerQuestion
        val pRank = binding.rank
    }
}