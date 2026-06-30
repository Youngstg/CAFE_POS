<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class KnowledgeBase extends Model
{
    protected $table = 'knowledge_base';

    public function tenant()
    {
        return $this->belongsTo(Tenant::class);
    }
}
