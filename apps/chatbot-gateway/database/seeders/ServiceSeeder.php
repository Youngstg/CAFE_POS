<?php

namespace Database\Seeders;

use App\Domains\Chatbot\Models\Pricing;
use App\Domains\Chatbot\Models\Service;
use App\Domains\Chatbot\Models\ServiceFeature;
use Illuminate\Database\Seeder;

class ServiceSeeder extends Seeder
{
    public function run(): void
    {
        $services = [
            [
                'name'           => 'Website Company Profile',
                'description'    => 'Website profesional untuk menampilkan profil, layanan, dan kontak bisnis Anda.',
                'starting_price' => 2500000,
                'duration'       => '7-14 hari',
                'features' => [
                    ['feature_name' => 'Responsive Design',  'feature_description' => 'Tampil sempurna di semua perangkat'],
                    ['feature_name' => 'SEO Basic',           'feature_description' => 'Optimasi mesin pencari dasar'],
                    ['feature_name' => 'Contact Form',        'feature_description' => 'Formulir kontak terintegrasi'],
                    ['feature_name' => 'CMS Admin Panel',     'feature_description' => 'Kelola konten sendiri dengan mudah'],
                    ['feature_name' => '5 Halaman',           'feature_description' => 'Home, About, Services, Portfolio, Contact'],
                    ['feature_name' => 'Konsultasi 1 Bulan',  'feature_description' => 'Support teknis selama 1 bulan'],
                ],
                'pricing' => [
                    ['package_name' => 'Standard', 'price' => 2500000, 'notes' => '5 halaman, domain & hosting 1 tahun'],
                    ['package_name' => 'Premium',  'price' => 4500000, 'notes' => '10 halaman, domain & hosting 2 tahun, premium theme'],
                ],
            ],
            [
                'name'           => 'Website E-Commerce',
                'description'    => 'Platform toko online lengkap dengan keranjang belanja, pembayaran, dan manajemen produk.',
                'starting_price' => 7500000,
                'duration'       => '14-30 hari',
                'features' => [
                    ['feature_name' => 'Katalog Produk',    'feature_description' => 'Kelola produk dengan mudah'],
                    ['feature_name' => 'Keranjang Belanja', 'feature_description' => 'Sistem cart yang mudah digunakan'],
                    ['feature_name' => 'Payment Gateway',   'feature_description' => 'Midtrans / Xendit terintegrasi'],
                    ['feature_name' => 'Manajemen Order',   'feature_description' => 'Tracking pesanan real-time'],
                    ['feature_name' => 'Dashboard Admin',   'feature_description' => 'Panel admin lengkap'],
                ],
                'pricing' => [
                    ['package_name' => 'Basic',        'price' => 7500000,  'notes' => 'Hingga 100 produk'],
                    ['package_name' => 'Professional', 'price' => 12000000, 'notes' => 'Unlimited produk + fitur premium'],
                ],
            ],
            [
                'name'           => 'Mobile App Development',
                'description'    => 'Aplikasi mobile Android & iOS yang disesuaikan dengan kebutuhan bisnis Anda.',
                'starting_price' => 15000000,
                'duration'       => '30-60 hari',
                'features' => [
                    ['feature_name' => 'Android & iOS',    'feature_description' => 'Deploy ke kedua platform'],
                    ['feature_name' => 'UI/UX Design',     'feature_description' => 'Desain antarmuka yang modern'],
                    ['feature_name' => 'Backend API',      'feature_description' => 'API terintegrasi dengan app'],
                    ['feature_name' => 'Push Notification','feature_description' => 'Notifikasi real-time'],
                ],
                'pricing' => [
                    ['package_name' => 'Standard', 'price' => 15000000, 'notes' => 'React Native / Flutter'],
                ],
            ],
            [
                'name'           => 'UI/UX Design',
                'description'    => 'Desain antarmuka yang menarik, intuitif, dan berorientasi pada pengalaman pengguna.',
                'starting_price' => 3000000,
                'duration'       => '7-14 hari',
                'features' => [
                    ['feature_name' => 'Wireframe',     'feature_description' => 'Kerangka tampilan halaman'],
                    ['feature_name' => 'Mockup Design', 'feature_description' => 'Desain visual lengkap di Figma'],
                    ['feature_name' => 'Prototyping',   'feature_description' => 'Prototype interaktif'],
                    ['feature_name' => 'Revisi 3x',     'feature_description' => 'Tiga kali revisi desain'],
                ],
                'pricing' => [
                    ['package_name' => 'Standard', 'price' => 3000000, 'notes' => 'Per aplikasi / website'],
                ],
            ],
            [
                'name'           => 'Hosting & Maintenance',
                'description'    => 'Layanan hosting cepat dan maintenance rutin agar website Anda selalu optimal.',
                'starting_price' => 500000,
                'duration'       => 'Per bulan',
                'features' => [
                    ['feature_name' => 'Server SSD',     'feature_description' => 'Performa loading cepat'],
                    ['feature_name' => 'SSL Certificate','feature_description' => 'Keamanan HTTPS gratis'],
                    ['feature_name' => 'Backup Harian',  'feature_description' => 'Backup data otomatis'],
                    ['feature_name' => 'Monitoring 24/7','feature_description' => 'Pemantauan uptime'],
                ],
                'pricing' => [
                    ['package_name' => 'Basic',        'price' => 500000,  'notes' => 'Per bulan, 10GB storage'],
                    ['package_name' => 'Professional', 'price' => 1000000, 'notes' => 'Per bulan, unlimited bandwidth'],
                ],
            ],
            [
                'name'           => 'Digital Marketing',
                'description'    => 'Layanan pemasaran digital untuk meningkatkan visibilitas dan penjualan bisnis Anda.',
                'starting_price' => 2000000,
                'duration'       => 'Per bulan',
                'features' => [
                    ['feature_name' => 'Social Media Management', 'feature_description' => 'Kelola akun sosial media'],
                    ['feature_name' => 'Google Ads',              'feature_description' => 'Iklan berbayar Google'],
                    ['feature_name' => 'Content Creation',        'feature_description' => 'Pembuatan konten berkualitas'],
                    ['feature_name' => 'Monthly Report',          'feature_description' => 'Laporan performa bulanan'],
                ],
                'pricing' => [
                    ['package_name' => 'Starter',      'price' => 2000000, 'notes' => 'Per bulan, 2 platform sosmed'],
                    ['package_name' => 'Professional', 'price' => 5000000, 'notes' => 'Per bulan, full service'],
                ],
            ],
        ];

        foreach ($services as $serviceData) {
            $features = $serviceData['features'];
            $pricing  = $serviceData['pricing'];
            unset($serviceData['features'], $serviceData['pricing']);

            $service = Service::create($serviceData);

            foreach ($features as $feature) {
                ServiceFeature::create(['service_id' => $service->id, ...$feature]);
            }

            foreach ($pricing as $price) {
                Pricing::create(['service_id' => $service->id, ...$price]);
            }
        }
    }
}
