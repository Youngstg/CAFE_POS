<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Menu extends Model
{
    protected $table = 'menus';

    public function tenant()
    {
        return $this->belongsTo(Tenant::class);
    }
}
