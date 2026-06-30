<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Http;
use App\Models\Tenant;
use Illuminate\Support\Facades\Log;

class TelegramWebhookController extends Controller
{
    public function handleWebhook(Request $request)
    {
        // 1. Ambil data dari request Telegram
        $update = $request->all();
        
        if (!isset($update['message'])) {
            return response()->json(['status' => 'ok']); // Hanya proses jika ada message
        }

        $message = $update['message'];
        $chatId = $message['chat']['id'] ?? null;
        $userText = $message['text'] ?? '';

        if (!$chatId || !$userText) {
            return response()->json(['status' => 'ok']);
        }

        // Karena kita menggunakan 1 Brand Utama, ambil Tenant pertama (CoffeeOS)
        $tenant = Tenant::with(['knowledgeBases', 'menus'])->first();

        if (!$tenant || !$tenant->telegram_bot_token) {
            Log::error("Telegram Bot Token tidak ditemukan di database Tenant.");
            return response()->json(['status' => 'error']);
        }

        // 2. Siapkan RAG (Retrieval Augmented Generation) Prompt
        $systemPrompt = $tenant->system_prompt ?? 'Anda adalah asisten AI yang ramah.';
        
        $kbText = "";
        if ($tenant->knowledgeBases->count() > 0) {
            $kbText = "\n\nINFORMASI PENTING (KNOWLEDGE BASE):\n";
            foreach ($tenant->knowledgeBases as $kb) {
                $kbText .= "- " . $kb->question . ": " . $kb->answer . "\n";
            }
        }

        $menuText = "";
        if ($tenant->menus->count() > 0) {
            $menuText = "\n\nDAFTAR MENU AKTIF:\n";
            foreach ($tenant->menus as $menu) {
                if ($menu->is_active) {
                    $menuText .= "- " . $menu->name . " (Rp " . number_format($menu->base_price, 0, ',', '.') . ")\n";
                }
            }
        }

        $fullSystemInstruction = $systemPrompt . $kbText . $menuText . "\n\nPastikan untuk merujuk pada informasi di atas saat menjawab.";

        // 3. Panggil Gemini API
        $geminiApiKey = env('GEMINI_API_KEY');
        if (!$geminiApiKey) {
            Log::error("GEMINI_API_KEY tidak ditemukan di .env.");
            $this->sendTelegramMessage($tenant->telegram_bot_token, $chatId, "Maaf, sistem AI sedang gangguan konfigurasi.");
            return response()->json(['status' => 'ok']);
        }

        $geminiResponseText = $this->callGeminiApi($geminiApiKey, $fullSystemInstruction, $userText);

        // 4. Kirim balik ke Telegram
        $this->sendTelegramMessage($tenant->telegram_bot_token, $chatId, $geminiResponseText);

        return response()->json(['status' => 'ok']);
    }

    private function callGeminiApi($apiKey, $systemInstruction, $userText)
    {
        $url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" . $apiKey;

        // Sesuai struktur Gemini 1.5 API
        $payload = [
            "system_instruction" => [
                "parts" => [
                    ["text" => $systemInstruction]
                ]
            ],
            "contents" => [
                [
                    "role" => "user",
                    "parts" => [
                        ["text" => $userText]
                    ]
                ]
            ]
        ];

        try {
            $response = Http::post($url, $payload);
            
            if ($response->successful()) {
                $data = $response->json();
                return $data['candidates'][0]['content']['parts'][0]['text'] ?? "Maaf, saya tidak mengerti maksud Anda.";
            } else {
                Log::error("Gemini API Error: " . $response->body());
                return "Maaf, saya sedang mengalami gangguan memori. (Error API)";
            }
        } catch (\Exception $e) {
            Log::error("Gemini Request Exception: " . $e->getMessage());
            return "Maaf, server AI sedang terputus.";
        }
    }

    private function sendTelegramMessage($botToken, $chatId, $text)
    {
        $url = "https://api.telegram.org/bot" . $botToken . "/sendMessage";
        
        try {
            Http::post($url, [
                'chat_id' => $chatId,
                'text' => $text,
                'parse_mode' => 'Markdown'
            ]);
        } catch (\Exception $e) {
            Log::error("Telegram Send Exception: " . $e->getMessage());
        }
    }
}
