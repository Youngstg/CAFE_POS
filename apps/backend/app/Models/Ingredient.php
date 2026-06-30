<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use App\Traits\BelongsToTenant;

class Ingredient extends Model
{
    use BelongsToTenant;
    //
}
