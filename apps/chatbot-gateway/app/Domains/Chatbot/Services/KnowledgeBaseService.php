<?php

namespace App\Domains\Chatbot\Services;

use App\Domains\Chatbot\Models\KnowledgeBase;
use App\Domains\Chatbot\Models\Service;

class KnowledgeBaseService
{
    /**
     * Retrieve relevant knowledge base context based on user query.
     */
    public function retrieveContext(string $query): string
    {
        $knowledgeEntries = KnowledgeBase::active()
            ->where(function ($q) use ($query) {
                $q->where('title', 'like', "%{$query}%")
                  ->orWhere('content', 'like', "%{$query}%");
            })
            ->limit(3)
            ->get();

        $services = Service::where('is_active', true)
            ->where(function ($q) use ($query) {
                $q->where('name', 'like', "%{$query}%")
                  ->orWhere('description', 'like', "%{$query}%");
            })
            ->with(['features', 'pricing'])
            ->limit(3)
            ->get();

        // If no specific match, return all services as general context
        if ($services->isEmpty()) {
            $services = Service::where('is_active', true)
                ->with(['features', 'pricing'])
                ->get();
        }

        return $this->formatContext($knowledgeEntries->toArray(), $services->toArray());
    }

    public function formatContext(array $knowledgeEntries = [], array $services = []): string
    {
        $context = '';

        if (! empty($services)) {
            $context .= "=== LAYANAN TERSEDIA ===\n";
            foreach ($services as $service) {
                $context .= "\n[{$service['name']}]\n";
                $context .= "Deskripsi: {$service['description']}\n";
                $context .= "Harga Mulai: Rp " . number_format($service['starting_price'], 0, ',', '.') . "\n";
                $context .= "Durasi: {$service['duration']}\n";

                if (! empty($service['features'])) {
                    $context .= "Fitur:\n";
                    foreach ($service['features'] as $feature) {
                        $context .= "- {$feature['feature_name']}";
                        if ($feature['feature_description']) {
                            $context .= ": {$feature['feature_description']}";
                        }
                        $context .= "\n";
                    }
                }
            }
        }

        if (! empty($knowledgeEntries)) {
            $context .= "\n=== INFORMASI TAMBAHAN ===\n";
            foreach ($knowledgeEntries as $entry) {
                $context .= "\n[{$entry['title']}]\n{$entry['content']}\n";
            }
        }

        return $context;
    }

    public function getAllContext(): string
    {
        $services = Service::where('is_active', true)->with(['features', 'pricing'])->get();
        $entries  = KnowledgeBase::active()->get();

        return $this->formatContext($entries->toArray(), $services->toArray());
    }
}
