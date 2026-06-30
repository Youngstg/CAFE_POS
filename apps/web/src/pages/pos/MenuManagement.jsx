import React, { useState, useEffect } from 'react';
import { Plus, Edit2, Trash2, Coffee, Tag, Search, Filter } from 'lucide-react';
import api from '../../lib/axios';

const MenuManagement = () => {
  const [menus, setMenus] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // Filter state
  const [activeCategory, setActiveCategory] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    category_id: '',
    base_price: '',
    is_active: true
  });

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [resMenus, resCats] = await Promise.all([
        api.get('/menus'),
        api.get('/categories')
      ]);
      setMenus(resMenus.data);
      setCategories(resCats.data);
      if (resCats.data.length > 0 && !formData.category_id) {
        setFormData(prev => ({ ...prev, category_id: resCats.data[0].id }));
      }
    } catch (error) {
      console.error("Gagal mengambil data menu", error);
    } finally {
      setLoading(false);
    }
  };

  const openModal = (menu = null) => {
    if (menu) {
      setEditingId(menu.id);
      setFormData({
        name: menu.name,
        category_id: menu.category_id,
        base_price: menu.base_price,
        is_active: menu.is_active
      });
    } else {
      setEditingId(null);
      setFormData({
        name: '',
        category_id: categories.length > 0 ? categories[0].id : '',
        base_price: '',
        is_active: true
      });
    }
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingId) {
        await api.put(`/menus/${editingId}`, formData);
      } else {
        await api.post('/menus', formData);
      }
      closeModal();
      fetchData();
    } catch (error) {
      console.error("Gagal menyimpan menu", error);
      alert('Gagal menyimpan menu');
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Apakah Anda yakin ingin menghapus menu ini?')) {
      try {
        await api.delete(`/menus/${id}`);
        fetchData();
      } catch (error) {
        console.error("Gagal menghapus menu", error);
      }
    }
  };

  // Filtering
  const filteredMenus = menus.filter(menu => {
    const matchCat = activeCategory === 'all' || menu.category_id.toString() === activeCategory.toString();
    const matchSearch = menu.name.toLowerCase().includes(searchQuery.toLowerCase());
    return matchCat && matchSearch;
  });

  if (loading) return <div className="text-center py-10 text-tertiary">Memuat Data Menu...</div>;

  return (
    <div className="flex flex-col md:flex-row gap-6">
      
      {/* Sidebar Kategori */}
      <div className="w-full md:w-64 shrink-0 space-y-4">
        <div className="bg-white rounded-xl shadow-sm border border-secondary/20 p-4">
          <h3 className="font-bold text-primary mb-3 flex items-center gap-2">
            <Filter size={18} /> Kategori Menu
          </h3>
          <div className="flex flex-col gap-1">
            <button 
              onClick={() => setActiveCategory('all')}
              className={`text-left px-4 py-2 rounded-lg text-sm font-medium transition-colors ${activeCategory === 'all' ? 'bg-secondary text-white' : 'text-tertiary hover:bg-cream'}`}
            >
              Semua Menu
            </button>
            {categories.map(cat => (
              <button 
                key={cat.id}
                onClick={() => setActiveCategory(cat.id.toString())}
                className={`text-left px-4 py-2 rounded-lg text-sm font-medium transition-colors ${activeCategory === cat.id.toString() ? 'bg-secondary text-white' : 'text-tertiary hover:bg-cream'}`}
              >
                {cat.name}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Konten Utama Menu */}
      <div className="flex-1 space-y-4">
        
        {/* Header Konten */}
        <div className="flex flex-col sm:flex-row justify-between items-center gap-4 bg-white p-4 rounded-xl shadow-sm border border-secondary/20">
          <div className="relative w-full sm:w-72">
            <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-tertiary" />
            <input 
              type="text" 
              placeholder="Cari nama menu..." 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-cream/50 border border-secondary/30 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-secondary/50"
            />
          </div>
          <button 
            onClick={() => openModal()}
            className="w-full sm:w-auto flex items-center justify-center gap-2 bg-primary text-cream px-6 py-2 rounded-lg font-medium hover:bg-tertiary transition-colors"
          >
            <Plus size={18} /> Tambah Menu Baru
          </button>
        </div>

        {/* Grid Menu */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredMenus.length === 0 ? (
            <div className="col-span-full py-12 text-center text-secondary bg-white rounded-xl border border-secondary/20 border-dashed">
              Tidak ada menu yang ditemukan.
            </div>
          ) : (
            filteredMenus.map(menu => (
              <div key={menu.id} className={`bg-white rounded-xl shadow-sm border p-5 flex flex-col transition-all hover:shadow-md ${!menu.is_active ? 'border-red-200 bg-red-50/30' : 'border-secondary/20'}`}>
                <div className="flex justify-between items-start mb-4">
                  <div className="p-2 bg-cream rounded-lg text-secondary">
                    <Coffee size={24} />
                  </div>
                  <div className="flex gap-2">
                    <button onClick={() => openModal(menu)} className="text-tertiary hover:text-primary transition-colors">
                      <Edit2 size={16} />
                    </button>
                    <button onClick={() => handleDelete(menu.id)} className="text-red-400 hover:text-red-600 transition-colors">
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>
                
                <h3 className="font-bold text-primary text-lg truncate mb-1">{menu.name}</h3>
                
                <div className="flex items-center justify-between mt-auto pt-4">
                  <div className="flex items-center gap-1.5 text-xs font-medium text-tertiary bg-cream px-2 py-1 rounded">
                    <Tag size={12} /> {menu.category?.name || 'Uncategorized'}
                  </div>
                  <div className="font-bold text-primary">
                    Rp {Number(menu.base_price).toLocaleString('id-ID')}
                  </div>
                </div>
                {!menu.is_active && (
                  <div className="text-xs text-red-500 font-semibold mt-3 text-center">Menu Tidak Aktif</div>
                )}
              </div>
            ))
          )}
        </div>
      </div>

      {/* Modal Form */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-primary/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="px-6 py-4 border-b border-secondary/20 flex justify-between items-center bg-cream/30">
              <h3 className="text-lg font-bold text-primary">{editingId ? 'Edit Menu' : 'Tambah Menu Baru'}</h3>
              <button onClick={closeModal} className="text-tertiary hover:text-primary">&times;</button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-semibold text-tertiary mb-1">Nama Menu</label>
                <input 
                  type="text" 
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({...formData, name: e.target.value})}
                  className="w-full px-4 py-2 border border-secondary/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-secondary/50"
                  placeholder="Misal: Matcha Latte"
                />
              </div>
              
              <div>
                <label className="block text-sm font-semibold text-tertiary mb-1">Kategori</label>
                <select 
                  required
                  value={formData.category_id}
                  onChange={(e) => setFormData({...formData, category_id: e.target.value})}
                  className="w-full px-4 py-2 border border-secondary/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-secondary/50"
                >
                  <option value="" disabled>Pilih Kategori</option>
                  {categories.map(cat => (
                    <option key={cat.id} value={cat.id}>{cat.name}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-semibold text-tertiary mb-1">Harga Dasar (Rp)</label>
                <input 
                  type="number" 
                  required
                  min="0"
                  value={formData.base_price}
                  onChange={(e) => setFormData({...formData, base_price: e.target.value})}
                  className="w-full px-4 py-2 border border-secondary/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-secondary/50"
                  placeholder="Misal: 25000"
                />
              </div>

              <div className="flex items-center gap-2 pt-2">
                <input 
                  type="checkbox" 
                  id="isActive"
                  checked={formData.is_active}
                  onChange={(e) => setFormData({...formData, is_active: e.target.checked})}
                  className="w-4 h-4 text-primary rounded border-secondary/30 focus:ring-primary"
                />
                <label htmlFor="isActive" className="text-sm font-medium text-tertiary">Menu Aktif (Ditampilkan ke Kasir)</label>
              </div>

              <div className="pt-4 flex gap-3">
                <button type="button" onClick={closeModal} className="flex-1 px-4 py-2 border border-secondary/30 text-tertiary rounded-lg font-medium hover:bg-cream transition-colors">Batal</button>
                <button type="submit" className="flex-1 px-4 py-2 bg-primary text-cream rounded-lg font-medium hover:bg-tertiary transition-colors">Simpan Data</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default MenuManagement;
