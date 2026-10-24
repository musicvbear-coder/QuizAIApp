package com.example.quizaiapp.data.presentation.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quizaiapp.R
import com.example.quizaiapp.data.local.KetNoiCoSoDuLieu
import com.example.quizaiapp.data.local.SessionManager
import com.example.quizaiapp.data.presentation.ai.AiQuizActivity
import com.example.quizaiapp.data.presentation.nganhang.LichSuDeThiAdapter
import com.example.quizaiapp.data.presentation.nganhang.XuatDeThiBottomSheetFragment
import com.example.quizaiapp.data.presentation.quizz.QuizActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TrangChuFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trang_chu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvWelcomeName = view.findViewById<TextView>(R.id.tvWelcomeName)
        val tvUserInitials = view.findViewById<TextView>(R.id.tvUserInitials)
        val cardOpenAiQuiz = view.findViewById<View>(R.id.cardOpenAiQuiz)
        val btnGenerateAi = view.findViewById<Button>(R.id.btnGenerateAi)
        val tvViewAllHistory = view.findViewById<TextView>(R.id.tvViewAllHistory)
        val recyclerViewRecentExams = view.findViewById<RecyclerView>(R.id.recyclerViewRecentExams)

        val sessionManager = SessionManager(requireContext().applicationContext)
        val userName = sessionManager.layTenNguoiDung()
        tvWelcomeName.text = "$userName 👋"
        if (userName.isNotBlank()) {
            val words = userName.trim().split(" ")
            val initials = if (words.size >= 2) {
                "${words.first().take(1)}${words.last().take(1)}".uppercase()
            } else {
                userName.take(2).uppercase()
            }
            tvUserInitials.text = initials
        }

        val openAiAction = {
            startActivity(Intent(requireContext(), AiQuizActivity::class.java))
        }

        cardOpenAiQuiz?.setOnClickListener { openAiAction() }
        btnGenerateAi?.setOnClickListener { openAiAction() }

        tvViewAllHistory.setOnClickListener {
            (activity as? MainActivity)?.switchFragmentByMenuId(R.id.nav_bank)
        }

        recyclerViewRecentExams.layoutManager = LinearLayoutManager(requireContext())

        val db = KetNoiCoSoDuLieu.layInstance(requireContext().applicationContext)
        viewLifecycleOwner.lifecycleScope.launch {
            db.truyVanDeThiDao().layToanBoLichSuDeThi().collectLatest { list ->
                val recentList = list.take(5)
                val adapter = LichSuDeThiAdapter(
                    listDeThi = recentList,
                    onLamThuClick = { deThi ->
                        val intent = Intent(requireContext(), QuizActivity::class.java).apply {
                            putExtra("EXTRA_ID_DE_THI", deThi.idDeThi)
                        }
                        startActivity(intent)
                    },
                    onXuatFileClick = { deThi ->
                        val dialog = XuatDeThiBottomSheetFragment.newInstance(deThi.idDeThi)
                        dialog.show(parentFragmentManager, "XuatDeThiBottomSheet")
                    }
                )
                recyclerViewRecentExams.adapter = adapter
            }
        }
    }
}