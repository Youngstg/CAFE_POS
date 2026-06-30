<?php

namespace App\Domains\Chatbot\Services;

use App\Domains\Chatbot\Models\Conversation;
use App\Repositories\ConversationRepository;

class ConversationService
{
    public function __construct(
        protected ConversationRepository $conversationRepository
    ) {}

    public function getOrCreate(string $sessionId, ?string $userName = null): Conversation
    {
        return $this->conversationRepository->findOrCreateBySessionId($sessionId, $userName);
    }

    public function addMessage(int $conversationId, string $role, string $content): void
    {
        $this->conversationRepository->addMessage($conversationId, $role, $content);
    }

    /**
     * Hapus semua pesan dalam conversation (reset context)
     */
    public function clearHistory(string $sessionId): void
    {
        $conversation = $this->conversationRepository->findOrCreateBySessionId($sessionId);
        \App\Domains\Chatbot\Models\Message::where('conversation_id', $conversation->id)->delete();
    }

    /**
     * Get conversation history formatted for Gemini API.
     * Returns [['role' => 'user'|'assistant', 'content' => '...']]
     */
    public function getHistoryForLLM(int $conversationId, int $limit = 10): array
    {
        $messages = $this->conversationRepository->getRecentMessages($conversationId, $limit);

        return $messages->map(fn ($msg) => [
            'role'    => $msg->role,
            'content' => $msg->content,
        ])->toArray();
    }
}
