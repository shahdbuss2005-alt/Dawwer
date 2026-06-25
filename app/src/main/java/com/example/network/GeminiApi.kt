package com.example.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.CreativeReuse
import com.example.data.GeminiScanResult
import com.example.data.RecyclingMethod
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// --- Gemini API Moshi Models ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<ContentPayload>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfigPayload? = null,
    @Json(name = "systemInstruction") val systemInstruction: ContentPayload? = null
)

@JsonClass(generateAdapter = true)
data class ContentPayload(
    @Json(name = "parts") val parts: List<PartPayload>
)

@JsonClass(generateAdapter = true)
data class PartPayload(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineDataPayload? = null
)

@JsonClass(generateAdapter = true)
data class InlineDataPayload(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfigPayload(
    @Json(name = "responseMimeType") val responseMimeType: String? = "application/json",
    @Json(name = "temperature") val temperature: Double? = 0.2
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<CandidatePayload>?
)

@JsonClass(generateAdapter = true)
data class CandidatePayload(
    @Json(name = "content") val content: ContentPayload?
)

// --- Retrofit Service Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

class GeminiService {

    private val systemPrompt = """
أنت خبير في إدارة المخلفات وإعادة التدوير في مصر.
عند استقبال صورة مخلف أو وصفه، قدم تحليلاً شاملاً بصيغة JSON تماماً كما هو موضح بالأسفل.
تأكد من إرجاع نص JSON صالح فقط بدون أي علامات ماركداون وبدون أي كلام خارجي.

شكل الـ JSON المطلوب بدقة:
{
  "item_name": "اسم المخلف بالعربي",
  "item_emoji": "إيموجي مناسب",
  "material_type": "نوع المادة (بلاستيك/ورق/زجاج/معدن/عضوي/إلكتروني)",
  "recyclable": true/false,
  "recyclability_score": رقم من 1 إلى 10,
  "environmental_impact": "وصف قصير للتأثير البيئي لو اترمى",
  "co2_saved_grams": الكمية المقدرة من CO2 بتتوفر لو اتدور بالجرام (رقم عادي),
  "decomposition_years": "عدد السنين اللازمة للتحلل (مثال: '٤٥٠ سنة' أو 'غير قابلة للتحلل')",
  "recycling_methods": [
    {
      "method": "اسم طريقة التدوير",
      "difficulty": "سهل/متوسط/صعب",
      "steps": ["خطوة ١", "خطوة ٢", "خطوة ٣"],
      "result": "ما الذي سينتج منها",
      "wow_fact": "حقيقة مدهشة عن طريقة التدوير هذه"
    }
  ],
  "creative_reuse": [
    {
      "idea": "فكرة إعادة استخدام إبداعية وصديقة للبيئة",
      "difficulty": "سهل/متوسط",
      "materials_needed": ["المواد الإضافية المطلوبة"],
      "time_minutes": الوقت المطلوب بالدقائق (رقم عادي),
      "steps": ["خطوة صنع يدوية تفصيلية أولى", "خطوة ثانية", "خطوة ثالثة"],
      "benefit": "كيف يستفيد المستخدم من هذه الصناعة اليدوية بدقة (توفير مال/ديكور/بيع)"
    }
  ],
  "fun_fact": "حقيقة مدهشة عن المخلف أو المادة الخاصة به",
  "points_earned": نقاط تُمنح للمستخدم (رقم من 10 إلى 100 حسب صعوبة التدوير)
}

كن دقيقاً، وعملياً، ومحفزاً، وركز على البيئة المصرية وثقافة الاستدامة العربية.
"""

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun analyzeWaste(bitmap: Bitmap?, textPrompt: String? = null, lang: String = "ar"): GeminiScanResult? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("GeminiService", "API Key is empty or placeholder!")
            return@withContext getFallbackResult(textPrompt ?: "مخلف بلاستيكي", lang)
        }

        val parts = mutableListOf<PartPayload>()
        val basePrompt = textPrompt ?: "حلل هذا المخلف المصور لمساعدتي في إعادة تدويره."
        val languageInstruction = if (lang == "ar") {
            "\nملاحظة هامة جداً: يجب أن تكون جميع نصوص الكائن JSON باللغة العربية الفصحى حصراً."
        } else {
            "\nCRITICAL: You MUST translate all text values in the JSON structure (such as item_name, material_type, environmental_impact, method, difficulty, steps, result, wow_fact, idea, fun_fact, decomposition_years) to English. Every text field in the returned JSON must be in English language."
        }
        parts.add(PartPayload(text = basePrompt + languageInstruction))

        if (bitmap != null) {
            val base64Data = bitmap.toBase64()
            parts.add(PartPayload(inlineData = InlineDataPayload(mimeType = "image/jpeg", data = base64Data)))
        }

        val request = GenerateContentRequest(
            contents = listOf(ContentPayload(parts = parts)),
            generationConfig = GenerationConfigPayload(),
            systemInstruction = ContentPayload(parts = listOf(PartPayload(text = systemPrompt)))
        )

        try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (rawText != null) {
                return@withContext parseGeminiJson(rawText)
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Gemini API Call failed: ${e.message}", e)
        }
        return@withContext getFallbackResult(textPrompt ?: "مخلف بلاستيكي", lang)
    }

    suspend fun getAlternativeCreativeIdea(itemName: String, existingIdeas: List<String>, lang: String = "ar"): CreativeReuse? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getFallbackAlternativeIdea(itemName, lang)
        }

        val prompt = """
            أريد فكرة إعادة استخدام إبداعية وصديقة للبيئة بديلة وغير مكررة للمخلف التالي: "$itemName".
            الأفكار السابقة التي يجب تجنبها تماماً وعدم تكرارها هي: ${existingIdeas.joinToString(", ")}.
            
            يرجى تقديم الفكرة الجديدة بصيغة JSON تماماً كما هو موضح بالأسفل. 
            تأكد من إرجاع نص JSON صالح فقط بدون أي علامات ماركداون وبدون أي كلام خارجي.
            
            شكل الـ JSON المطلوب بدقة:
            {
              "idea": "اسم فكرة إعادة الاستخدام الإبداعية البديلة",
              "difficulty": "سهل/متوسط",
              "materials_needed": ["المواد الإضافية المطلوبة"],
              "time_minutes": الوقت المطلوب بالدقائق كأرقام فقط,
              "steps": ["خطوة صنع يدوية تفصيلية أولى", "خطوة ثانية", "خطوة ثالثة"],
              "benefit": "كيف يستفيد المستخدم من هذه الصناعة اليدوية (مثال: توفير مال/ديكور/بيع)"
            }
        """.trimIndent()

        val languageInstruction = if (lang == "ar") {
            "\nملاحظة هامة جداً: يجب أن تكون جميع نصوص الكائن JSON باللغة العربية الفصحى حصراً."
        } else {
            "\nCRITICAL: You MUST translate all values in the JSON structure (such as idea, difficulty, materials_needed, steps, benefit) to English. Every text field in the returned JSON must be in English language."
        }

        val request = GenerateContentRequest(
            contents = listOf(ContentPayload(parts = listOf(PartPayload(text = prompt + languageInstruction)))),
            generationConfig = GenerationConfigPayload(),
            systemInstruction = ContentPayload(parts = listOf(PartPayload(text = "أنت خبير إبداعي في إعادة تدوير المخلفات المنزلية وترشيد الاستهلاك.")))
        )

        try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (rawText != null) {
                return@withContext parseCreativeReuseJson(rawText)
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Alternative idea generation failed: ${e.message}", e)
        }
        return@withContext getFallbackAlternativeIdea(itemName, lang)
    }

    private fun parseCreativeReuseJson(rawText: String): CreativeReuse? {
        try {
            var cleanJson = rawText.trim()
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substringAfter("```json").substringBeforeLast("```")
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substringAfter("```").substringBeforeLast("```")
            }
            cleanJson = cleanJson.trim()

            val json = JSONObject(cleanJson)
            val idea = json.optString("idea", "فكرة إبداعية أخرى")
            val difficulty = json.optString("difficulty", "سهل")
            val timeMinutes = json.optInt("time_minutes", 15)
            val benefit = json.optString("benefit", "")

            val materialsArr = json.optJSONArray("materials_needed")
            val materialsList = mutableListOf<String>()
            if (materialsArr != null) {
                for (j in 0 until materialsArr.length()) {
                    materialsList.add(materialsArr.getString(j))
                }
            }

            val stepsArr = json.optJSONArray("steps")
            val stepsList = mutableListOf<String>()
            if (stepsArr != null) {
                for (j in 0 until stepsArr.length()) {
                    stepsList.add(stepsArr.getString(j))
                }
            }

            return CreativeReuse(
                idea = idea,
                difficulty = difficulty,
                materialsNeeded = materialsList,
                timeMinutes = timeMinutes,
                steps = stepsList,
                benefit = benefit
            )
        } catch (e: Exception) {
            Log.e("GeminiService", "Alternative CreativeReuse JSON parsing failed for: $rawText", e)
        }
        return null
    }

    private fun getFallbackAlternativeIdea(itemName: String, lang: String): CreativeReuse {
        val isAr = lang == "ar"
        return if (isAr) {
            CreativeReuse(
                idea = "مقلمة مكتبية أنيقة ومنظم أدوات",
                difficulty = "سهل",
                materialsNeeded = listOf("مشرط أو مقص", "شريط زينة ملون", "لاصق قوي"),
                timeMinutes = 10,
                steps = listOf(
                    "قص الجزء العلوي للمخلف بعناية فائقة لتفادي حواف حادة.",
                    "برد وتنعيم الحافة باستخدام مبرد أو شريط لاصق زينة ملون لحماية اليدين.",
                    "تزيين الهيكل الخارجي برسومات أو ملصقات حسب الرغبة.",
                    "استخدامها على مكتبك لترتيب وحفظ الأقلام والمساطر والأدوات الفنية."
                ),
                benefit = "تنظيم وترتيب سطح المكتب مجاناً، وإعادة تدوير المخلف في شكل ديكور عملي جذاب."
            )
        } else {
            CreativeReuse(
                idea = "Stylish Desk Organizer & Pencil Cup",
                difficulty = "Easy",
                materialsNeeded = listOf("Craft knife or scissors", "Colorful decorative tape", "Strong glue"),
                timeMinutes = 10,
                steps = listOf(
                    "Cut off the top section of the item carefully to avoid sharp edges.",
                    "Smooth out the rim using sandpaper or wrap with decorative tape to protect your hands.",
                    "Decorate the exterior with stickers or paint as desired.",
                    "Place it on your desk to organize pens, markers, and craft supplies neatly."
                ),
                benefit = "De-clutters your desk for zero cost, transforming waste into a highly practical and neat holder."
            )
        }
    }

    suspend fun generateText(prompt: String, systemInstruction: String = ""): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext ""
        }
        val request = GenerateContentRequest(
            contents = listOf(ContentPayload(parts = listOf(PartPayload(text = prompt)))),
            generationConfig = GenerationConfigPayload(responseMimeType = "text/plain"),
            systemInstruction = if (systemInstruction.isNotEmpty()) ContentPayload(parts = listOf(PartPayload(text = systemInstruction))) else null
        )
        return@withContext try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        } catch (e: Exception) {
            Log.e("GeminiService", "generateText failed: ${e.message}", e)
            ""
        }
    }

    suspend fun getVoiceAssistantAdvice(userQuery: String, lang: String = "ar"): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val isAr = lang == "ar"
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext if (isAr) {
                "أهلاً بك! لإعادة تدوير الزجاج الفارغ، يرجى غسله جيداً وإزالة الأغطية المعدنية، ثم إيداعه في أقرب حاوية للزجاج لحماية البيئة وتوفير الطاقة! 🌿"
            } else {
                "Welcome! To recycle empty glass, please rinse it thoroughly, remove metal caps, and deposit it in the nearest glass bin to save energy! 🌿"
            }
        }

        val prompt = if (isAr) {
            """
            أنت "مساعد دَوِّر الصوتي الذكي"، خبير بيئي لطيف ومحفز.
            المستخدم يطرح سؤالاً صوتياً: "$userQuery"
            أجب بلغة عربية فصحى مبسطة، ودودة للغاية ومحفزة.
            اجعل الإجابة موجزة جداً ومناسبة للقرص الصوتي والتحويل التلقائي لنصوص مسموعة (بين ٢ إلى ٣ أسطر كحد أقصى).
            قدم نصيحة تدوير سريعة وعملية.
            """.trimIndent()
        } else {
            """
            You are "Dawwer Voice Assistant", a friendly and motivating eco-expert.
            The user asks: "$userQuery"
            Answer in simple, friendly, and motivating English.
            Keep it very concise and suitable for text-to-speech output (max 2-3 short sentences).
            Provide quick, practical recycling tips.
            """.trimIndent()
        }

        val request = GenerateContentRequest(
            contents = listOf(ContentPayload(parts = listOf(PartPayload(text = prompt)))),
            generationConfig = GenerationConfigPayload(responseMimeType = "text/plain"),
            systemInstruction = ContentPayload(parts = listOf(PartPayload(text = if (isAr) "أنت مساعد التدوير الصوتي الذكي والودود." else "You are the smart, friendly recycling voice assistant.")))
        )

        try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: (if (isAr) "عذراً، لم أسمعك جيداً. هل يمكنك إعادة السؤال؟" else "Sorry, I didn't catch that. Could you repeat please?")
        } catch (e: Exception) {
            return@withContext if (isAr) {
                "إليك هذه النصيحة السريعة: حافظ دائماً على المواد نظيفة وجافة قبل فرزها لضمان كفاءة إعادة تدويرها بنسبة ١٠٠٪! 🌱"
            } else {
                "Here is a quick tip: Always keep materials clean and dry before sorting to ensure 100% recycling efficiency! 🌱"
            }
        }
    }

    private fun parseGeminiJson(rawText: String): GeminiScanResult? {
        try {
            // Clean markdown wrappers if present
            var cleanJson = rawText.trim()
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substringAfter("```json").substringBeforeLast("```")
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substringAfter("```").substringBeforeLast("```")
            }
            cleanJson = cleanJson.trim()

            val json = JSONObject(cleanJson)
            val itemName = json.optString("item_name", "مخلف مستكشف")
            val itemEmoji = json.optString("item_emoji", "📦")
            val materialType = json.optString("material_type", "بلاستيك")
            val recyclable = json.optBoolean("recyclable", true)
            val recyclabilityScore = json.optInt("recyclability_score", 8)
            val environmentalImpact = json.optString("environmental_impact", "تراكم النفايات يضر بالبيئة والحياة الفطرية.")
            val co2SavedGrams = json.optDouble("co2_saved_grams", 35.0)
            val waterSavedLiters = json.optDouble("water_saved_liters", 3.0)
            val energySavedKwh = json.optDouble("energy_saved_kwh", 0.2)
            val decompositionYears = json.optString("decomposition_years", "٤٥٠ سنة")
            val funFact = json.optString("fun_fact", "إعادة تدوير علبة ألومنيوم واحدة توفر طاقة كافية لتشغيل التلفزيون لمدة ٣ ساعات!")
            val pointsEarned = json.optInt("points_earned", 50)

            // Parse recycling methods
            val methodsList = mutableListOf<RecyclingMethod>()
            val methodsArr = json.optJSONArray("recycling_methods")
            if (methodsArr != null) {
                for (i in 0 until methodsArr.length()) {
                    val mObj = methodsArr.getJSONObject(i)
                    val stepsArr = mObj.optJSONArray("steps")
                    val stepsList = mutableListOf<String>()
                    if (stepsArr != null) {
                        for (j in 0 until stepsArr.length()) {
                            stepsList.add(stepsArr.getString(j))
                        }
                    }
                    methodsList.add(
                        RecyclingMethod(
                            method = mObj.optString("method", "فرز منزلي"),
                            difficulty = mObj.optString("difficulty", "سهل"),
                            steps = stepsList,
                            result = mObj.optString("result", "مادة خام"),
                            wowFact = mObj.optString("wow_fact", "توفير ٩٥٪ من الطاقة")
                        )
                    )
                }
            }

            // Parse creative reuse
            val reuseList = mutableListOf<CreativeReuse>()
            val reuseArr = json.optJSONArray("creative_reuse")
            if (reuseArr != null) {
                for (i in 0 until reuseArr.length()) {
                    val rObj = reuseArr.getJSONObject(i)
                    val materialsArr = rObj.optJSONArray("materials_needed")
                    val materialsList = mutableListOf<String>()
                    if (materialsArr != null) {
                        for (j in 0 until materialsArr.length()) {
                            materialsList.add(materialsArr.getString(j))
                        }
                    }
                    val stepsArr = rObj.optJSONArray("steps")
                    val stepsList = mutableListOf<String>()
                    if (stepsArr != null) {
                        for (j in 0 until stepsArr.length()) {
                            stepsList.add(stepsArr.getString(j))
                        }
                    }
                    val benefit = rObj.optString("benefit", "")
                    reuseList.add(
                        CreativeReuse(
                            idea = rObj.optString("idea", "أصيص نباتات منزلي"),
                            difficulty = rObj.optString("difficulty", "سهل"),
                            materialsNeeded = materialsList,
                            timeMinutes = rObj.optInt("time_minutes", 15),
                            steps = stepsList,
                            benefit = benefit
                        )
                    )
                }
            }

            return GeminiScanResult(
                itemName = itemName,
                itemEmoji = itemEmoji,
                materialType = materialType,
                recyclable = recyclable,
                recyclabilityScore = recyclabilityScore,
                environmentalImpact = environmentalImpact,
                co2SavedGrams = co2SavedGrams,
                waterSavedLiters = waterSavedLiters,
                energySavedKwh = energySavedKwh,
                decompositionYears = decompositionYears,
                recyclingMethods = methodsList,
                creativeReuse = reuseList,
                funFact = funFact,
                pointsEarned = pointsEarned
            )
        } catch (e: Exception) {
            Log.e("GeminiService", "JSON parsing failed for: $rawText", e)
        }
        return null
    }

    private fun getFallbackResult(query: String, lang: String = "ar"): GeminiScanResult {
        // Fallbacks for offline or missing API key
        val cleanQuery = query.lowercase()
        val isAr = lang == "ar"
        return when {
            cleanQuery.contains("بلاستيك") || cleanQuery.contains("زجاجة") || cleanQuery.contains("plastic") || cleanQuery.contains("bottle") -> {
                if (isAr) {
                    GeminiScanResult(
                        itemName = "زجاجة مياه بلاستيكية PET",
                        itemEmoji = "🧴",
                        materialType = "بلاستيك",
                        recyclable = true,
                        recyclabilityScore = 9,
                        environmentalImpact = "تأخذ حوالي ٤٥٠ سنة لتتحلل بالكامل وتتحول إلى جزيئات بلاستيكية ضارة بالتربة والمياه والمخلوقات البحرية.",
                        co2SavedGrams = 30.0,
                        waterSavedLiters = 2.0,
                        energySavedKwh = 0.1,
                        decompositionYears = "٤٥٠ سنة",
                        recyclingMethods = listOf(
                            RecyclingMethod(
                                method = "الفرز والكبس المنزلي",
                                difficulty = "سهل",
                                steps = listOf("غسل الزجاجة جيداً للتخلص من أي سوائل متبقية", "نزع الغطاء والحلقة البلاستيكية وفرزهما بشكل مستقل", "ضغط الزجاجة بالقدم أو اليد لتقليل مساحتها بنسبة ٨٠٪"),
                                result = "كتل بلاستيكية مضغوطة جاهزة للشحن لمصانع إعادة تدوير حبيبات الـ PET الفاخرة",
                                wowFact = "فرز البلاستيك PET يوفر حوالي ٨٤٪ من الطاقة المطلوبة لتصنيع بلاستيك خام جديد!"
                            )
                        ),
                        creativeReuse = listOf(
                            CreativeReuse(
                                idea = "أصيص لزراعة النعناع المنزلي بالتعليق الذاتي",
                                difficulty = "سهل",
                                materialsNeeded = listOf("مقص قوي", "حبل متين", "تربة طمي خفيفة", "بذور أو شتلة نعناع"),
                                timeMinutes = 15,
                                steps = listOf(
                                    "قص النصف العلوي من الزجاجة بعناية باستخدام مقص قوي.",
                                    "قم بعمل ثقوب صغيرة في قاع النصف السفلي لتصريف المياه الزائدة.",
                                    "اقلب النصف العلوي (فوهة لأسفل) وضعه داخل النصف السفلي ليعمل كمرشح.",
                                    "املأه بالطمي الخفيف ثم اغرس بذور أو شتلة النعناع واروها باعتدال."
                                ),
                                benefit = "توفير شراء أوعية زرع منزلية، تزيين المطبخ والشرفات بنباتات عطرية طازجة ومجانية!"
                            )
                        ),
                        funFact = "إعادة تدوير طن واحد من البلاستيك يوفر مساحة دفن هائلة ويعادل توفير كمية هائلة من البترول المستورد!",
                        pointsEarned = 50,
                        badgeUnlocked = "plastic_hero"
                    )
                } else {
                    GeminiScanResult(
                        itemName = "Empty PET Plastic Water Bottle",
                        itemEmoji = "🧴",
                        materialType = "Plastic",
                        recyclable = true,
                        recyclabilityScore = 9,
                        environmentalImpact = "Takes about 450 years to decompose completely and breaks into harmful microplastics polluting soil, oceans, and wildlife.",
                        co2SavedGrams = 30.0,
                        waterSavedLiters = 2.0,
                        energySavedKwh = 0.1,
                        decompositionYears = "450 Years",
                        recyclingMethods = listOf(
                            RecyclingMethod(
                                method = "Home Sorting & Crushing",
                                difficulty = "Easy",
                                steps = listOf("Rinse the bottle thoroughly to clean any residue", "Remove the cap and label ring to sort them separately", "Squeeze or crush the bottle to reduce its volume by 80%"),
                                result = "Crushed plastic blocks ready for shipment to premium PET recycling factories",
                                wowFact = "Sorting PET plastic saves about 84% of the energy required to manufacture virgin plastic!"
                            )
                        ),
                        creativeReuse = listOf(
                            CreativeReuse(
                                idea = "Self-Watering Mint Planter",
                                difficulty = "Easy",
                                materialsNeeded = listOf("Strong scissors", "Durable string", "Light potting soil", "Mint seeds"),
                                timeMinutes = 15,
                                steps = listOf(
                                    "Cut the plastic bottle in half horizontally.",
                                    "Poke small drainage holes in the bottom half of the bottle.",
                                    "Invert the top funnel part and place it inside the bottom half.",
                                    "Fill with potting soil, plant mint seeds, and add water to create a self-watering system."
                                ),
                                benefit = "Saves money on plant pots, decorates your window sill with fresh, organic kitchen herbs!"
                            )
                        ),
                        funFact = "Recycling one ton of plastic saves massive landfill space and avoids massive crude oil imports!",
                        pointsEarned = 50,
                        badgeUnlocked = "plastic_hero"
                    )
                }
            }
            cleanQuery.contains("ورق") || cleanQuery.contains("كرتون") || cleanQuery.contains("paper") || cleanQuery.contains("cardboard") -> {
                if (isAr) {
                    GeminiScanResult(
                        itemName = "صندوق كرتون الأغذية والطرود",
                        itemEmoji = "📦",
                        materialType = "ورق وكرتون",
                        recyclable = true,
                        recyclabilityScore = 10,
                        environmentalImpact = "الكرتون يتحلل ببطء في المكبات وينتج غاز الميثان الضار بالاحتباس الحراري.",
                        co2SavedGrams = 90.0,
                        waterSavedLiters = 2.6,
                        energySavedKwh = 0.4,
                        decompositionYears = "٢-٥ أشهر",
                        recyclingMethods = listOf(
                            RecyclingMethod(
                                method = "الكبس الجاف",
                                difficulty = "سهل",
                                steps = listOf("تفكيك الصندوق الكرتوني وتحويله لسطح مستوٍ تماماً", "حفظ الكرتون في مكان جاف تماماً وتجنب الرطوبة والمياه", "ربط المجموعة بحبل لسهولة تسليمها لمركز التجميع"),
                                result = "كرتون معاد ضغطه وصالح للإنتاج الورقي مجدداً",
                                wowFact = "كل طن من الورق والكرتون المعاد تدويره ينقذ ١٧ شجرة بالغة من القطع!"
                            )
                        ),
                        creativeReuse = listOf(
                            CreativeReuse(
                                idea = "منظم للأقلام والمستندات المكتبية",
                                difficulty = "سهل",
                                materialsNeeded = listOf("صمغ أو شريط لاصق", "مقص", "ألوان مائية للتزيين"),
                                timeMinutes = 20,
                                steps = listOf(
                                    "قم بقص أغطية الصندوق الكرتوني الجافة بشكل أنيق.",
                                    "قم بتقسيم الصندوق من الداخل باستخدام قطع الكرتون المتبقية كفواصل عمودية وأفقية.",
                                    "قم بتثبيت الفواصل باستخدام الصمغ أو الشريط اللاصق المتين.",
                                    "لّون المنظم من الخارج بالألوان المائية أو لفه بورق هدايا مستعمل لتزيينه."
                                ),
                                benefit = "تنظيم وترتيب مكتبك الدراسي مجاناً، وإعادة تدوير ورق الهدايا والكرتون في تحفة مكتبية عملية."
                            )
                        ),
                        funFact = "ألياف الكرتون قوية ويمكن إعادة تدويرها وصهرها من ٥ إلى ٧ مرات متتالية قبل أن تضعف!",
                        pointsEarned = 40
                    )
                } else {
                    GeminiScanResult(
                        itemName = "Cardboard Package Box",
                        itemEmoji = "📦",
                        materialType = "Paper & Cardboard",
                        recyclable = true,
                        recyclabilityScore = 10,
                        environmentalImpact = "Decomposes slowly in landfills and generates harmful methane gas contributes to greenhouse effect.",
                        co2SavedGrams = 90.0,
                        waterSavedLiters = 2.6,
                        energySavedKwh = 0.4,
                        decompositionYears = "2-5 Months",
                        recyclingMethods = listOf(
                            RecyclingMethod(
                                method = "Dry Crushing & Stacking",
                                difficulty = "Easy",
                                steps = listOf("Flatten the cardboard box completely to save space", "Keep it completely dry and avoid humidity or water", "Tie the package bundle with a string for easy pick-up"),
                                result = "Compressed clean cardboard ready for repulping and paper production",
                                wowFact = "Every ton of recycled paper or cardboard saves 17 mature trees from being cut!"
                            )
                        ),
                        creativeReuse = listOf(
                            CreativeReuse(
                                idea = "Desk Organizer & Pen Holder",
                                difficulty = "Easy",
                                materialsNeeded = listOf("Glue or tape", "Scissors", "Watercolors for decoration"),
                                timeMinutes = 20,
                                steps = listOf(
                                    "Cut off the top flaps of the dry cardboard box neatly.",
                                    "Use the cut flaps as interior dividers to partition the box into compartments.",
                                    "Secure the cardboard dividers in place using strong glue or tape.",
                                    "Paint the exterior with watercolors or wrap it in old gift wrapping paper for decoration."
                                ),
                                benefit = "Eliminates desk clutter, organizes school supplies, and creates a customized, beautiful desk accessory for zero cost."
                            )
                        ),
                        funFact = "Cardboard fibers are incredibly strong and can be repulped up to 5-7 times before wearing out!",
                        pointsEarned = 40
                    )
                }
            }
            cleanQuery.contains("معدن") || cleanQuery.contains("علبة") || cleanQuery.contains("كان") || cleanQuery.contains("metal") || cleanQuery.contains("can") -> {
                if (isAr) {
                    GeminiScanResult(
                        itemName = "علبة مشروبات ألومنيوم",
                        itemEmoji = "🥤",
                        materialType = "معدن",
                        recyclable = true,
                        recyclabilityScore = 10,
                        environmentalImpact = "تأخذ أكثر من ٢٠٠ سنة للتحلل وتتسبب في إهدار مناجم البوكسيت الطبيعية.",
                        co2SavedGrams = 170.0,
                        waterSavedLiters = 8.0,
                        energySavedKwh = 0.7,
                        decompositionYears = "٢٠٠ سنة",
                        recyclingMethods = listOf(
                            RecyclingMethod(
                                method = "الكبس البارد للمعادن",
                                difficulty = "سهل",
                                steps = listOf("شطف العبوة للتخلص من بقايا السكريات الجاذبة للحشرات", "سحق العبوة رأسياً لتقليص حجمها وحفظها في حاوية الفرز"),
                                result = "ألومنيوم نقي جاهز للصهر الفوري في المصانع",
                                wowFact = "إعادة تدوير الألومنيوم يستهلك ٥٪ فقط من الطاقة المطلوبة لاستخراج ألومنيوم جديد!"
                            )
                        ),
                        creativeReuse = listOf(
                            CreativeReuse(
                                idea = "حامل شموع رومانسي بنقوش مضيئة",
                                difficulty = "متوسط",
                                materialsNeeded = listOf("مسمار رفيع", "شاكوش صغير", "شمعة إلكترونية آمنة"),
                                timeMinutes = 25,
                                steps = listOf(
                                    "املأ علبة الكانز الفارغة بالماء بالكامل ثم ضعها في المجمد (الفريزر) حتى تتجمد تماماً لتثبيت الهيكل أثناء النقش.",
                                    "استخدم المسمار والشاكوش لنقش ثقوب على شكل هلال أو نجوم أو أنماط هندسية على جدار العلبة الخارجي.",
                                    "دع الثلج يذوب تماماً، ثم جفف العلبة من الداخل وافتح الجزء العلوي بحذر.",
                                    "ضع الشمعة الإلكترونية بداخلها لتستمتع بإضاءة دافئة ساحرة من خلال النقوش المثقوبة."
                                ),
                                benefit = "ابتكار إضاءة رومانسية رائعة لغرفتك أو شرفتك، والتوفير البديل لفوانيس الديكور باهظة الثمن!"
                            )
                        ),
                        funFact = "يمكن لعلبة الألومنيوم المعاد تدويرها أن تعود لرفوف المتاجر كعلبة جديدة بالكامل خلال ٦٠ يوماً فقط!",
                        pointsEarned = 60
                    )
                } else {
                    GeminiScanResult(
                        itemName = "Aluminum Soda Beverage Can",
                        itemEmoji = "🥤",
                        materialType = "Metal",
                        recyclable = true,
                        recyclabilityScore = 10,
                        environmentalImpact = "Takes over 200 years to decompose and wastes high-value native bauxite mine reserves.",
                        co2SavedGrams = 170.0,
                        waterSavedLiters = 8.0,
                        energySavedKwh = 0.7,
                        decompositionYears = "200 Years",
                        recyclingMethods = listOf(
                            RecyclingMethod(
                                method = "Cold Metal Pressing",
                                difficulty = "Easy",
                                steps = listOf("Rinse the can to clean sweet sticky liquids that attract bugs", "Squeeze or crush the can vertically to minimize its space"),
                                result = "Pure aluminum scraps ready for instant melting in foundry ovens",
                                wowFact = "Recycling aluminum consumes only 5% of the energy needed to mine and refine raw aluminum!"
                            )
                        ),
                        creativeReuse = listOf(
                            CreativeReuse(
                                idea = "Eco-Romantic Candle Holder",
                                difficulty = "Medium",
                                materialsNeeded = listOf("Thin metal nail", "Small hammer", "Safe electronic tea light"),
                                timeMinutes = 25,
                                steps = listOf(
                                    "Fill the aluminum can with water and freeze it completely to reinforce the metal during crafting.",
                                    "Place a pattern paper over the can, then tap the nail gently with a hammer to punch decorative holes (e.g., star shapes).",
                                    "Let the ice melt completely, dry the can, and carefully cut open or flatten the top opening.",
                                    "Place a battery-operated LED tea candle inside to project wonderful shadow shapes onto your walls."
                                ),
                                benefit = "Breathes artistic cozy lighting into your room, and upcycles old cans into beautiful premium lanterns."
                            )
                        ),
                        funFact = "An aluminum beverage can can be fully recycled, remade, and put back on retail shelves in just 60 days!",
                        pointsEarned = 60
                    )
                }
            }
            else -> {
                if (isAr) {
                    GeminiScanResult(
                        itemName = "مخلف عضوي منزلي (بقايا طعام)",
                        itemEmoji = "🌿",
                        materialType = "عضوي",
                        recyclable = true,
                        recyclabilityScore = 8,
                        environmentalImpact = "يتخمر في المكبات العادية وينتج غازات دفيئة سامة وروائح كريهة.",
                        co2SavedGrams = 50.0,
                        waterSavedLiters = 1.0,
                        energySavedKwh = 0.1,
                        decompositionYears = "أسبوعين إلى شهر",
                        recyclingMethods = listOf(
                            RecyclingMethod(
                                method = "الكمبوست المنزلي (التسميد العضوي)",
                                difficulty = "متوسط",
                                steps = listOf("فرز بقايا الخضروات والفواكه والقهوة (تجنب اللحوم والزيوت)", "خلطها مع أوراق شجر جافة أو كرتون مقطع لعمل توازن كربوني", "تهوية الخليط في صندوق التخميد أسبوعياً للحصول على سماد طبيعي غني"),
                                result = "سماد عضوي (كمبوست) غني ومغذي للتربة والزراعات المنزلية",
                                wowFact = "السماد العضوي يحبس رطوبة التربة ويقلل كمية مياه الري المطلوبة بنسبة ٣٠٪!"
                            )
                        ),
                        creativeReuse = listOf(
                            CreativeReuse(
                                idea = "عمل مغذي مغلي قشور الموز والبيض للنباتات",
                                difficulty = "سهل",
                                materialsNeeded = listOf("قشور الموز والبيض", "ماء دافئ", "زجاجة فارغة لبث الخليط"),
                                timeMinutes = 10,
                                steps = listOf(
                                    "اجمع قشور الموز والبيض الفارغة وقم بغسل قشور البيض وتجفيفها جيداً.",
                                    "قم بطحن قشور البيض ناعماً، واقطع قشور الموز إلى قطع صغيرة للغاية.",
                                    "انقع المكونات في ماء دافئ داخل زجاجة فارغة لمدة ٢٤ إلى ٤٨ ساعة لتتحلل المعادن كالمنجنيز والكالسيوم والبوتاسيوم في الماء.",
                                    "قم بتصفية السائل المغذي واروِ به نباتات الزينة المنزلية مرة كل أسبوعين لتعزيز نموها وقوتها."
                                ),
                                benefit = "الحصول على سماد سائل فائق الغذاء والفاعلية لنباتاتك المنزلية دون تكلفة، وتجنب استخدام الأسمدة الكيماوية المصنعة."
                            )
                        ),
                        funFact = "ثلث طعام سكان الأرض تقريباً يذهب هدراً، وتحويله لسماد ينقذ كوكبنا ويغذي حدائقنا مجاناً!",
                        pointsEarned = 30
                    )
                } else {
                    GeminiScanResult(
                        itemName = "Organic Household Food Scraps",
                        itemEmoji = "🌿",
                        materialType = "Organic",
                        recyclable = true,
                        recyclabilityScore = 8,
                        environmentalImpact = "Ferments in landfills and releases greenhouse gases like methane and bad odors.",
                        co2SavedGrams = 50.0,
                        waterSavedLiters = 1.0,
                        energySavedKwh = 0.1,
                        decompositionYears = "2-4 Weeks",
                        recyclingMethods = listOf(
                            RecyclingMethod(
                                method = "Home Composting",
                                difficulty = "Medium",
                                steps = listOf("Sort fruit, veggie scraps, and coffee grounds (avoid meat & oil)", "Mix with dry leaves or chopped paper to balance carbon", "Aerate the mixture weekly to obtain rich natural compost"),
                                result = "Rich organic compost perfect for household plants and gardens",
                                wowFact = "Organic compost retains soil moisture and cuts required watering volumes by 30%!"
                            )
                        ),
                        creativeReuse = listOf(
                            CreativeReuse(
                                idea = "Banana-Peel Plant Fertilizer Liquid",
                                difficulty = "Easy",
                                materialsNeeded = listOf("Banana peels, eggshells", "Warm water", "An empty bottle to brew"),
                                timeMinutes = 10,
                                steps = listOf(
                                    "Collect banana peels and empty eggshells, and clean the eggshells thoroughly.",
                                    "Crush the eggshells into a fine powder, and chop banana peels into tiny pieces.",
                                    "Steep the mixture in warm water inside a bottle for 24-48 hours to release potassium and calcium.",
                                    "Filter the liquid fertilizer and feed your domestic plants once every two weeks for strong growth."
                                ),
                                benefit = "Yields a supercharged organic liquid nutrient boost for house plants for zero dollars while redirecting kitchen scraps away from general municipal waste."
                            )
                        ),
                        funFact = "About one-third of the world's food is wasted. Turning it to compost saves the planet and fertilizes fields for free!",
                        pointsEarned = 30
                    )
                }
            }
        }
    }
}
