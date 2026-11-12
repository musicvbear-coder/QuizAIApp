package com.example.quizaiapp.data.presentation.quizz

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.quizaiapp.R
import com.example.quizaiapp.data.repository.SubmissionItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LeaderboardAdapter(private var listSubmissions: List<SubmissionItem>) :
    RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    class LeaderboardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRankBadge: TextView = itemView.findViewById(R.id.tvRankBadge)
        val tvStudentName: TextView = itemView.findViewById(R.id.tvStudentName)
        val tvSubmitTime: TextView = itemView.findViewById(R.id.tvSubmitTime)
        val tvStudentScore: TextView = itemView.findViewById(R.id.tvStudentScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bxh, parent, false)
        return LeaderboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val item = listSubmissions[position]
        val rank = position + 1

        holder.tvRankBadge.text = when (rank) {
            1 -> "🥇"
            2 -> "🥈"
            3 -> "🥉"
            else -> "#$rank"
        }

        holder.tvStudentName.text = item.userName
        val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        holder.tvSubmitTime.text = "Nộp lúc: ${dateFormat.format(Date(item.submittedAt))}"
        holder.tvStudentScore.text = String.format(Locale.getDefault(), "%.1f đ", item.score)
    }

    override fun getItemCount(): Int = listSubmissions.size

    fun updateList(newList: List<SubmissionItem>) {
        listSubmissions = newList
        notifyDataSetChanged()
    }
}