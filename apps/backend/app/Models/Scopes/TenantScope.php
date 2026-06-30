<?php

namespace App\Models\Scopes;

use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Scope;

class TenantScope implements Scope
{
    /**
     * Apply the scope to a given Eloquent query builder.
     */
    public function apply(Builder $builder, Model $model): void
    {
        // Check if there is an active tenant context.
        // We will store the current tenant id in the service container or configuration.
        // For now, we will use a global helper or config if available.
        if (app()->has('current_tenant_id')) {
            $tenantId = app('current_tenant_id');
            if ($tenantId) {
                $builder->where($model->getTable() . '.tenant_id', $tenantId);
            }
        }
    }
}
