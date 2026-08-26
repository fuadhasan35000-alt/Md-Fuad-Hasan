package com.example.util

import com.example.data.model.CustomerEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SaleEntity

data class BusinessInsight(
    val titleBn: String,
    val descriptionBn: String,
    val category: String, // "SALES", "STOCK", "DUE", "PROFIT"
    val isUrgent: Boolean = false
)

object AiAdvisorService {

    fun generateInsights(
        sales: List<SaleEntity>,
        products: List<ProductEntity>,
        customers: List<CustomerEntity>,
        expenses: List<ExpenseEntity>
    ): List<BusinessInsight> {
        val insights = mutableListOf<BusinessInsight>()

        // 1. Low stock alert
        val lowStockList = products.filter { it.currentStock <= it.minimumStock }
        if (lowStockList.isNotEmpty()) {
            val names = lowStockList.take(3).joinToString(", ") { it.name }
            insights.add(
                BusinessInsight(
                    titleBn = "স্টক সতর্কবার্তা (${lowStockList.size}টি পণ্য)",
                    descriptionBn = "$names সহ মোট ${lowStockList.size}টি পণ্যের স্টক শেষ হওয়ার পথে। অবিলম্বে নতুন অর্ডার করুন।",
                    category = "STOCK",
                    isUrgent = true
                )
            )
        }

        // 2. High Due Customer alert
        val dueCusts = customers.filter { it.totalDue > 1000 }.sortedByDescending { it.totalDue }
        val totalDue = customers.sumOf { it.totalDue }
        if (totalDue > 0) {
            val topDebtor = dueCusts.firstOrNull()
            val debtorInfo = if (topDebtor != null) "সর্বোচ্চ বকেয়া: ${topDebtor.name} (৳${topDebtor.totalDue})" else ""
            insights.add(
                BusinessInsight(
                    titleBn = "বকেয়া আদায় রিমাইন্ডার (মোট ৳$totalDue)",
                    descriptionBn = "দোকানের মোট বকেয়া ৳$totalDue। $debtorInfo। কাস্টমারদের হোয়াটসঅ্যাপে রিমাইন্ডার পাঠিয়ে দ্রুত আদায় করুন।",
                    category = "DUE",
                    isUrgent = totalDue > 10000
                )
            )
        }

        // 3. Sales performance
        val totalRevenue = sales.sumOf { it.total }
        if (sales.isNotEmpty()) {
            val avgTicket = totalRevenue / sales.size
            insights.add(
                BusinessInsight(
                    titleBn = "গড় বিক্রির পরিমাণ",
                    descriptionBn = "প্রতি চালানে আপনার গড় বিক্রি ৳${"%.1f".format(avgTicket)}। বিশেষ কম্বো অফার দিলে গড় বাস্কেট সাইজ ২০% বৃদ্ধি পেতে পারে।",
                    category = "SALES",
                    isUrgent = false
                )
            )
        }

        // 4. Expense Ratio
        val totalExpense = expenses.sumOf { it.amount }
        if (totalRevenue > 0 && totalExpense > 0) {
            val ratio = (totalExpense / totalRevenue) * 100
            val isHigh = ratio > 35
            insights.add(
                BusinessInsight(
                    titleBn = "খরচের অনুপাত: ${"%.1f".format(ratio)}%",
                    descriptionBn = if (isHigh) {
                        "মোট বিক্রির তুলনায় খরচের হার কিছুটা বেশি (${"%.1f".format(ratio)}%)। অপ্রয়োজনীয় ব্যয় পর্যবেক্ষণ করুন।"
                    } else {
                        "আপনার খরচের হার স্বাভাবিক ও নিয়ন্ত্রিত সীমার মধ্যে রয়েছে। ব্যবসার মার্জিন ভালো।"
                    },
                    category = "PROFIT",
                    isUrgent = isHigh
                )
            )
        }

        if (insights.isEmpty()) {
            insights.add(
                BusinessInsight(
                    titleBn = "দোকান প্রস্তুত",
                    descriptionBn = "আজকের বিক্রি ও স্টক তথ্য নিয়মিত এন্ট্রি করুন। এআই স্বয়ংক্রিয়ভাবে আপনাকে ব্যবসায়িক পরামর্শ দেবে।",
                    category = "SALES"
                )
            )
        }

        return insights
    }

