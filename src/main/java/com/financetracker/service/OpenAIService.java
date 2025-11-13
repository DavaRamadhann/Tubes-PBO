package com.financetracker.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

public class OpenAIService {

    private final OpenAIClient client;

    public OpenAIService() {
        String apiKey = System.getenv("OPENAI_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException(
                    "Environment variable OPENAI_API_KEY tidak ditemukan. " +
                    "Set API key terlebih dahulu."
            );
        }

        this.client = OpenAIOkHttpClient.fromEnv();
    }

    /**
     * Menghasilkan saran keuangan menggunakan OpenAI Responses API (SDK baru).
     */
    public String getFinancialAdvice(String transactionSummary) {

        String systemPrompt =
                """
                Anda adalah penasihat keuangan pribadi yang berpengalaman.
                Analisis transaksi pengguna dan berikan saran keuangan praktis,
                realistis, dan relevan dengan kondisi Indonesia.
                Fokus hanya pada 2–3 hal utama yang paling berdampak.
                Bahasa harus jelas dan mudah dipahami.
                """;

        String userPrompt =
                """
                Berikut ringkasan transaksi saya:

                %s

                Tolong berikan saran keuangan yang spesifik dan actionable.
                """.formatted(transactionSummary);

        // Gabungkan system + user input
        String fullPrompt = systemPrompt + "\n\n" + userPrompt;

        ResponseCreateParams params = ResponseCreateParams.builder()
                .model("gpt-4.1-mini")       // model modern, akurat & murah
                .input(fullPrompt)
                .maxOutputTokens(400)
                .temperature(0.7)
                .build();

        Response response = client.responses().create(params);

        String raw = response.output().get(0).toString();
        return raw;
    }
}
