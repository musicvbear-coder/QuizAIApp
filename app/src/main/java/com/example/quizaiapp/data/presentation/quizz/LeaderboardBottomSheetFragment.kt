package com.example.quizaiapp.data.presentation.quizz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quizaiapp.R
import com.example.quizaiapp.data.repository.FirebaseRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class LeaderboardBottomSheetFragment : BottomSheetDialogFragment() {

    private var roomCode: String = ""

    companion object {
        private const val ARG_ROOM_CODE = "arg_room_code"

        fun newInstance(roomCode: String): LeaderboardBottomSheetFragment {
            val fragment = LeaderboardBottomSheetFragment()
            val args = Bundle().apply {
                putString(ARG_ROOM_CODE, roomCode)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomCode = arguments?.getString(ARG_ROOM_CODE) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_bxh, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvRoomTitle = view.findViewById<TextView>(R.id.tvRoomTitle)
        val tvEmptyBxh = view.findViewById<TextView>(R.id.tvEmptyBxh)
        val btnCloseBxh = view.findViewById<ImageView>(R.id.btnCloseBxh)
        val recyclerViewBxh = view.findViewById<RecyclerView>(R.id.recyclerViewBxh)

        tvRoomTitle.text = "Mã phòng thi: #$roomCode"
        recyclerViewBxh.layoutManager = LinearLayoutManager(requireContext())

        btnCloseBxh.setOnClickListener {
            dismiss()
        }

        val firebaseRepo = FirebaseRepository()
        viewLifecycleOwner.lifecycleScope.launch {
            val list = firebaseRepo.layBangXepHang(roomCode)
            if (list.isEmpty()) {
                tvEmptyBxh.visibility = View.VISIBLE
                recyclerViewBxh.visibility = View.GONE
            } else {
                tvEmptyBxh.visibility = View.GONE
                recyclerViewBxh.visibility = View.VISIBLE
                recyclerViewBxh.adapter = LeaderboardAdapter(list)
            }
        }
    }
}