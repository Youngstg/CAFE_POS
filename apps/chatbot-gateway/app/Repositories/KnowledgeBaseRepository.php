<?php

namespace App\Repositories;

use App\Domains\Chatbot\Models\KnowledgeBase;
use Illuminate\Database\Eloquent\Collection;

class KnowledgeBaseRepository
{
    public function getAll(): Collection
    {
        return KnowledgeBase::active()->orderBy('category')->get();
    }

    public function findByCategory(string $category): Collection
    {
        return KnowledgeBase::active()->byCategory($category)->get();
    }

    public function search(string $keyword): Collection
    {
        return KnowledgeBase::active()
            ->where(function ($query) use ($keyword) {
                $query->where('title', 'like', "%{$keyword}%")
                      ->orWhere('content', 'like', "%{$keyword}%");
            })
            ->limit(5)
            ->get();
    }

    public function create(array $data): KnowledgeBase
    {
        return KnowledgeBase::create($data);
    }

    public function update(int $id, array $data): bool
    {
        return (bool) KnowledgeBase::findOrFail($id)->update($data);
    }

    public function delete(int $id): bool
    {
        return (bool) KnowledgeBase::findOrFail($id)->delete();
    }
}
