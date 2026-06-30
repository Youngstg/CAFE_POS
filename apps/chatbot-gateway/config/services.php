<?php

return [

    /*
    |--------------------------------------------------------------------------
    | Third Party Services
    |--------------------------------------------------------------------------
    |
    | This file is for storing the credentials for third party services such
    | as Mailgun, Postmark, AWS and more. This file provides the de facto
    | location for this type of information, allowing packages to have
    | a conventional file to locate the various service credentials.
    |
    */

    'postmark' => [
        'key' => env('POSTMARK_API_KEY'),
    ],

    'resend' => [
        'key' => env('RESEND_API_KEY'),
    ],

    'ses' => [
        'key' => env('AWS_ACCESS_KEY_ID'),
        'secret' => env('AWS_SECRET_ACCESS_KEY'),
        'region' => env('AWS_DEFAULT_REGION', 'us-east-1'),
    ],

    'slack' => [
        'notifications' => [
            'bot_user_oauth_token' => env('SLACK_BOT_USER_OAUTH_TOKEN'),
            'channel' => env('SLACK_BOT_USER_DEFAULT_CHANNEL'),
        ],
    ],

    'gemini' => [
        'key'        => env('GEMINI_API_KEY'),
        'base_url'   => 'https://generativelanguage.googleapis.com/v1beta',
        // Fallback chain — dicoba dari atas ke bawah saat rate limit
        'model'      => env('GEMINI_MODEL',      'gemini-2.5-flash'),
        'fallback_1' => env('GEMINI_FALLBACK_1', 'gemini-3.1-flash-lite'),
        'fallback_2' => env('GEMINI_FALLBACK_2', 'gemma-4-27b-it'),
        'fallback_3' => env('GEMINI_FALLBACK_3', null),
    ],

    'telegram' => [
        'token' => env('TELEGRAM_BOT_TOKEN'),
    ],

    'whatsapp' => [
        'token'           => env('WHATSAPP_TOKEN'),
        'phone_number_id' => env('WHATSAPP_PHONE_NUMBER_ID'),
        'verify_token'    => env('WHATSAPP_VERIFY_TOKEN', 'digo_webhook_secret'),
        'api_version'     => env('WHATSAPP_API_VERSION', 'v18.0'),
    ],

    'fonnte' => [
        'token' => env('FONNTE_TOKEN'),
    ],

];
