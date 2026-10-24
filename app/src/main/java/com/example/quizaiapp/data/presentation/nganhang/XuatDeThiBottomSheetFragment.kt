package com.example.quizaiapp.data.presentation.nganhang

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.quizaiapp.R
import com.example.quizaiapp.data.local.KetNoiCoSoDuLieu
import com.example.quizaiapp.data.service.PdfGeneratorService
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class XuatDeThiBottomSheetFragment : BottomSheetDialogFragment() {

    private var idDeThi: String = ""

    companion object {
        private const val ARG_ID_DE_THI = "arg_id_de_thi"

        fun newInstance(idDeThi: String): XuatDeThiBottomSheetFragment {
            val fragment = XuatDeThiBottomSheetFragment()
            val args = Bundle().apply {
                putString(ARG_ID_DE_THI, idDeThi)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        idDeThi = arguments?.getString(ARG_ID_DE_THI) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_xuat_de_thi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnCloseDialog = view.findViewById<ImageView>(R.id.btnCloseDialog)
        val btnDownloadAll = view.findViewById<Button>(R.id.btnDownloadAll)

        btnCloseDialog.setOnClickListener {
            dismiss()
        }

        btnDownloadAll.setOnClickListener {
            val db = KetNoiCoSoDuLieu.layInstance(requireContext().applicationContext)
            viewLifecycleOwner.lifecycleScope.launch {
                Toast.makeText(requireContext(), "Đang chuẩn bị bộ tài liệu in ấn...", Toast.LENGTH_SHORT).show()
                val deThi = db.truyVanDeThiDao().layDeThiTheoId(idDeThi)
                if (deThi != null) {
                    val questions = db.truyVanDeThiDao().layDanhSachCauHoiTheoDeThi(idDeThi)
                    val pdfService = PdfGeneratorService(requireContext().applicationContext)
                    val result = pdfService.xuatFilePdfDeThiVaDapAn(deThi, questions)

                    if (result.isSuccess) {
                        Toast.makeText(
                            requireContext(),
                            "Đã xuất trọn bộ file PDF & Đáp án vào thư mục Downloads!",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Lỗi khi xuất file: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Đã tải xuống gói tài liệu mẫu!", Toast.LENGTH_SHORT).show()
                }
                dismiss()
            }
        }
    }
}