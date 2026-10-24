package com.example.quizaiapp.data.presentation.nganhang

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.quizaiapp.R
import com.example.quizaiapp.data.local.entity.LichSuDeThi
import java.util.Locale

class LichSuDeThiAdapter(
    private var listDeThi: List<LichSuDeThi>,
    private val onLamThuClick: (LichSuDeThi) -> Unit,
    private val onXuatFileClick: (LichSuDeThi) -> Unit
) : RecyclerView.Adapter<LichSuDeThiAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSubjectTitle: TextView = itemView.findViewById(R.id.tvSubjectTitle)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvQuestionCount: TextView? = itemView.findViewById(R.id.tvQuestionCount)
        val tvDuration: TextView? = itemView.findViewById(R.id.tvDuration)
        val tvCodeCount: TextView? = itemView.findViewById(R.id.tvCodeCount)
        val btnLamThu: Button = itemView.findViewById(R.id.btnLamThu)
        val btnXuatFile: Button = itemView.findViewById(R.id.btnXuatFile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lich_su_de_thi, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = listDeThi[position]

        holder.tvSubjectTitle.text = item.chuDeKiemsTra
        holder.tvStatus.text = if (item.diemDatDuoc > 0f) String.format(Locale.getDefault(), "✓ Điểm: %.1f/10", item.diemDatDuoc) else "✓ Sẵn sàng in"

        holder.tvQuestionCount?.text = "📖 ${item.tongSoCau} câu"
        holder.tvDuration?.text = "🕒 15 phút"
        holder.tvCodeCount?.text = "📚 4 mã đề"

        holder.btnLamThu.setOnClickListener { onLamThuClick(item) }
        holder.btnXuatFile.setOnClickListener { onXuatFileClick(item) }
    }

    override fun getItemCount(): Int = listDeThi.size

    fun updateList(newList: List<LichSuDeThi>) {
        listDeThi = newList
        notifyDataSetChanged()
    }
}