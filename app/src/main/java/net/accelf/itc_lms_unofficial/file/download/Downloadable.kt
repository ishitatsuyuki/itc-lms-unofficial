package net.accelf.itc_lms_unofficial.file.download

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import com.google.gson.Gson
import io.reactivex.Single
import net.accelf.itc_lms_unofficial.file.download.ConfirmDownloadDialogFragment.Companion.BUNDLE_RESULT
import net.accelf.itc_lms_unofficial.file.pdf.PdfActivity
import net.accelf.itc_lms_unofficial.models.File
import net.accelf.itc_lms_unofficial.models.Material
import net.accelf.itc_lms_unofficial.network.LMS
import net.accelf.itc_lms_unofficial.permission.Permission
import net.accelf.itc_lms_unofficial.permission.RequestPermissionActivity
import net.accelf.itc_lms_unofficial.util.TIME_SECONDS_FORMAT
import net.accelf.itc_lms_unofficial.util.notify
import okhttp3.ResponseBody
import java.io.Serializable
import java.util.*

data class Downloadable(
    val type: Type,
    val file: File,
    val courseId: String,
    val materialParams: MaterialParams?,
) : Serializable {

    fun download(lms: LMS): Single<ResponseBody> {
        return when (type) {
            Type.MATERIAL -> lms.getFileId(courseId, materialParams!!.materialId,
                materialParams.resourceId, file.fileName, file.objectName)
                .flatMap {
                    lms.downloadMaterialFile(it,
                        courseId,
                        materialParams.materialId,
                        TIME_SECONDS_FORMAT.format(materialParams.endDate))
                }
            Type.REPORT -> lms.downloadReportFile(file.objectName, courseId)
        }
    }

    fun open(fragment: Fragment, gson: Gson) {
        val context = fragment.requireContext()
        if (file.fileName.endsWith(".pdf")) {
            context.startActivity(PdfActivity.intent(context, this))
            return
        }

        val permission = Permission.WRITE_EXTERNAL_STORAGE
        if (!permission.granted(context)) {
            permission.request(fragment.requireActivity() as AppCompatActivity) {
                if (permission.granted(context)) {
                    downloadWithDialog(fragment, gson)
                    return@request
                }

                val (id, notification) = RequestPermissionActivity.permissionRequiredNotification(
                    context,
                    permission)
                context.notify(id, notification)
            }
            return
        }

        downloadWithDialog(fragment, gson)
    }

    private fun downloadWithDialog(fragment: Fragment, gson: Gson) {
        val confirmDownloadDialog = ConfirmDownloadDialogFragment.newInstance(file.fileName)
        fragment.setFragmentResultListener(ConfirmDownloadDialogFragment::class.java.simpleName) { _, it ->
            @Suppress("UNCHECKED_CAST")
            (it.getSerializable(BUNDLE_RESULT) as Result<DownloadDialogResult>).onSuccess {
                FileDownloadWorker.enqueue(fragment.requireContext(), gson, this, it)
            }
        }
        confirmDownloadDialog.show(fragment.parentFragmentManager,
            ConfirmDownloadDialogFragment::class.java.simpleName)
    }

    companion object {

        fun materialFile(courseId: String, material: Material): Downloadable {
            return Downloadable(
                Type.MATERIAL,
                material.file!!,
                courseId,
                MaterialParams(
                    material.materialId,
                    material.resourceId,
                    material.until!!,
                ),
            )
        }

        fun reportFile(courseId: String, file: File): Downloadable {
            return Downloadable(
                Type.REPORT,
                file,
                courseId,
                null,
            )
        }
    }

    enum class Type {
        MATERIAL,
        REPORT,
    }

    data class MaterialParams(
        val materialId: String,
        val resourceId: String,
        val endDate: Date,
    ) : Serializable
}