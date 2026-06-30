<?php

namespace App\Domains\Chatbot\Services;

use App\Domains\Chatbot\Models\Lead;
use App\Repositories\LeadRepository;
use Illuminate\Database\Eloquent\Collection;

class LeadService
{
    public function __construct(
        protected LeadRepository $leadRepository
    ) {}

    public function create(array $data): Lead
    {
        return $this->leadRepository->create($data);
    }

    public function getAll(): Collection
    {
        return $this->leadRepository->getAll();
    }

    public function updateStatus(int $id, string $status): bool
    {
        return $this->leadRepository->updateStatus($id, $status);
    }
}
