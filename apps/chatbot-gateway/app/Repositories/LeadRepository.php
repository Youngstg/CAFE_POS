<?php

namespace App\Repositories;

use App\Domains\Chatbot\Models\Lead;
use Illuminate\Database\Eloquent\Collection;

class LeadRepository
{
    public function create(array $data): Lead
    {
        return Lead::create($data);
    }

    public function getAll(): Collection
    {
        return Lead::orderBy('created_at', 'desc')->get();
    }

    public function findByEmail(string $email): ?Lead
    {
        return Lead::where('email', $email)->first();
    }

    public function updateStatus(int $id, string $status): bool
    {
        return (bool) Lead::findOrFail($id)->update(['status' => $status]);
    }
}
