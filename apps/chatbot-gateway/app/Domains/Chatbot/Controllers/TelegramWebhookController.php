<?php

namespace App\Domains\Chatbot\Controllers;

use App\Http\Controllers\Controller;
use App\Domains\Chatbot\Jobs\ProcessTelegramMessage;
use App\Domains\Chatbot\Services\TelegramService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Cache;

class TelegramWebhookController extends Controller
{
    public function __construct(
        protected TelegramService $telegramService,
    ) {}

    /**
     * Handle incoming Telegram webhook update.
     * Langsung return 200 agar Telegram tidak retry,
     * lalu proses pesan via Job di background.
     */
    public function handle(Request $request): JsonResponse
    {
        $update = $request->all();

        // Hanya proses pesan teks biasa
        if (! isset($update['message']['text'])) {
            return response()->json(['ok' => true]);
        }

        $updateId = $update['update_id'];

        // Deduplication — cegah pesan yang sama diproses dua kali
        $cacheKey = 'tg_update_' . $updateId;
        if (Cache::has($cacheKey)) {
            return response()->json(['ok' => true]);
        }
        Cache::put($cacheKey, true, now()->addHours(24));

        $chatId    = $update['message']['chat']['id'];
        $text      = $update['message']['text'];
        $firstName = $update['message']['from']['first_name'] ?? '';
        $lastName  = $update['message']['from']['last_name'] ?? '';
        $userName  = trim("{$firstName} {$lastName}") ?: null;
        $sessionId = 'tg_' . $chatId;

        // Proses langsung secara sinkron tanpa queue
        ProcessTelegramMessage::dispatchSync($chatId, $text, $userName, $sessionId);

        return response()->json(['ok' => true]);
    }

    /**
     * Daftarkan webhook URL ke Telegram
     */
    public function setWebhook(Request $request): JsonResponse
    {
        $url    = $request->input('url', config('app.url') . '/api/webhook/telegram');
        $result = $this->telegramService->setWebhook($url);
        return response()->json($result);
    }

    /**
     * Hapus webhook (untuk ganti ke polling)
     */
    public function deleteWebhook(): JsonResponse
    {
        $result = $this->telegramService->deleteWebhook();
        return response()->json($result);
    }
}
