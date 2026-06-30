<?php

namespace App\Domains\CoffeeShop\Models;

use Illuminate\Database\Eloquent\Model;
use App\Traits\BelongsToTenant;

class OrderItem extends Model
{
    use BelongsToTenant;
    //
}
