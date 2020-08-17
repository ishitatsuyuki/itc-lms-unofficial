package net.accelf.itc_lms_unofficial.network

import net.accelf.itc_lms_unofficial.models.CourseDetail
import net.accelf.itc_lms_unofficial.models.NotifyDetail
import net.accelf.itc_lms_unofficial.models.TimeTable
import net.accelf.itc_lms_unofficial.models.Updates
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class DocumentConverterFactory : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        val baseUri = retrofit.baseUrl().toString()
        return when (type) {
            CourseDetail::class.java -> CourseDetail.CourseDetailConverter(baseUri)
            NotifyDetail::class.java -> NotifyDetail.NotifyDetailConverter(baseUri)
            String::class.java -> StringConverter()
            TimeTable::class.java -> TimeTable.TimeTableConverter(baseUri)
            Updates::class.java -> Updates.UpdatesConverter(baseUri)
            else -> null
        }
    }

    abstract class DocumentConverter<T : Any>(private val baseUri: String) :
        Converter<ResponseBody, T?> {
        abstract override fun convert(value: ResponseBody): T?

        fun document(value: ResponseBody): Document {
            return Jsoup.parse(
                value.byteStream(),
                value.contentType()?.charset()?.name() ?: "UTF-8",
                baseUri,
                Parser.htmlParser()
            )
        }
    }

    class StringConverter : Converter<ResponseBody, String> {
        override fun convert(value: ResponseBody): String? {
            return value.string()
        }
    }
}
