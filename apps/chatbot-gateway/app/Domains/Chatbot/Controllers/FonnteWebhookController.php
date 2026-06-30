<?php

namespace App\Domains\Chatbot\Controllers;

use App\Http\Controllers\Controller;
use App\Prompts\SalesAssistantPrompt;
use App\Domains\Chatbot\Services\ConversationService;
use App\Domains\Chatbot\Services\FonnteService;
use App\Domains\Chatbot\Services\GeminiService;
use App\Domains\Chatbot\Services\KnowledgeBaseService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class FonnteWebhookController extends Controller
{
    public function __construct(
        protected FonnteService        $fonnteService,
        protected GeminiService        $geminiService,
        protected KnowledgeBaseService $knowledgeBaseService,
        protected ConversationService  $conversationService
    ) {}

    /**
     * Handle pesan masuk dari Fonnte
     * Fonnte mengirim POST dengan field: sender, message, device
     */
    public function handle(Request $request): JsonResponse
    {
        $sender  = $request->input('sender');   // nomor pengirim: 628xxxxxxxxxx
        $text    = $request->input('message');  // isi pesan
        $name    = $request->input('name');     // nama kontak (bila tersedia)

        if (! $sender || ! $text) {
            return response()->json(['ok' => true]);
        }

        $sessionId = 'fonnte_' . $sender;

        try {
            $conversation = $this->conversationService->getOrCreate($sessionId, $name);
            $this->conversationService->addMessage($conversation->id, 'user', $text);

            $context      = $this->knowledgeBaseService->retrieveContext($text);
            $systemPrompt = SalesAssistantPrompt::buildSystemPrompt($context);
            $history      = $this->conversationService->getHistoryForLLM($conversation->id, 10);

            $reply = $this->geminiService->chat($history, $systemPrompt);

            $this->conversationService->addMessage($conversation->id, 'assistant', $reply);
            $this->fonnteService->sendMessage($sender, $reply);
        } catch (\Exception $e) {
            \Illuminate\Support\Facades\Log::error('Fonnte handler error: ' . $e->getMessage());
            $this->fonnteService->sendMessage($sender, 'Maaf, terjadi kesalahan. Silakan coba lagi. 🙏');
        }

        return response()->json(['ok' => true]);
    }
}