    fun answerBusinessQuery(query: String, shopName: String): String {
        val q = query.lowercase()
        return when {
            q.contains("বিক্রি") || q.contains("সেল") || q.contains("বাড়াবো") -> {
                "📈 *$shopName-এর বিক্রি বৃদ্ধির ৫টি কার্যকর কৌশল:*\n\n" +
                        "১. দ্রুত বিক্রি হওয়া আইটেমগুলোর স্টক সবসময় পর্যাপ্ত রাখুন।\n" +
                        "২. চাল, তেল ও চিনির মতো দৈনন্দিন পণ্যে ছোট কম্বো অফার তৈরি করুন।\n" +
                        "৩. নিয়মিত কাস্টমারদের জন্য ৫% পর্যন্ত বিশেষ লয়্যালটি ডিসকাউন্ট দিন।\n" +
                        "৪. সন্ধ্যার পিক টাইমে দ্রুত বিলিং করার জন্য বারকোড/পিওএস ব্যবহার করুন।\n" +
                        "৫. বকেয়া কাস্টমারদের সাথে ভালো সম্পর্ক রেখে হোয়াটসঅ্যাপে বন্ধুত্বপূর্ণ রিমাইন্ডার দিন।"
            }
            q.contains("বকেয়া") || q.contains("দেনা") || q.contains("আদায়") -> {
                "💰 *বকেয়া দ্রুত ও সুন্দরভাবে আদায়ের উপায়:*\n\n" +
                        "১. প্রতি সপ্তাহের নির্দিষ্ট দিনে (যেমন শুক্রবার) অটোমেটিক হোয়াটসঅ্যাপ বা এসএমএস রিমাইন্ডার পাঠান।\n" +
                        "২. নতুন ক্রেতাদের ক্ষেত্রে বাকি দেওয়ার একটি নির্দিষ্ট লিমিট (যেমন সর্বোচ্চ ২,০০০ টাকা) নির্ধারণ করুন।\n" +
                        "৩. বাকি পরিশোধ করলে পরবর্তী ক্রয়ে সামান্য ছাড়ের প্রণোদনা দিন।\n" +
                        "৪. দেনা-পাওনা লেজারে প্রতিদিনের হিসাব স্বচ্ছ রাখুন।"
            }
            q.contains("স্টক") || q.contains("ইনভেন্টরি") || q.contains("ক্রয়") -> {
                "📦 *স্মার্ট ইনভেন্টরি ম্যানেজমেন্ট টিপস:*\n\n" +
                        "১. লো-স্টক নোটিফিকেশন পাওয়া মাত্রই ডিলারের কাছে রিকুইজিশন পাঠান।\n" +
                        "২. স্লো-মুভিং (যেগুলো কম বিক্রি হয়) পণ্যে টাকা আটকে না রেখে দ্রুত ছাড় দিয়ে ক্যাশ ফ্লো চালু রাখুন।\n" +
                        "৩. প্রতি মাসের শুরুতে ড্যামেজ বা মেয়াদের হিসাব যাচাই করতে 'স্টক সমন্বয়' ফিচারটি ব্যবহার করুন।"
            }
            q.contains("খরচ") || q.contains("লাভ") || q.contains("প্রফিট") -> {
                "📊 *দোকানের নিট মুনাফা বৃদ্ধির সূত্র:*\n\n" +
                        "১. দোকানের বিদ্যুৎ, পরিবহন ও আনুষঙ্গিক খরচ নিয়মিত 'খরচ' ট্যাবে এন্ট্রি করুন।\n" +
                        "২. ডিলারের কাছ থেকে নগদ ক্রয়ে অতিরিক্ত ক্যাশ ডিসকাউন্ট আদায় করুন।\n" +
                        "৩. উচ্চ মার্জিনের আইটেমগুলো (কসমেটিকস ও স্ন্যাক্স) কাউন্টারের সামনে রাখুন।"
            }
            else -> {
                "🤖 *আমার দোকান এআই বিজনেস অ্যাসিস্ট্যান্ট:*\n\n" +
                        "আপনার প্রশ্নটি পেয়েছি। $shopName-এর হিসাব-নিকাশ, পিওএস বিলিং, বাকি খাতা ও ব্রাঞ্চ রিপোর্ট সঠিকভাবে পরিচালিত হচ্ছে।\n" +
                        "আপনি চাইলে বিক্রি বৃদ্ধি, বকেয়া আদায়, স্টক প্ল্যানিং বা খরচ নিয়ন্ত্রণ সম্পর্কিত যেকোনো পরামর্শ জানতে পারেন।"
            }
        }
    }
}
