package com.example.first_try

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class UploadBottomSheetFragment : BottomSheetDialogFragment() {

    interface UploadBottomSheetListener {
        fun onUploadFile(fileType: String)
    }

    private var listener: UploadBottomSheetListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_upload, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnPdf: Button = view.findViewById(R.id.btn_pdf)
        val btnYoutube: Button = view.findViewById(R.id.btn_youtube)
        val btnWebsite: Button = view.findViewById(R.id.btn_website)
        val btnAudio: Button = view.findViewById(R.id.btn_audio)
        val btnImage: Button = view.findViewById(R.id.btn_image)

        btnPdf.setOnClickListener { listener?.onUploadFile("pdf") }
        btnYoutube.setOnClickListener { listener?.onUploadFile("youtube") }
        btnWebsite.setOnClickListener { listener?.onUploadFile("website") }
        btnAudio.setOnClickListener { listener?.onUploadFile("audio") }
        btnImage.setOnClickListener { listener?.onUploadFile("image") }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? UploadBottomSheetListener
            ?: throw ClassCastException("$context must implement UploadBottomSheetListener")
    }
}
