<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class StoreChatRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'message'    => 'required|string|min:1|max:2000',
            'session_id' => 'nullable|string|max:255',
            'user_name'  => 'nullable|string|max:100',
        ];
    }
}
