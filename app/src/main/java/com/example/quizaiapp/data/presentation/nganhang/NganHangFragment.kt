package com.example.quizaiapp.data.presentation.nganhang

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quizaiapp.R
import com.example.quizaiapp.data.local.KetNoiCoSoDuLieu
import com.example.quizaiapp.data.presentation.quizz.QuizActivity
import com.example.quizaiapp.data.service.PdfGeneratorService
import com.example.quizaiapp.data.service.WordParserService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NganHangFragment : Fragment() {

    private enum class SelectedTab { HISTORY, BANK }
    private var currentTab = SelectedTab.HISTORY

    private var historyAdapter: LichSuDeThiAdapter? = null
    private var bankAdapter: CauHoiNganHangAdapter? = null

    private var recyclerView: RecyclerView? = null
    private var tvEmptyState: TextView? = null
    private var btnTabHistory: Button? = null
    private var btnTabBank: Button? = null

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            importFile(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_ngan_hang, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val btnImportFile = view.findViewById<Button>(R.id.btnImportFile)
        btnTabHistory = view.findViewById(R.id.btnTabHistory)
        btnTabBank = view.findViewById(R.id.btnTabBank)
        recyclerView = view.findViewById(R.id.recyclerViewNganHang)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)

        btnBack.visibility = View.GONE

        recyclerView?.layoutManager = LinearLayoutManager(requireContext())

        btnTabHistory?.setOnClickListener {
            switchTab(SelectedTab.HISTORY)
        }

        btnTabBank?.setOnClickListener {
            switchTab(SelectedTab.BANK)
        }

        btnImportFile.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        switchTab(SelectedTab.HISTORY)
    }

    private fun switchTab(tab: SelectedTab) {
        currentTab = tab
        if (tab == SelectedTab.HISTORY) {
            btnTabHistory?.setBackgroundColor(Color.parseColor("#0F172A"))
            btnTabHistory?.setTextColor(Color.WHITE)
            btnTabBank?.setBackgroundColor(Color.TRANSPARENT)
            btnTabBank?.setTextColor(Color.parseColor("#64748B"))
            loadHistoryData()
        } else {
            btnTabBank?.setBackgroundColor(Color.parseColor("#0F172A"))
            btnTabBank?.setTextColor(Color.WHITE)
            btnTabHistory?.setBackgroundColor(Color.TRANSPARENT)
            btnTabHistory?.setTextColor(Color.parseColor("#64748B"))
            loadBankData()
        }
    }

    private fun loadHistoryData() {
        val db = KetNoiCoSoDuLieu.layInstance(requireContext().applicationContext)
        viewLifecycleOwner.lifecycleScope.launch {
            db.truyVanDeThiDao().layToanBoLichSuDeThi().collectLatest { historyList ->
                if (historyList.isEmpty()) {
                    tvEmptyState?.visibility = View.VISIBLE
                    tvEmptyState?.text = "Chưa có lịch sử đề thi nào. Hãy tạo đề thi bằng AI!"
                    recyclerView?.visibility = View.GONE
                } else {
                    tvEmptyState?.visibility = View.GONE
                    recyclerView?.visibility = View.VISIBLE

                    historyAdapter = LichSuDeThiAdapter(
                        listDeThi = historyList,
                        onLamThuClick = { deThi ->
                            val intent = Intent(requireContext(), QuizActivity::class.java).apply {
                                putExtra("EXTRA_ID_DE_THI", deThi.idDeThi)
                            }
                            startActivity(intent)
                        },
                        onXuatFileClick = { deThi ->
                            viewLifecycleOwner.lifecycleScope.launch {
                                Toast.makeText(requireContext(), "Đang xuất file PDF...", Toast.LENGTH_SHORT).show()
                                val questions = db.truyVanDeThiDao().layDanhSachCauHoiTheoDeThi(deThi.idDeThi)
                                val pdfService = PdfGeneratorService(requireContext().applicationContext)
                                val res = pdfService.xuatFilePdfDeThiVaDapAn(deThi, questions)
                                if (res.isSuccess) {
                                    Toast.makeText(requireContext(), "Đã xuất PDF vào thư mục Downloads!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(requireContext(), "Lỗi khi xuất PDF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                    recyclerView?.adapter = historyAdapter
                }
            }
        }
    }

    private fun loadBankData() {
        val db = KetNoiCoSoDuLieu.layInstance(requireContext().applicationContext)
        viewLifecycleOwner.lifecycleScope.launch {
            val questions = db.nganHangDao().layToanBoCauHoi()
            if (questions.isEmpty()) {
                tvEmptyState?.visibility = View.VISIBLE
                tvEmptyState?.text = "Ngân hàng chưa có câu hỏi nào. Bấm 'Nhập File' để thêm!"
                recyclerView?.visibility = View.GONE
            } else {
                tvEmptyState?.visibility = View.GONE
                recyclerView?.visibility = View.VISIBLE

                bankAdapter = CauHoiNganHangAdapter(
                    listCauHoi = questions,
                    onDeleteClick = { cauHoi ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            db.nganHangDao().xoaCauHoi(cauHoi.idCauHoi)
                            Toast.makeText(requireContext(), "Đã xóa câu hỏi!", Toast.LENGTH_SHORT).show()
                            loadBankData()
                        }
                    }
                )
                recyclerView?.adapter = bankAdapter
            }
        }
    }

    private fun importFile(uri: Uri) {
        val db = KetNoiCoSoDuLieu.layInstance(requireContext().applicationContext)
        val parserService = WordParserService(requireContext().applicationContext, db.nganHangDao())

        Toast.makeText(requireContext(), "Đang đọc và nhập dữ liệu từ file...", Toast.LENGTH_SHORT).show()

        viewLifecycleOwner.lifecycleScope.launch {
            val result = parserService.parseAndImportFile(uri, "Tài liệu đính kèm")
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                Toast.makeText(requireContext(), "Đã nhập thành công $count câu hỏi!", Toast.LENGTH_LONG).show()
                if (currentTab == SelectedTab.BANK) {
                    loadBankData()
                } else {
                    switchTab(SelectedTab.BANK)
                }
            } else {
                Toast.makeText(requireContext(), "Lỗi khi đọc file: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}