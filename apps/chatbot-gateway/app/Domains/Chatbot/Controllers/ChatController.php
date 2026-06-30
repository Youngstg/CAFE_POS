<?php

namespace App\Domains\Chatbot\Controllers;

use App\Http\Controllers\Controller;
use App\Http\Requests\StoreChatRequest;
use App\Prompts\SalesAssistantPrompt;
use App\Domains\Chatbot\Services\ConversationService;
use App\Domains\Chatbot\Services\GeminiService;
use App\Domains\Chatbot\Services\KnowledgeBaseService;
use Illuminate\Http\JsonResponse;
use Illuminate\Support\Str;

class ChatController extends Controller
{
    public function __construct(
        protected GeminiService $geminiService,
        protected KnowledgeBaseService $knowledgeBaseService,
        protected ConversationService $conversationService
    ) {}

    public function store(StoreChatRequest $request): JsonResponse
    {
        $sessionId   = $request->session_id ?? Str::uuid()->toString();
        $userMessage = $request->message;
        $userName    = $request->user_name;

        // 1. Get or create conversation
        $conversation = $this->conversationService->getOrCreate($sessionId, $userName);

        // 2. Save user message
        $this->conversationService->addMessage($conversation->id, 'user', $userMessage);

        // 3. Retrieve relevant knowledge base context
        $context = $this->knowledgeBaseService->retrieveContext($userMessage);

        // 4. Build system prompt with context
        $systemPrompt = SalesAssistantPrompt::buildSystemPrompt($context);

        // 5. Get conversation history for LLM (last 10 messages)
        $history = $this->conversationService->getHistoryForLLM($conversation->id, 10);

        // 6. Send to Gemini
        $aiResponse = $this->geminiService->chat($history, $systemPrompt);

        // 7. Save AI response
        $this->conversationService->addMessage($conversation->id, 'assistant', $aiResponse);

        return response()->json([
            'reply'           => $aiResponse,
            'session_id'      => $sessionId,
            'conversation_id' => $conversation->id,
        ]);
    }
}
