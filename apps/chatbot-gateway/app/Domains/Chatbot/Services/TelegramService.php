<?php

namespace App\Domains\Chatbot\Services;

use GuzzleHttp\Client;
use GuzzleHttp\Exception\RequestException;
use Illuminate\Support\Facades\Log;

class TelegramService
{
    protected Client $client;
    protected string $token;
    protected string $baseUrl;

    public function __construct()
    {
        $this->token   = config('services.telegram.token', '');
        $this->baseUrl = "https://api.telegram.org/bot{$this->token}";
        $this->client  = new Client(['timeout' => 15]);
    }

    public function sendMessage(int|string $chatId, string $text): void
    {
        try {
            $this->client->post("{$this->baseUrl}/sendMessage", [
                'json' => [
                    'chat_id'    => $chatId,
                    'text'       => $text,
                    'parse_mode' => 'HTML',
                ],
            ]);
        } catch (RequestException $e) {
            Log::error('Telegram sendMessage error: ' . $e->getMessage());
        }
    }

    public function sendTyping(int|string $chatId): void
    {
        try {
            $this->client->post("{$this->baseUrl}/sendChatAction", [
                'json' => ['chat_id' => $chatId, 'action' => 'typing'],
            ]);
        } catch (RequestException $e) {
            Log::warning('Telegram sendTyping error: ' . $e->getMessage());
        }
    }

    public function setWebhook(string $url): array
    {
        try {
            $response = $this->client->post("{$this->baseUrl}/setWebhook", [
                'json' => ['url' => $url],
            ]);
            return json_decode($response->getBody()->getContents(), true);
        } catch (RequestException $e) {
            return ['ok' => false, 'description' => $e->getMessage()];
        }
    }

    public function deleteWebhook(): array
    {
        try {
            $response = $this->client->post("{$this->baseUrl}/deleteWebhook");
            return json_decode($response->getBody()->getContents(), true);
        } catch (RequestException $e) {
            return ['ok' => false, 'description' => $e->getMessage()];
        }
    }
}
