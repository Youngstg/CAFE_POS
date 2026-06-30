<?php

namespace App\Domains\Chatbot\Controllers;

use App\Http\Controllers\Controller;
use App\Repositories\KnowledgeBaseRepository;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class KnowledgeBaseController extends Controller
{
    public function __construct(protected KnowledgeBaseRepository $repository) {}

    public function index(): JsonResponse
    {
        return response()->json(['data' => $this->repository->getAll()]);
    }

    public function store(Request $request): JsonResponse
    {
        $data = $request->validate([
            'category' => 'required|in:service,pricing,faq,portfolio,promotion,policy',
            'title'    => 'required|string|max:255',
            'content'  => 'required|string',
        ]);

        $entry = $this->repository->create($data);

        return response()->json(['message' => 'Berhasil ditambahkan', 'data' => $entry], 201);
    }

    public function update(Request $request, int $id): JsonResponse
    {
        $data = $request->validate([
            'category'  => 'sometimes|in:service,pricing,faq,portfolio,promotion,policy',
            'title'     => 'sometimes|string|max:255',
            'content'   => 'sometimes|string',
            'is_active' => 'sometimes|boolean',
        ]);

        $this->repository->update($id, $data);

        return response()->json(['message' => 'Berhasil diperbarui']);
    }

    public function destroy(int $id): JsonResponse
    {
        $this->repository->delete($id);

        return response()->json(['message' => 'Berhasil dihapus']);
    }
}
