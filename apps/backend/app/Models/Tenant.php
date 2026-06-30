<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Tenant extends Model
{
    protected $fillable = [
        'name',
        'domain',
        'is_active',
        'telegram_bot_token',
        'system_prompt',
    ];
}
