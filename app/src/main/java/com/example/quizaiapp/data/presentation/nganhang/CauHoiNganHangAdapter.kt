package com.example.quizaiapp.data.presentation.nganhang

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.quizaiapp.R
import com.example.quizaiapp.data.local.entity.CauHoi

class CauHoiNganHangAdapter(
    private var listCauHoi: List<CauHoi>,
    private val onDeleteClick: (CauHoi) -> Unit
) : RecyclerView.Adapter<CauHoiNganHangAdapter.QuestionViewHolder>() {

    class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuestionDifficulty: TextView = itemView.findViewById(R.id.tvQuestionDifficulty)
        val btnDeleteQuestion: ImageView = itemView.findViewById(R.id.btnDeleteQuestion)
        val tvBankQuestionContent: TextView = itemView.findViewById(R.id.tvBankQuestionContent)
        val tvBankCorrectAnswer: TextView = itemView.findViewById(R.id.tvBankCorrectAnswer)
        val tvBankSubjectInfo: TextView = itemView.findViewById(R.id.tvBankSubjectInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cau_hoi_ngan_hang, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val item = listCauHoi[position]

        holder.tvBankQuestionContent.text = item.noiDungCauHoi
        holder.tvBankCorrectAnswer.text = "Đáp án đúng: ${item.dapAnDung}"
        holder.tvQuestionDifficulty.text = when (item.mucDoKho) {
            1 -> "Dễ / Nhận biết"
            2 -> "Trung bình / Thông hiểu"
            else -> "Khó / Vận dụng"
        }
        holder.tvBankSubjectInfo.text = "Mã câu hỏi: #${item.idCauHoi} · Mã chương: ${item.idChuong}"

        holder.btnDeleteQuestion.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount(): Int = listCauHoi.size

    fun updateList(newList: List<CauHoi>) {
        listCauHoi = newList
        notifyDataSetChanged()
    }
}