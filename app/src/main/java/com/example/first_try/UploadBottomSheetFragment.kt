package com.example.first_try

import android.content.Context
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class UploadBottomSheetFragment : BottomSheetDialogFragment() {

    interface UploadBottomSheetListener {
        fun onUploadPdf()
        fun onUploadYoutube()
        fun onUploadWebsite()
        fun onUploadAudio()
        fun onUploadImage()
    }

    private var listener: UploadBottomSheetListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_upload_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnPdf: Button = view.findViewById(R.id.upload_pdf_button)
        val btnYoutube: Button = view.findViewById(R.id.upload_youtube_button)
        val btnWebsite: Button = view.findViewById(R.id.upload_website_button)
        val btnAudio: Button = view.findViewById(R.id.upload_audio_button)
        val btnImage: Button = view.findViewById(R.id.upload_image_button)

        btnPdf.setOnClickListener { listener?.onUploadPdf() }
        btnYoutube.setOnClickListener { listener?.onUploadYoutube() }
        btnWebsite.setOnClickListener { listener?.onUploadWebsite() }
        btnAudio.setOnClickListener { listener?.onUploadAudio() }
        btnImage.setOnClickListener { listener?.onUploadImage() }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? UploadBottomSheetListener
            ?: throw ClassCastException("$context must implement UploadBottomSheetListener")
    }
}
