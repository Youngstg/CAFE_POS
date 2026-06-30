<?php

namespace App\Domains\Chatbot\Services;

use GuzzleHttp\Client;
use GuzzleHttp\Exception\RequestException;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Log;

class GeminiService
{
    protected Client $client;
    protected string $apiKey;
    protected string $baseUrl;

    /**
     * Daftar model yang dicoba secara berurutan.
     * Jika model pertama kena rate limit, otomatis pakai berikutnya.
     */
    protected array $models;

    /**
     * Durasi model di-skip saat kena rate limit (detik)
     */
    protected const RATE_LIMIT_COOLDOWN = 3600; // 1 jam

    public function __construct()
    {
        $this->client  = new Client(['timeout' => 30]);
        $this->apiKey  = config('services.gemini.key');
        $this->baseUrl = config('services.gemini.base_url', 'https://generativelanguage.googleapis.com/v1beta');

        // Fallback chain — model dicoba dari atas ke bawah
        $this->models = array_values(array_filter([
            config('services.gemini.model'),
            config('services.gemini.fallback_1'),
            config('services.gemini.fallback_2'),
            config('services.gemini.fallback_3'),
        ]));
    }

    /**
     * Kirim pesan ke Gemini dengan auto-fallback antar model.
     *
     * @param  array  $messages  [['role' => 'user'|'assistant', 'content' => '...']]
     * @param  string $systemPrompt  System instruction untuk persona AI
     */
    public function chat(array $messages, string $systemPrompt = ''): string
    {
        $contents = array_map(fn ($msg) => [
            'role'  => $msg['role'] === 'assistant' ? 'model' : 'user',
            'parts' => [['text' => $msg['content']]],
        ], $messages);

        $payload = [
            'contents'         => $contents,
            'generationConfig' => [
                'temperature'     => 0.7,
                'topK'            => 40,
                'topP'            => 0.95,
                'maxOutputTokens' => 1024,
            ],
        ];

        if ($systemPrompt) {
            $payload['system_instruction'] = [
                'parts' => [['text' => $systemPrompt]],
            ];
        }

        foreach ($this->models as $model) {
            // Skip model yang sedang kena cooldown rate limit
            if ($this->isRateLimited($model)) {
                Log::info("GeminiService: skip {$model} (cooldown aktif)");
                continue;
            }

            $result = $this->tryModel($model, $payload);

            if ($result !== null) {
                return $result;
            }
            // null = model kena 429/503, coba model berikutnya
        }

        return 'Maaf, semua model AI sedang sibuk. Silakan coba beberapa menit lagi. 🙏';
    }

    /**
     * Coba kirim request ke satu model tertentu.
     * Return string jika sukses atau error non-quota.
     * Return null jika rate limited (agar dicoba model berikutnya).
     */
    protected function tryModel(string $model, array $payload): ?string
    {
        $endpoint = "{$this->baseUrl}/models/{$model}:generateContent?key={$this->apiKey}";

        try {
            $response = $this->client->post($endpoint, [
                'headers' => ['Content-Type' => 'application/json'],
                'json'    => $payload,
            ]);

            $data = json_decode($response->getBody()->getContents(), true);
            $text = $data['candidates'][0]['content']['parts'][0]['text']
                ?? 'Maaf, saya tidak dapat memberikan respons saat ini.';

            Log::info("GeminiService: sukses pakai model [{$model}]");

            return $this->stripMarkdown($text);

        } catch (RequestException $e) {
            $statusCode = $e->getResponse()?->getStatusCode();

            if (in_array($statusCode, [429, 503])) {
                // Tandai model ini kena rate limit → skip selama cooldown
                $this->markRateLimited($model);
                Log::warning("GeminiService: {$model} kena {$statusCode}, tandai cooldown 1 jam. Coba model berikutnya.");
                return null; // sinyal: coba model berikutnya
            }

            // Error lain (400, 500, dll) — tidak perlu coba model lain
            Log::error("GeminiService [{$model}] error {$statusCode}: " . $e->getMessage());
            return 'Maaf, terjadi kesalahan saat menghubungi AI. Silakan coba lagi.';
        }
    }

    /**
     * Cek apakah model sedang dalam cooldown rate limit
     */
    protected function isRateLimited(string $model): bool
    {
        return Cache::has('gemini_rate_limit_' . md5($model));
    }

    /**
     * Tandai model kena rate limit (akan di-skip selama RATE_LIMIT_COOLDOWN detik)
     */
    protected function markRateLimited(string $model): void
    {
        Cache::put(
            'gemini_rate_limit_' . md5($model),
            true,
            now()->addSeconds(self::RATE_LIMIT_COOLDOWN)
        );
    }

    /**
     * Reset cooldown semua model (untuk keperluan debug/admin)
     */
    public function resetRateLimits(): void
    {
        foreach ($this->models as $model) {
            Cache::forget('gemini_rate_limit_' . md5($model));
        }
        Log::info('GeminiService: semua cooldown model di-reset');
    }

    /**
     * Lihat status semua model (available / cooldown)
     */
    public function getModelStatus(): array
    {
        return collect($this->models)->mapWithKeys(fn ($model) => [
            $model => $this->isRateLimited($model) ? 'cooldown' : 'available',
        ])->toArray();
    }

    /**
     * Strip Markdown formatting dari respons AI.
     */
    protected function stripMarkdown(string $text): string
    {
        $text = preg_replace('/\*\*(.*?)\*\*/s', '$1', $text);
        $text = preg_replace('/__(.*?)__/s', '$1', $text);
        $text = preg_replace('/(?<!\*)\*(?!\*)(.*?)(?<!\*)\*(?!\*)/s', '$1', $text);
        $text = preg_replace('/(?<!_)_(?!_)(.*?)(?<!_)_(?!_)/s', '$1', $text);
        $text = preg_replace('/^#{1,6}\s+/m', '', $text);
        $text = preg_replace('/`([^`]+)`/', '$1', $text);
        $text = preg_replace('/```[\s\S]*?```/', '', $text);
        $text = preg_replace('/\n{3,}/', "\n\n", $text);

        return trim($text);
    }
}
