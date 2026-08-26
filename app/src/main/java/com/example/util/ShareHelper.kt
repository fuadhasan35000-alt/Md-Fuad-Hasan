package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.CustomerEntity
import com.example.data.model.SaleEntity
import com.example.data.model.SaleItemEntity
import java.text.SimpleDateFormat
import java.util.*

object ShareHelper {

    fun formatInvoiceText(
        businessName: String,
        branchName: String,
        phone: String,
        sale: SaleEntity,
        items: List<SaleItemEntity>
    ): String {
        val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(sale.createdAt))
        val sb = StringBuilder()
        sb.append("🧾 *${businessName}*\n")
        sb.append("শাখা: ${branchName} | ফোন: ${phone}\n")
        sb.append("----------------------------\n")
        sb.append("চালান নং: ${sale.invoiceNumber}\n")
        sb.append("তারিখ: ${dateStr}\n")
        sb.append("ক্রেতা: ${sale.customerName} (${sale.customerPhone})\n")
        sb.append("----------------------------\n")
        sb.append("পণ্যসমূহ:\n")
        items.forEachIndexed { i, item ->
            sb.append("${i + 1}. ${item.productName}\n")
            sb.append("   ${item.quantity} ${item.unit} x ৳${item.unitPrice} = ৳${item.subtotal}\n")
        }
        sb.append("----------------------------\n")
        sb.append("মোট মূল্য: ৳${sale.subtotal}\n")
        if (sale.discount > 0) {
            sb.append("ছাড়: -৳${sale.discount}\n")
        }
        sb.append("সর্বমোট: ৳${sale.total}\n")
        sb.append("নগদ আদায়: ৳${sale.paid}\n")
        if (sale.due > 0) {
            sb.append("⚠️ বকেয়া: ৳${sale.due}\n")
        }
        sb.append("পরিশোধের মাধ্যম: ${sale.paymentMethod.getDisplayNameBn()}\n")
        sb.append("----------------------------\n")
        sb.append("ধন্যবাদ! আবার আসবেন। (আমার দোকান অ্যাপ)\n")
        return sb.toString()
    }

    fun formatDueReminderText(
        businessName: String,
        phone: String,
        customer: CustomerEntity
    ): String {
        return """
            শ্রদ্ধেয় ${customer.name},
            *${businessName}*-এ আপনার বর্তমান বকেয়ার পরিমাণ: *৳${customer.totalDue}*।
            
            বকেয়া পরিশোধের জন্য অনুরোধ করা যাচ্ছে।
            প্রয়োজনে যোগাযোগ করুন: ${phone}
            
            ধন্যবাদান্তে,
            ${businessName}
        """.trimIndent()
    }

    fun shareViaWhatsApp(context: Context, phoneNumber: String, message: String) {
        try {
            val cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "").let {
                if (it.startsWith("0")) "88$it" else if (!it.startsWith("88") && it.length == 10) "880$it" else it
            }
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            shareTextGeneral(context, message, "হোয়াটসঅ্যাপে পাঠান")
        }
    }

    fun sendSms(context: Context, phoneNumber: String, message: String) {
        try {
            val uri = Uri.parse("smsto:$phoneNumber")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            shareTextGeneral(context, message, "এসএমএস পাঠান")
        }
    }

    fun shareTextGeneral(context: Context, text: String, title: String = "শেয়ার করুন") {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "শেয়ার করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
        }
    }
}
