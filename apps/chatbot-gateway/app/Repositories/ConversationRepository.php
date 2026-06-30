<?php

namespace App\Repositories;

use App\Domains\Chatbot\Models\Conversation;
use App\Domains\Chatbot\Models\Message;
use Illuminate\Database\Eloquent\Collection;

class ConversationRepository
{
    public function findOrCreateBySessionId(string $sessionId, ?string $userName = null): Conversation
    {
        $conversation = Conversation::where('session_id', $sessionId)->first();

        if (! $conversation) {
            $conversation = Conversation::create([
                'session_id' => $sessionId,
                'user_name'  => $userName,
            ]);
        } elseif ($userName && ! $conversation->user_name) {
            $conversation->update(['user_name' => $userName]);
        }

        return $conversation;
    }

    public function addMessage(int $conversationId, string $role, string $content): Message
    {
        return Message::create([
            'conversation_id' => $conversationId,
            'role'            => $role,
            'content'         => $content,
        ]);
    }

    public function getRecentMessages(int $conversationId, int $limit = 10): Collection
    {
        return Message::where('conversation_id', $conversationId)
            ->where('role', '!=', 'system')
            ->orderBy('created_at', 'desc')
            ->limit($limit)
            ->get()
            ->reverse()
            ->values();
    }
}
