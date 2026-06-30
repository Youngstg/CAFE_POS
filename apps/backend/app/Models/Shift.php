<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use App\Traits\BelongsToTenant;

class Shift extends Model
{
    use BelongsToTenant;
    //
}
