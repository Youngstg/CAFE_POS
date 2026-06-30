<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class TenantMiddleware
{
    /**
     * Handle an incoming request.
     *
     * @param  \Closure(\Illuminate\Http\Request): (\Symfony\Component\HttpFoundation\Response)  $next
     */
    public function handle(Request $request, Closure $next): Response
    {
        // 1. If user is authenticated and has a tenant_id, use that.
        if (auth()->check() && auth()->user()->tenant_id) {
            app()->instance('current_tenant_id', auth()->user()->tenant_id);
        }
        // 2. Otherwise, check if tenant_id is passed via header (e.g. for customer API calls where tenant is known)
        elseif ($request->hasHeader('X-Tenant-ID')) {
            app()->instance('current_tenant_id', $request->header('X-Tenant-ID'));
        }
        // 3. (Optional) For webhook/chatbot routes, the tenant might be resolved in the controller
        // and set manually using app()->instance('current_tenant_id', $id)

        return $next($request);
    }
}
