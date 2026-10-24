package com.example.quizaiapp.data.presentation.ai

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.quizaiapp.R
import com.example.quizaiapp.data.local.entity.ChiTietCauHoiAi

class AiQuizPreviewAdapter(private var listCauHoi: List<ChiTietCauHoiAi>) :
    RecyclerView.Adapter<AiQuizPreviewAdapter.QuestionViewHolder>() {

    class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuestionContent: TextView = itemView.findViewById(R.id.tvQuestionContent)

        val layoutOptionA: LinearLayout = itemView.findViewById(R.id.layoutOptionA)
        val tvCircleA: TextView = itemView.findViewById(R.id.tvCircleA)
        val tvOptionA: TextView = itemView.findViewById(R.id.tvOptionA)
        val imgCheckA: ImageView = itemView.findViewById(R.id.imgCheckA)

        val layoutOptionB: LinearLayout = itemView.findViewById(R.id.layoutOptionB)
        val tvCircleB: TextView = itemView.findViewById(R.id.tvCircleB)
        val tvOptionB: TextView = itemView.findViewById(R.id.tvOptionB)
        val imgCheckB: ImageView = itemView.findViewById(R.id.imgCheckB)

        val layoutOptionC: LinearLayout = itemView.findViewById(R.id.layoutOptionC)
        val tvCircleC: TextView = itemView.findViewById(R.id.tvCircleC)
        val tvOptionC: TextView = itemView.findViewById(R.id.tvOptionC)
        val imgCheckC: ImageView = itemView.findViewById(R.id.imgCheckC)

        val layoutOptionD: LinearLayout = itemView.findViewById(R.id.layoutOptionD)
        val tvCircleD: TextView = itemView.findViewById(R.id.tvCircleD)
        val tvOptionD: TextView = itemView.findViewById(R.id.tvOptionD)
        val imgCheckD: ImageView = itemView.findViewById(R.id.imgCheckD)

        val tvExplanation: TextView = itemView.findViewById(R.id.tvExplanation)

        fun bind(cauHoi: ChiTietCauHoiAi) {
            tvQuestionContent.text = cauHoi.noiDungCauHoi
            tvOptionA.text = cauHoi.phuongAnA
            tvOptionB.text = cauHoi.phuongAnB
            tvOptionC.text = cauHoi.phuongAnC
            tvOptionD.text = cauHoi.phuongAnD
            tvExplanation.text = cauHoi.loiGiaiThich

            resetAllOptions()

            when (cauHoi.dapAnChinhXac.uppercase()) {
                "A" -> highlightCorrect(layoutOptionA, tvCircleA, imgCheckA)
                "B" -> highlightCorrect(layoutOptionB, tvCircleB, imgCheckB)
                "C" -> highlightCorrect(layoutOptionC, tvCircleC, imgCheckC)
                "D" -> highlightCorrect(layoutOptionD, tvCircleD, imgCheckD)
            }
        }

        private fun highlightCorrect(layout: LinearLayout, circle: TextView, check: ImageView) {
            layout.setBackgroundResource(R.drawable.bg_option_correct)
            circle.setBackgroundResource(R.drawable.bg_circle_green)
            circle.setTextColor(Color.WHITE)
            check.visibility = View.VISIBLE
        }

        private fun resetAllOptions() {
            val options = listOf(
                Triple(layoutOptionA, tvCircleA, imgCheckA),
                Triple(layoutOptionB, tvCircleB, imgCheckB),
                Triple(layoutOptionC, tvCircleC, imgCheckC),
                Triple(layoutOptionD, tvCircleD, imgCheckD)
            )
            for ((layout, circle, check) in options) {
                layout.setBackgroundResource(R.drawable.bg_option_normal)
                circle.setBackgroundResource(R.drawable.bg_circle_gray)
                circle.setTextColor(Color.parseColor("#9CA3AF"))
                check.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ai_question_preview, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.bind(listCauHoi[position])
    }

    override fun getItemCount(): Int = listCauHoi.size
}