<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use App\Domains\Chatbot\Models\KnowledgeBase;

class KnowledgeBaseSeeder extends Seeder
{
    public function run(): void
    {
        $tenant = \App\Models\Tenant::first();
        $tenantId = $tenant ? $tenant->id : 1;

        $entries = [
            [
                'tenant_id' => $tenantId,
                'category' => 'faq',
                'title'    => 'Berapa lama proses pembuatan website?',
                'content'  => 'Durasi pengerjaan tergantung jenis layanan: Website Company Profile 7-14 hari, E-Commerce 14-30 hari, Mobile App 30-60 hari. Durasi bisa bervariasi tergantung kompleksitas dan kecepatan feedback dari klien.',
            ],
            [
                'tenant_id' => $tenantId,
                'category' => 'faq',
                'title'    => 'Apakah bisa melakukan revisi?',
                'content'  => 'Ya, kami menyediakan revisi selama proses pengerjaan. Jumlah revisi tergantung paket yang dipilih. Kami memprioritaskan kepuasan klien dalam setiap proyek.',
            ],
            [
                'tenant_id' => $tenantId,
                'category' => 'faq',
                'title'    => 'Apakah ada garansi setelah selesai?',
                'content'  => 'Ya, kami memberikan garansi bug fixing selama 1 bulan setelah project selesai dan diserahterimakan. Garansi mencakup perbaikan bug dan error teknis, tidak termasuk perubahan desain atau fitur baru.',
            ],
            [
                'tenant_id' => $tenantId,
                'category' => 'faq',
                'title'    => 'Bagaimana sistem pembayaran?',
                'content'  => 'Sistem pembayaran kami: 50% di awal sebagai tanda jadi, dan 50% setelah project selesai dan disetujui. Kami menerima transfer bank, e-wallet (GoPay, OVO, DANA).',
            ],
            [
                'tenant_id' => $tenantId,
                'category' => 'policy',
                'title'    => 'Kebijakan Privasi',
                'content'  => 'Kami menjamin kerahasiaan data dan informasi klien. Seluruh source code dan materi pendukung yang diserahkan akan menjadi hak milik penuh klien setelah pelunasan pembayaran.',
            ]
        ];

        foreach ($entries as $entry) {
            KnowledgeBase::updateOrCreate(
                ['title' => $entry['title']],
                $entry
            );
        }
    }
}
