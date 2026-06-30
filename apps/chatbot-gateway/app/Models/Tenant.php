<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Tenant extends Model
{
    protected $table = 'tenants';

    public function knowledgeBases()
    {
        return $this->hasMany(KnowledgeBase::class);
    }

    public function menus()
    {
        return $this->hasMany(Menu::class);
    }
}
