<?php

namespace App\Domains\CoffeeShop\Models;

use Illuminate\Database\Eloquent\Model;
use App\Traits\BelongsToTenant;

class Ingredient extends Model
{
    use BelongsToTenant;
    //
}
