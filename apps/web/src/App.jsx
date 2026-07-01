import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Link, useLocation, Navigate, useNavigate } from 'react-router-dom';
import { Coffee, MessageSquare, LayoutDashboard, Settings, Users, Package, LogOut, Search, Bell, ChefHat } from 'lucide-react';
import api from './lib/axios';
import Login from './pages/Login';
import MenuManagement from './pages/pos/MenuManagement';
import Cashier from './pages/pos/Cashier';
import StaffManagement from './pages/pos/StaffManagement';
import KitchenDisplay from './pages/pos/KitchenDisplay';
import DailyStock from './pages/pos/DailyStock';

// Komponen Sidebar
const Sidebar = ({ activePath }) => {
  const menuItems = [
    { path: '/', name: 'Dashboard', icon: <LayoutDashboard size={20} /> },
    { path: '/pos/menu', name: 'Menu & Resep', icon: <Coffee size={20} /> },
    { path: '/pos/orders', name: 'Kasir / Transaksi', icon: <Package size={20} /> },
    { path: '/pos/kitchen', name: 'Layar Dapur', icon: <ChefHat size={20} /> },
    { path: '/pos/staff', name: 'Karyawan', icon: <Users size={20} /> },
    { path: '/pos/stock', name: 'Input Stok Harian', icon: <Package size={20} /> },
    { path: '/chatbot', name: 'AI Chatbot', icon: <MessageSquare size={20} /> },
    { path: '/settings', name: 'Pengaturan', icon: <Settings size={20} /> },
  ];

  return (
    <aside className="w-64 bg-primary text-cream flex flex-col h-screen fixed left-0 top-0">
      <div className="px-6 py-8 flex items-center gap-3">
        <div className="w-10 h-10 bg-cream rounded-lg flex items-center justify-center text-primary shadow-sm">
          <Coffee size={24} />
        </div>
        <h2 className="text-xl font-bold tracking-wide">CoffeeOS</h2>
      </div>

      <nav className="flex flex-col gap-2 px-4 flex-1">
        {menuItems.map(item => {
          const isActive = activePath === item.path || (item.path !== '/' && activePath.startsWith(item.path));
          return (
            <Link key={item.path} to={item.path} 
              className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors font-medium
                ${isActive ? 'bg-secondary text-cream shadow-md' : 'text-tertiary hover:bg-secondary/50 hover:text-white'}`}
            >
              {item.icon}
              {item.name}
            </Link>
          );
        })}
      </nav>

      <div className="p-4 border-t border-secondary/50">
        <button 
          onClick={() => {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            window.location.href = '/login';
          }}
          className="flex items-center gap-3 px-4 py-3 w-full text-tertiary hover:text-cream transition-colors text-sm font-medium">
          <LogOut size={18} /> Keluar
        </button>
      </div>
    </aside>
  );
};

// Komponen Header
const Header = ({ title }) => {
  return (
    <header className="h-20 bg-white/80 backdrop-blur-md border-b border-tertiary/20 flex items-center justify-between px-8 sticky top-0 z-10 shadow-sm">
      <h1 className="text-xl font-bold text-primary">{title}</h1>
      
      <div className="flex items-center gap-6">
        <div className="relative">
          <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-tertiary" />
          <input type="text" placeholder="Cari menu, pesanan..." 
            className="pl-10 pr-4 py-2 bg-cream/50 border border-transparent focus:bg-white focus:border-secondary rounded-full w-64 text-sm text-primary placeholder-tertiary focus:outline-none focus:ring-2 focus:ring-secondary/20 transition-all" />
        </div>
        
        <button className="text-tertiary hover:text-secondary transition-colors relative">
          <Bell size={22} />
          <span className="absolute -top-1 -right-1 w-2.5 h-2.5 bg-red-500 rounded-full border-2 border-white"></span>
        </button>
        
        <div className="w-10 h-10 rounded-full bg-cream border-2 border-primary flex items-center justify-center font-bold text-secondary shadow-sm">
          A
        </div>
      </div>
    </header>
  );
};

// Halaman Dashboard Home
const DashboardHome = () => {
  const [data, setData] = useState({ revenue: 0, active_orders: 0, chatbot_interactions: 0 });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await api.get('/dashboard');
        setData(response.data);
      } catch (error) {
        console.error("Gagal mengambil data dashboard", error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div className="bg-white rounded-xl p-6 shadow-sm border border-tertiary/20 flex flex-col hover:shadow-md transition-shadow">
        <div className="text-tertiary text-sm font-semibold mb-2">Total Pendapatan</div>
        <div className="text-3xl font-bold text-primary">
          {loading ? '...' : `Rp ${data.revenue.toLocaleString('id-ID')}`}
        </div>
        <div className="text-green-600 text-sm font-medium mt-3 bg-green-50 border border-green-100 px-2 py-1 rounded w-max">Data Hari Ini</div>
      </div>
      
      <div className="bg-white rounded-xl p-6 shadow-sm border border-tertiary/20 flex flex-col hover:shadow-md transition-shadow">
        <div className="text-tertiary text-sm font-semibold mb-2">Pesanan Aktif</div>
        <div className="text-3xl font-bold text-primary">{loading ? '...' : data.active_orders}</div>
        <div className="text-secondary text-sm font-medium mt-3 bg-cream px-2 py-1 rounded border border-secondary/20 w-max">Menunggu diproses</div>
      </div>

      <div className="bg-white rounded-xl p-6 shadow-sm border border-secondary flex flex-col hover:shadow-md transition-shadow relative overflow-hidden">
        <div className="absolute right-0 top-0 w-24 h-24 bg-cream rounded-bl-full -mr-4 -mt-4 -z-0"></div>
        <div className="text-secondary text-sm font-semibold mb-2 relative z-10">AI Chatbot Interaksi</div>
        <div className="text-3xl font-bold text-primary relative z-10">{loading ? '...' : data.chatbot_interactions}</div>
        <div className="text-tertiary text-sm mt-3 relative z-10 font-medium">Aktivitas Chatbot Global</div>
      </div>
    </div>
  );
};

// Halaman Chatbot Settings
const ChatbotSettings = () => {
  const [settings, setSettings] = useState({ telegram_bot_token: '', system_prompt: '' });
  const [kbs, setKbs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  
  // State form Knowledge Base baru
  const [newKb, setNewKb] = useState({ title: '', category: 'faq', content: '' });

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [resSettings, resKb] = await Promise.all([
        api.get('/chatbot/settings'),
        api.get('/chatbot/kb')
      ]);
      setSettings({
        telegram_bot_token: resSettings.data.telegram_bot_token || '',
        system_prompt: resSettings.data.system_prompt || '',
      });
      setKbs(resKb.data);
    } catch (error) {
      console.error("Failed to load chatbot data", error);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveSettings = async () => {
    try {
      setSaving(true);
      await api.put('/chatbot/settings', settings);
      alert('Pengaturan Chatbot berhasil disimpan!');
    } catch (error) {
      console.error(error);
      alert('Gagal menyimpan pengaturan');
    } finally {
      setSaving(false);
    }
  };

  const handleAddKb = async (e) => {
    e.preventDefault();
    try {
      await api.post('/chatbot/kb', newKb);
      setNewKb({ title: '', category: 'faq', content: '' });
      fetchData(); // reload KBs
      alert('Knowledge Base berhasil ditambahkan!');
    } catch (error) {
      console.error(error);
      alert('Gagal menambah Knowledge Base');
    }
  };

  if (loading) return <div className="text-center py-10">Memuat data...</div>;

  return (
    <div className="space-y-8">
      {/* Kartu Konfigurasi Utama */}
      <div className="bg-white rounded-xl shadow-sm border border-secondary/20 p-6">
        <h2 className="text-xl font-bold text-primary mb-4 flex items-center gap-2">
          <Settings size={20} /> Konfigurasi Utama AI
        </h2>
        
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-semibold text-tertiary mb-1">Telegram Bot Token</label>
            <input 
              type="text" 
              value={settings.telegram_bot_token}
              onChange={(e) => setSettings({...settings, telegram_bot_token: e.target.value})}
              placeholder="123456789:ABCdefGHIjklMNOpqrSTUvwxYZ"
              className="w-full px-4 py-2 border border-secondary/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-tertiary"
            />
            <p className="text-xs text-secondary mt-1">Dapatkan token dari BotFather di Telegram.</p>
          </div>

          <div>
            <label className="block text-sm font-semibold text-tertiary mb-1">System Prompt (Kepribadian AI)</label>
            <textarea 
              value={settings.system_prompt}
              onChange={(e) => setSettings({...settings, system_prompt: e.target.value})}
              rows="4"
              placeholder="Kamu adalah asisten kasir virtual yang ramah..."
              className="w-full px-4 py-2 border border-secondary/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-tertiary"
            ></textarea>
            <p className="text-xs text-secondary mt-1">Instruksi ini akan mendikte cara AI merespon pelanggan.</p>
          </div>

          <button 
            onClick={handleSaveSettings}
            disabled={saving}
            className="bg-primary text-white px-6 py-2 rounded-lg font-medium hover:bg-tertiary transition-colors"
          >
            {saving ? 'Menyimpan...' : 'Simpan Konfigurasi'}
          </button>
        </div>
      </div>

      {/* Kartu Knowledge Base */}
      <div className="bg-white rounded-xl shadow-sm border border-secondary/20 p-6">
        <h2 className="text-xl font-bold text-primary mb-4 flex items-center gap-2">
          <MessageSquare size={20} /> Knowledge Base (Data Latih)
        </h2>
        
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Form Tambah */}
          <div className="lg:col-span-1 bg-cream/30 p-4 rounded-lg border border-secondary/20">
            <h3 className="font-semibold text-tertiary mb-3">Tambah Informasi Baru</h3>
            <form onSubmit={handleAddKb} className="space-y-3">
              <div>
                <label className="block text-xs font-medium text-secondary mb-1">Kategori</label>
                <select 
                  value={newKb.category}
                  onChange={(e) => setNewKb({...newKb, category: e.target.value})}
                  className="w-full px-3 py-2 border border-secondary/30 rounded-lg text-sm"
                >
                  <option value="faq">FAQ (Tanya Jawab)</option>
                  <option value="service">Layanan</option>
                  <option value="policy">Kebijakan</option>
                  <option value="menu">Info Menu</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-secondary mb-1">Judul / Pertanyaan</label>
                <input 
                  type="text" 
                  required
                  value={newKb.title}
                  onChange={(e) => setNewKb({...newKb, title: e.target.value})}
                  className="w-full px-3 py-2 border border-secondary/30 rounded-lg text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-secondary mb-1">Isi / Jawaban</label>
                <textarea 
                  required
                  rows="3"
                  value={newKb.content}
                  onChange={(e) => setNewKb({...newKb, content: e.target.value})}
                  className="w-full px-3 py-2 border border-secondary/30 rounded-lg text-sm"
                ></textarea>
              </div>
              <button type="submit" className="w-full bg-tertiary text-white py-2 rounded-lg text-sm font-medium hover:bg-primary transition-colors">
                Tambahkan Data
              </button>
            </form>
          </div>

          {/* List Knowledge Base */}
          <div className="lg:col-span-2">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-cream/50 text-tertiary">
                  <tr>
                    <th className="px-4 py-3 font-semibold rounded-tl-lg">Kategori</th>
                    <th className="px-4 py-3 font-semibold">Judul</th>
                    <th className="px-4 py-3 font-semibold rounded-tr-lg">Kutipan Isi</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-secondary/20">
                  {kbs.length === 0 ? (
                    <tr>
                      <td colSpan="3" className="px-4 py-6 text-center text-secondary">Belum ada data latih.</td>
                    </tr>
                  ) : (
                    kbs.map(kb => (
                      <tr key={kb.id} className="hover:bg-cream/10">
                        <td className="px-4 py-3"><span className="bg-secondary/10 text-tertiary px-2 py-1 rounded text-xs uppercase font-medium">{kb.category}</span></td>
                        <td className="px-4 py-3 font-medium text-primary">{kb.title}</td>
                        <td className="px-4 py-3 text-secondary truncate max-w-xs">{kb.content}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

// Protected Route Component
const ProtectedRoute = ({ children }) => {
  const token = localStorage.getItem('token');
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return children;
};

// Layout Utama
const AppLayout = () => {
  const location = useLocation();
  const getPageTitle = (path) => {
    if (path === '/') return 'Ringkasan Hari Ini';
    if (path.startsWith('/chatbot')) return 'Manajemen AI Chatbot';
    if (path.startsWith('/pos/menu')) return 'Manajemen Menu';
    return 'Dashboard';
  };

  return (
    <div className="flex min-h-screen bg-cream/50">
      <Sidebar activePath={location.pathname} />
      <div className="flex flex-col flex-1 ml-64">
        <Header title={getPageTitle(location.pathname)} />
        <main className="p-8">
          <Routes>
            <Route path="/" element={<DashboardHome />} />
            <Route path="/chatbot" element={<ChatbotSettings />} />
            <Route path="/pos/menu" element={<MenuManagement />} />
            <Route path="/pos/orders" element={<Cashier />} />
            <Route path="/pos/kitchen" element={<KitchenDisplay />} />
            <Route path="/pos/staff" element={<StaffManagement />} />
            <Route path="/pos/stock" element={<DailyStock />} />
            
            {/* Customer Routes dipindah ke apps/customer-web */}

            <Route path="*" element={
              <div className="text-center py-20 text-tertiary">
                <Coffee size={64} className="opacity-30 mx-auto mb-4" />
                <h3 className="text-xl font-medium">Halaman Sedang Dibangun</h3>
              </div>
            } />
          </Routes>
        </main>
      </div>
    </div>
  );
};

export default function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="*" element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        } />
      </Routes>
    </Router>
  );
}
