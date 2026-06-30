<?php

namespace App\Domains\Chatbot\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class ServiceFeature extends Model
{
    protected $fillable = ['service_id', 'feature_name', 'feature_description'];

    public function service(): BelongsTo
    {
        return $this->belongsTo(Service::class);
    }
}
