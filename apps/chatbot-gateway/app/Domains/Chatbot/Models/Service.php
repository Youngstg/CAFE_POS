<?php

namespace App\Domains\Chatbot\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Service extends Model
{
    protected $fillable = ['name', 'description', 'starting_price', 'duration', 'is_active'];

    protected $casts = [
        'starting_price' => 'decimal:2',
        'is_active'      => 'boolean',
    ];

    public function features(): HasMany
    {
        return $this->hasMany(ServiceFeature::class);
    }

    public function pricing(): HasMany
    {
        return $this->hasMany(Pricing::class);
    }
}
