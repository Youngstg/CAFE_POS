<?php

namespace App\Domains\Chatbot\Services;

use GuzzleHttp\Client;
use GuzzleHttp\Exception\RequestException;
use Illuminate\Support\Facades\Log;

class WhatsAppService
{
    protected Client $client;
    protected string $token;
    protected string $phoneNumberId;
    protected string $apiVersion;

    public function __construct()
    {
        $this->token         = config('services.whatsapp.token', '');
        $this->phoneNumberId = config('services.whatsapp.phone_number_id', '');
        $this->apiVersion    = config('services.whatsapp.api_version', 'v18.0');
        $this->client        = new Client(['timeout' => 15]);
    }

    /**
     * Send a text message via WhatsApp Cloud API
     */
    public function sendMessage(string $to, string $text): void
    {
        try {
            $this->client->post(
                "https://graph.facebook.com/{$this->apiVersion}/{$this->phoneNumberId}/messages",
                [
                    'headers' => [
                        'Authorization' => "Bearer {$this->token}",
                        'Content-Type'  => 'application/json',
                    ],
                    'json' => [
                        'messaging_product' => 'whatsapp',
                        'recipient_type'    => 'individual',
                        'to'                => $to,
                        'type'              => 'text',
                        'text'              => [
                            'preview_url' => false,
                            'body'        => $text,
                        ],
                    ],
                ]
            );
        } catch (RequestException $e) {
            Log::error('WhatsApp sendMessage error: ' . $e->getMessage());
        }
    }

    /**
     * Mark a message as read
     */
    public function markAsRead(string $messageId): void
    {
        try {
            $this->client->post(
                "https://graph.facebook.com/{$this->apiVersion}/{$this->phoneNumberId}/messages",
                [
                    'headers' => [
                        'Authorization' => "Bearer {$this->token}",
                        'Content-Type'  => 'application/json',
                    ],
                    'json' => [
                        'messaging_product' => 'whatsapp',
                        'status'            => 'read',
                        'message_id'        => $messageId,
                    ],
                ]
            );
        } catch (RequestException $e) {
            Log::warning('WhatsApp markAsRead error: ' . $e->getMessage());
        }
    }
}
