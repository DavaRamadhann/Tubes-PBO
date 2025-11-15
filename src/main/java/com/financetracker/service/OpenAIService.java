package com.financetracker.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

/**
 * OpenAIService - Versi robust untuk berbagai variasi SDK 4.x
 * - Menggunakan reflection untuk mengekstrak teks dari Response (menghindari masalah Optional/variant API)
 * - Fallback ke parsing toString() untuk format tak terduga
 * - Menyediakan startFinancialAdviceSession() dan continueChat()
 */
public class OpenAIService {

    private final OpenAIClient client;
    private final List<String> chatHistory = new ArrayList<>();
    private String currentReportContext = null;

    public OpenAIService() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Environment variable OPENAI_API_KEY tidak ditemukan. Set API key terlebih dahulu.");
        }
        this.client = OpenAIOkHttpClient.fromEnv();
    }

    /**
     * Memulai sesi analisis berdasarkan ringkasan laporan.
     */
    public String startFinancialAdviceSession(String reportSummary) {
        if (reportSummary == null) reportSummary = "";

        this.currentReportContext = reportSummary;
        this.chatHistory.clear();

        String systemPrompt =
                "Anda adalah penasihat keuangan pribadi yang berpengalaman. " +
                "Analisis data dengan konteks Indonesia, berikan saran praktis. " +
                "Setelah analisis ringkas, ajukan 1 pertanyaan lanjutan untuk memperjelas.";

        String userPrompt = "Berikut laporan keuangan pengguna:\n\n" + reportSummary +
                "\n\nTolong beri analisis utama dan akhiri dengan 1 pertanyaan lanjutan.";

        String combined = systemPrompt + "\n\n" + userPrompt;

        ResponseCreateParams params = ResponseCreateParams.builder()
                .model("gpt-5.1")
                .input(combined)
                .maxOutputTokens(4096)
                .temperature(0.6)
                .build();

        Response resp = client.responses().create(params);
        String aiReply = safeExtractText(resp);

        chatHistory.add("SYSTEM: " + systemPrompt);
        chatHistory.add("USER: " + userPrompt);
        chatHistory.add("AI: " + aiReply);

        return aiReply;
    }

    /**
     * Lanjutan percakapan.
     */
    public String continueChat(String userMessage) {
        if (currentReportContext == null) {
            return "Sesi analisis belum dimulai. Klik 'Dapatkan Saran Keuangan (AI)' dulu.";
        }

        chatHistory.add("USER: " + userMessage);

        StringBuilder prompt = new StringBuilder();
        prompt.append("Anda adalah penasihat keuangan yang sedang berdiskusi dengan user.\n");
        prompt.append("Berikut konteks laporan keuangan asli (referensi penting):\n");
        prompt.append(currentReportContext).append("\n\n");
        prompt.append("Riwayat percakapan:\n");
        for (String line : chatHistory) {
            prompt.append(line).append("\n");
        }
        prompt.append("\nPesan terbaru user:\n").append(userMessage);

        ResponseCreateParams params = ResponseCreateParams.builder()
                .model("gpt-5.1")
                .input(prompt.toString())
                .maxOutputTokens(4096)
                .temperature(0.6)
                .build();

        Response resp = client.responses().create(params);
        String aiReply = safeExtractText(resp);

        chatHistory.add("AI: " + aiReply);
        return aiReply;
    }

    /**
     * Akhiri sesi (opsional).
     */
    public void endSession() {
        this.currentReportContext = null;
        this.chatHistory.clear();
    }

    // ---------------------
    // EXTRACTOR (robust)
    // ---------------------
    /**
     * Menangkap teks dari Response dengan beberapa strategi:
     * 1) Coba jelajah reflektif ke chain common: output -> message(Optional?) -> content -> outputText -> text
     * 2) Coba akses langsung: output().get(0).text()
     * 3) Fallback parsing dari response.output().get(0).toString() dengan regex text="..."
     * 4) Jika semua gagal, kembalikan toString() yang bersih
     */
    private String safeExtractText(Response response) {
        if (response == null) return "[Tidak ada respons dari server]";

        // 1) Coba struktur reflektif berlapis
        try {
            Object outputList = invokeIfExists(response, "output"); // expect List<?>
            if (outputList instanceof List) {
                List<?> out = (List<?>) outputList;
                if (!out.isEmpty()) {
                    Object item0 = out.get(0);
                    // Try: item0.message() -> Optional or object
                    Object messageObj = tryInvoke(item0, "message");
                    if (messageObj != null) {
                        // unwrap Optional if present
                        messageObj = unwrapOptional(messageObj);
                        // try content()
                        Object contentObj = tryInvoke(messageObj, "content");
                        contentObj = unwrapOptional(contentObj);
                        if (contentObj instanceof List) {
                            List<?> contents = (List<?>) contentObj;
                            if (!contents.isEmpty()) {
                                Object c0 = contents.get(0);
                                // try outputText()
                                Object outputText = tryInvoke(c0, "outputText");
                                outputText = unwrapOptional(outputText);
                                if (outputText != null) {
                                    Object textObj = tryInvoke(outputText, "text");
                                    if (textObj == null) {
                                        // maybe text is a field accessible via toString()
                                        textObj = tryInvoke(c0, "text");
                                    }
                                    if (textObj != null) {
                                        return textObj.toString();
                                    }
                                } else {
                                    // maybe c0 has text() directly
                                    Object textObj2 = tryInvoke(c0, "text");
                                    if (textObj2 != null) return textObj2.toString();
                                }
                            }
                        }

                        // As fallback, maybe messageObj has content list with outputText etc
                        Object contentMaybe = tryInvoke(messageObj, "content");
                        contentMaybe = unwrapOptional(contentMaybe);
                        if (contentMaybe instanceof List) {
                            List<?> cont = (List<?>) contentMaybe;
                            if (!cont.isEmpty()) {
                                Object c0 = cont.get(0);
                                Object oText = tryInvoke(c0, "outputText");
                                oText = unwrapOptional(oText);
                                if (oText != null) {
                                    Object txt = tryInvoke(oText, "text");
                                    if (txt != null) return txt.toString();
                                }
                                Object txt2 = tryInvoke(c0, "text");
                                if (txt2 != null) return txt2.toString();
                            }
                        }
                    }

                    // 2) coba direct text() pada item0
                    Object directText = tryInvoke(item0, "text");
                    if (directText != null) return directText.toString();

                    // 3) coba message field inside item0 (some SDK returns item0.message as Optional<ResponseOutputMessage>)
                    Object msg2 = tryInvoke(item0, "message");
                    msg2 = unwrapOptional(msg2);
                    if (msg2 != null) {
                        Object content2 = tryInvoke(msg2, "content");
                        content2 = unwrapOptional(content2);
                        if (content2 instanceof List) {
                            List<?> cont2 = (List<?>) content2;
                            if (!cont2.isEmpty()) {
                                Object c0 = cont2.get(0);
                                Object textCheck = tryInvoke(c0, "text");
                                if (textCheck != null) return textCheck.toString();
                                Object outText = tryInvoke(c0, "outputText");
                                outText = unwrapOptional(outText);
                                if (outText != null) {
                                    Object t = tryInvoke(outText, "text");
                                    if (t != null) return t.toString();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignore) {
            // jangan gagal total di sini, lanjut ke fallback
        }

        // 4) fallback: ambil raw string dan cari pola text="..."/text=... atau content text value
        try {
            Object outputList = invokeIfExists(response, "output");
            String raw = String.valueOf(outputList != null ? outputList.toString() : response.toString());

            // Cari pola text="...":
            Pattern p = Pattern.compile("text\\s*=\\s*\"([\\s\\S]*?)\"");
            Matcher m = p.matcher(raw);
            if (m.find()) {
                return m.group(1).trim();
            }

            // Cari pola text=... without quotes up to closing brace
            p = Pattern.compile("text\\s*=\\s*([^,\\}\\]]+)");
            m = p.matcher(raw);
            if (m.find()) {
                return m.group(1).replaceAll("[\\}\\]]+$", "").trim();
            }

            // Cari pola outputText=... text=...
            p = Pattern.compile("outputText\\s*=\\s*\\{[^}]*text\\s*=\\s*\"([\\s\\S]*?)\"\\}");
            m = p.matcher(raw);
            if (m.find()) {
                return m.group(1).trim();
            }

            // jika gak ketemu, kembalikan raw but trimmed
            String cleaned = raw.replaceAll("\\s+", " ").trim();
            if (cleaned.length() > 1000) cleaned = cleaned.substring(0, 1000) + "...";
            return cleaned;
        } catch (Throwable t) {
            return "[Gagal parsing respons AI: " + t.getMessage() + "]";
        }
    }

    // ---------------------
    // REFLECTION HELPERS
    // ---------------------
    private static Object tryInvoke(Object target, String methodName) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName);
            return m.invoke(target);
        } catch (NoSuchMethodException nsme) {
            // ignore - method not present
            return null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    private static Object invokeIfExists(Object target, String methodName) {
        return tryInvoke(target, methodName);
    }

    private static Object unwrapOptional(Object possibleOptional) {
        if (possibleOptional == null) return null;
        if (possibleOptional instanceof Optional) {
            Optional<?> o = (Optional<?>) possibleOptional;
            return o.orElse(null);
        }
        return possibleOptional;
    }
}
