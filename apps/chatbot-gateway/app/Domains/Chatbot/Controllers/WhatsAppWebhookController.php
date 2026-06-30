<?php

namespace App\Domains\Chatbot\Controllers;

use App\Http\Controllers\Controller;
use App\Prompts\SalesAssistantPrompt;
use App\Domains\Chatbot\Services\ConversationService;
use App\Domains\Chatbot\Services\GeminiService;
use App\Domains\Chatbot\Services\KnowledgeBaseService;
use App\Domains\Chatbot\Services\WhatsAppService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Response;

class WhatsAppWebhookController extends Controller
{
    public function __construct(
        protected WhatsAppService      $whatsAppService,
        protected GeminiService        $geminiService,
        protected KnowledgeBaseService $knowledgeBaseService,
        protected ConversationService  $conversationService
    ) {}

    /**
     * GET - Verifikasi webhook dari Meta
     */
    public function verify(Request $request): Response|JsonResponse
    {
        $mode        = $request->get('hub_mode');
        $token       = $request->get('hub_verify_token');
        $challenge   = $request->get('hub_challenge');
        $verifyToken = config('services.whatsapp.verify_token');

        if ($mode === 'subscribe' && $token === $verifyToken) {
            return response($challenge, 200);
        }

        return response()->json(['error' => 'Forbidden'], 403);
    }

    /**
     * POST - Handle pesan masuk dari WhatsApp
     */
    public function handle(Request $request): JsonResponse
    {
        $body = $request->all();

        // Pastikan ini adalah message event
        $entry   = $body['entry'][0] ?? null;
        $changes = $entry['changes'][0] ?? null;
        $value   = $changes['value'] ?? null;

        if (! isset($value['messages'][0])) {
            return response()->json(['ok' => true]);
        }

        $message = $value['messages'][0];

        // Hanya proses pesan teks
        if ($message['type'] !== 'text') {
            return response()->json(['ok' => true]);
        }

        $from        = $message['from'];                                    // nomor WA pengirim
        $messageId   = $message['id'];
        $text        = $message['text']['body'];
        $contactName = $value['contacts'][0]['profile']['name'] ?? null;

        // Mark as read
        $this->whatsAppService->markAsRead($messageId);

        // Gunakan nomor WA sebagai session_id
        $sessionId = 'wa_' . $from;

        try {
            $conversation = $this->conversationService->getOrCreate($sessionId, $contactName);
            $this->conversationService->addMessage($conversation->id, 'user', $text);

            $context      = $this->knowledgeBaseService->retrieveContext($text);
            $systemPrompt = SalesAssistantPrompt::buildSystemPrompt($context);
            $history      = $this->conversationService->getHistoryForLLM($conversation->id, 10);

            $reply = $this->geminiService->chat($history, $systemPrompt);

            $this->conversationService->addMessage($conversation->id, 'assistant', $reply);
            $this->whatsAppService->sendMessage($from, $reply);
        } catch (\Exception $e) {
            \Illuminate\Support\Facades\Log::error('WhatsApp handler error: ' . $e->getMessage());
            $this->whatsAppService->sendMessage($from, 'Maaf, terjadi kesalahan. Silakan coba lagi. 🙏');
        }

        return response()->json(['ok' => true]);
    }
}
