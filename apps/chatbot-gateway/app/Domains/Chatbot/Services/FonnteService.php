<?php

namespace App\Domains\Chatbot\Services;

use GuzzleHttp\Client;
use GuzzleHttp\Exception\RequestException;
use Illuminate\Support\Facades\Log;

class FonnteService
{
    protected Client $client;
    protected string $token;

    public function __construct()
    {
        $this->token  = config('services.fonnte.token', '');
        $this->client = new Client(['timeout' => 15]);
    }

    /**
     * Send a text message via Fonnte API
     *
     * @param string $to Phone number (format: 628xxxxxxxxxx)
     * @param string $text Message text
     */
    public function sendMessage(string $to, string $text): void
    {
        try {
            $this->client->post('https://api.fonnte.com/send', [
                'headers' => [
                    'Authorization' => $this->token,
                ],
                'form_params' => [
                    'target'  => $to,
                    'message' => $text,
                ],
            ]);
        } catch (RequestException $e) {
            Log::error('Fonnte sendMessage error: ' . $e->getMessage());
        }
    }
}
