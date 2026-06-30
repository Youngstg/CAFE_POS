<?php

use Illuminate\Support\Facades\Route;
use App\Http\Controllers\Api\TelegramWebhookController;

// ─── Telegram Webhook ────────────────────────────────────────────
Route::post('/telegram/webhook', [TelegramWebhookController::class, 'handleWebhook']);
