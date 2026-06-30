<?php

namespace App\Domains\Chatbot\Jobs;

use App\Prompts\SalesAssistantPrompt;
use App\Domains\Chatbot\Services\ConversationService;
use App\Domains\Chatbot\Services\GeminiService;
use App\Domains\Chatbot\Services\KnowledgeBaseService;
use App\Domains\Chatbot\Services\TelegramService;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Queue\Queueable;
use Illuminate\Support\Facades\Log;

class ProcessTelegramMessage implements ShouldQueue
{
    use Queueable;

    public int $tries   = 1;
    public int $timeout = 90;

    // Pesan sambutan untuk /start
    private const WELCOME_MESSAGE = "Halo! 👋 Selamat datang di Digital Agency kami.\n\nSaya Digo, AI Sales Assistant yang siap membantu Anda menemukan layanan digital yang tepat.\n\nAda yang bisa saya bantu? 😊\n\nKetik /help untuk melihat daftar perintah.";

    // Perintah yang tersedia
    private const HELP_MESSAGE = "Perintah yang tersedia:\n\n/start - Mulai percakapan baru\n/clear - Hapus riwayat percakapan\n/end   - Akhiri sesi percakapan\n/help  - Tampilkan perintah ini";

    public function __construct(
        public readonly int|string $chatId,
        public readonly string     $text,
        public readonly ?string    $userName,
        public readonly string     $sessionId,
    ) {}

    public function handle(
        TelegramService      $telegramService,
        GeminiService        $geminiService,
        KnowledgeBaseService $knowledgeBaseService,
        ConversationService  $conversationService,
    ): void {
        // Cek apakah pesan adalah command
        if (str_starts_with($this->text, '/')) {
            $this->handleCommand(
                $this->text,
                $telegramService,
                $conversationService
            );
            return;
        }

        // Proses pesan biasa dengan AI
        try {
            $telegramService->sendTyping($this->chatId);

            $conversation = $conversationService->getOrCreate($this->sessionId, $this->userName);
            
            // --- LOGIKA MULTI-TENANT COFFEEOS ---
            if (! $conversation->tenant_id) {
                // Cari tenant berdasarkan input text (asumsi input adalah domain/kode toko)
                $tenant = \App\Models\Tenant::where('domain', trim(strtolower($this->text)))->first();
                
                if ($tenant) {
                    $conversation->update(['tenant_id' => $tenant->id]);
                    $telegramService->sendMessage(
                        $this->chatId,
                        "✅ Berhasil terhubung dengan *{$tenant->name}*!\n\nSilakan ketik pesanan Anda atau tanyakan menu kami."
                    );
                } else {
                    $telegramService->sendMessage(
                        $this->chatId,
                        "🏪 *Selamat Datang di platform CoffeeOS!*\n\nAnda belum memilih Coffee Shop. Silakan balas dengan *Kode Toko* (domain) yang ingin Anda tuju."
                    );
                }
                return; // Stop processing further until tenant is selected
            }

            // Set current tenant id for global scopes
            app()->instance('current_tenant_id', $conversation->tenant_id);
            // ------------------------------------

            $conversationService->addMessage($conversation->id, 'user', $this->text);

            $context      = $knowledgeBaseService->retrieveContext($this->text);
            $systemPrompt = SalesAssistantPrompt::buildSystemPrompt($context);
            $history      = $conversationService->getHistoryForLLM($conversation->id, 10);

            $reply = $geminiService->chat($history, $systemPrompt);

            $conversationService->addMessage($conversation->id, 'assistant', $reply);
            $telegramService->sendMessage($this->chatId, $reply);

        } catch (\Exception $e) {
            Log::error('ProcessTelegramMessage error: ' . $e->getMessage());
            $telegramService->sendMessage(
                $this->chatId,
                'Maaf, terjadi kesalahan. Silakan coba lagi. 🙏'
            );
        }
    }

    /**
     * Handle perintah bot Telegram (/start, /clear, /end, /help)
     */
    private function handleCommand(
        string              $command,
        TelegramService     $telegramService,
        ConversationService $conversationService,
    ): void {
        // Ambil command tanpa argumen (misal: /start@botname → /start)
        $cmd = strtolower(explode('@', explode(' ', trim($command))[0])[0]);

        match ($cmd) {
            '/start' => $this->cmdStart($telegramService, $conversationService),
            '/clear' => $this->cmdClear($telegramService, $conversationService),
            '/end'   => $this->cmdEnd($telegramService, $conversationService),
            '/help'  => $telegramService->sendMessage($this->chatId, self::HELP_MESSAGE),
            default  => $telegramService->sendMessage(
                $this->chatId,
                "Perintah tidak dikenal. Ketik /help untuk melihat perintah yang tersedia."
            ),
        };
    }

    private function cmdStart(
        TelegramService     $telegramService,
        ConversationService $conversationService,
    ): void {
        // Reset history lalu kirim sambutan
        $conversationService->clearHistory($this->sessionId);
        $telegramService->sendMessage($this->chatId, self::WELCOME_MESSAGE);
    }

    private function cmdClear(
        TelegramService     $telegramService,
        ConversationService $conversationService,
    ): void {
        $conversationService->clearHistory($this->sessionId);
        $telegramService->sendMessage(
            $this->chatId,
            "Riwayat percakapan telah dihapus. ✅\n\nSilakan mulai percakapan baru!"
        );
    }

    private function cmdEnd(
        TelegramService     $telegramService,
        ConversationService $conversationService,
    ): void {
        $conversationService->clearHistory($this->sessionId);
        $name = $this->userName ? ", {$this->userName}" : '';
        $telegramService->sendMessage(
            $this->chatId,
            "Terima kasih{$name}! Sesi percakapan telah diakhiri. 👋\n\nJika ingin memulai lagi, ketik /start kapan saja."
        );
    }
}
