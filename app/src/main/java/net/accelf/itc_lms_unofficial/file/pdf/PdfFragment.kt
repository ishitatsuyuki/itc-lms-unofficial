package net.accelf.itc_lms_unofficial.file.pdf

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import com.github.polesapart.pdfviewer.PDFView
import com.shockwave.pdfium.PdfPasswordException
import dagger.hilt.android.AndroidEntryPoint
import net.accelf.itc_lms_unofficial.BaseFragment
import net.accelf.itc_lms_unofficial.Notifications
import net.accelf.itc_lms_unofficial.R
import net.accelf.itc_lms_unofficial.databinding.FragmentPdfBinding
import net.accelf.itc_lms_unofficial.file.download.ConfirmDownloadDialogFragment
import net.accelf.itc_lms_unofficial.file.download.DownloadDialogResult
import net.accelf.itc_lms_unofficial.file.pdf.PasswordDialogFragment.Companion.BUNDLE_PASSWORD
import net.accelf.itc_lms_unofficial.util.Success
import net.accelf.itc_lms_unofficial.util.notify
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@AndroidEntryPoint
class PdfFragment : BaseFragment<FragmentPdfBinding>(FragmentPdfBinding::class.java) {

    private val passwordDialog by lazy {
        PasswordDialogFragment.newInstance()
    }

    @Inject
    lateinit var notificationId: AtomicInteger

    private val viewModel by activityViewModels<PdfViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setFragmentResultListener(PasswordDialogFragment::class.java.simpleName) { _, bundle ->
            when (bundle.getInt(PasswordDialogFragment.BUNDLE_RESULT_CODE)) {
                PasswordDialogFragment.RESULT_SUCCESS -> {
                    passwordDialog.hide(parentFragmentManager)

                    binding.pdfView.fromBytes((viewModel.pdfFile.value as Success<ByteArray>).data)
                        .setDefaults()
                        .password(bundle.getString(BUNDLE_PASSWORD))
                        .onLoad { _, _, _ ->
                            passwordDialog.dismissDialog()
                        }
                        .load()
                }
                PasswordDialogFragment.RESULT_CANCEL -> {
                    activity?.finish()
                }
            }
        }

        viewModel.pdfFile.observe(viewLifecycleOwner) {
            when (it) {
                is Success -> {
                    binding.pdfView.apply {
                        fromBytes(it.data)
                            .setDefaults()
                            .load()
                    }
                }
            }
        }
    }

    private fun PDFView.Configurator.setDefaults(): PDFView.Configurator {
        return spacing(1)
            .enableAnnotationRendering(true)
            .defaultPage(viewModel.openingPage)
            .onLoad { _, _, _ ->
                viewModel.pdfTitle.postValue(binding.pdfView.documentMeta.title)
            }
            .onPageChange { page, _ ->
                viewModel.openingPage = page
            }
            .onError {
                if (it is PdfPasswordException) {
                    passwordDialog.display(parentFragmentManager)
                }
            }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        menu.findItem(R.id.actionDownload)?.isVisible = viewModel.pdfFile.value is Success
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.actionDownload -> {
                val dialog =
                    ConfirmDownloadDialogFragment.newInstance(viewModel.downloadable.file.fileName)
                setFragmentResultListener(ConfirmDownloadDialogFragment::class.java.simpleName) { _, it ->
                    @Suppress("UNCHECKED_CAST")
                    (it.getSerializable(ConfirmDownloadDialogFragment.BUNDLE_RESULT) as Result<DownloadDialogResult>).onSuccess {
                        val file = it.writeToFile(requireContext(),
                            MIME_PDF,
                            (viewModel.pdfFile.value as Success).data)

                        val id =
                            Notifications.Ids.DOWNLOAD_PROGRESS + notificationId.incrementAndGet()
                        val notification =
                            NotificationCompat.Builder(requireContext(),
                                Notifications.Channels.DOWNLOADS)
                                .apply {
                                    setSmallIcon(R.drawable.ic_download)
                                    setContentTitle(viewModel.downloadable.file.fileName)
                                    setContentText(getString(R.string.notify_text_downloaded))

                                    priority = NotificationCompat.PRIORITY_LOW
                                    setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                                    setAutoCancel(true)

                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        setDataAndType(file.uri, file.type)
                                    }
                                    val chooser = Intent.createChooser(intent, file.name)
                                    val pendingIntent =
                                        PendingIntent.getActivity(context, id, chooser, 0)
                                    setContentIntent(pendingIntent)
                                }.build()
                        requireContext().notify(id, notification)
                    }
                }
                dialog.show(parentFragmentManager,
                    ConfirmDownloadDialogFragment::class.java.simpleName)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val MIME_PDF = "application/pdf"

        @JvmStatic
        fun newInstance(): PdfFragment {
            return PdfFragment()
        }
    }
}
