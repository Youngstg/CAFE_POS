import React, { useState, useEffect } from 'react';
import { Users, Plus, Trash2, MapPin, Search } from 'lucide-react';
import api from '../../lib/axios';

const StaffManagement = () => {
  const [staffList, setStaffList] = useState([]);
  const [outlets, setOutlets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    outlet_id: ''
  });

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [resStaff, resOutlets] = await Promise.all([
        api.get('/staff'),
        api.get('/outlets')
      ]);
      setStaffList(resStaff.data);
      setOutlets(resOutlets.data);
      if (resOutlets.data.length > 0) {
        setFormData(prev => ({ ...prev, outlet_id: resOutlets.data[0].id }));
      }
    } catch (error) {
      console.error("Gagal mengambil data staff", error);
    } finally {
      setLoading(false);
    }
  };

  const openModal = () => {
    setFormData({
      name: '',
      email: '',
      password: '',
      outlet_id: outlets.length > 0 ? outlets[0].id : ''
    });
    setIsModalOpen(true);
  };

  const closeModal = () => setIsModalOpen(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await api.post('/staff', formData);
      closeModal();
      fetchData();
    } catch (error) {
      console.error("Gagal menyimpan staff", error);
      alert('Gagal menyimpan kasir. Email mungkin sudah terdaftar.');
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Hapus akun kasir ini?')) {
      try {
        await api.delete(`/staff/${id}`);
        fetchData();
      } catch (error) {
        console.error("Gagal menghapus staff", error);
      }
    }
  };

  const filteredStaff = staffList.filter(s => 
    s.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
    s.email.toLowerCase().includes(searchQuery.toLowerCase())
  );

  if (loading) return <div className="text-center py-10 text-tertiary">Memuat Data Karyawan...</div>;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-6 rounded-2xl shadow-sm border border-secondary/20">
        <div>
          <h1 className="text-2xl font-bold text-primary flex items-center gap-2">
            <Users size={28} /> Manajemen Karyawan
          </h1>
          <p className="text-tertiary mt-1">Kelola akun kasir untuk setiap cabang Anda.</p>
        </div>
        
        <button 
          onClick={openModal}
          className="flex items-center gap-2 bg-primary text-cream px-6 py-2.5 rounded-lg font-bold hover:bg-tertiary transition-colors shadow-md"
        >
          <Plus size={18} /> Tambah Kasir
        </button>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-secondary/20 overflow-hidden">
        <div className="p-4 border-b border-secondary/20 flex justify-end">
          <div className="relative w-full sm:w-72">
            <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-tertiary" />
            <input 
              type="text" 
              placeholder="Cari nama atau email..." 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-cream/50 border border-secondary/30 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-secondary/50"
            />
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead className="bg-cream/30 text-tertiary text-sm border-b border-secondary/20">
              <tr>
                <th className="px-6 py-4 font-semibold">Nama Kasir</th>
                <th className="px-6 py-4 font-semibold">Email Login</th>
                <th className="px-6 py-4 font-semibold">Cabang (Outlet)</th>
                <th className="px-6 py-4 font-semibold text-right">Aksi</th>
              </tr>
            </thead>
            <tbody>
              {filteredStaff.length === 0 ? (
                <tr>
                  <td colSpan="4" className="px-6 py-8 text-center text-tertiary">Belum ada data kasir.</td>
                </tr>
              ) : (
                filteredStaff.map((staff) => (
                  <tr key={staff.id} className="border-b border-secondary/10 hover:bg-cream/20 transition-colors">
                    <td className="px-6 py-4">
                      <div className="font-bold text-primary">{staff.name}</div>
                      <div className="text-xs text-tertiary mt-0.5">Role: {staff.role}</div>
                    </td>
                    <td className="px-6 py-4 text-tertiary">{staff.email}</td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-1.5 text-sm font-medium text-secondary bg-cream px-3 py-1 rounded-full w-max">
                        <MapPin size={14} /> {staff.outlet?.name || 'Unknown'}
                      </div>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <button 
                        onClick={() => handleDelete(staff.id)} 
                        className="text-red-400 hover:text-red-600 transition-colors p-2 bg-red-50 rounded-lg hover:bg-red-100"
                        title="Hapus Kasir"
                      >
                        <Trash2 size={18} />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal Form */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-primary/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="px-6 py-4 border-b border-secondary/20 flex justify-between items-center bg-cream/30">
              <h3 className="text-lg font-bold text-primary">Tambah Kasir Baru</h3>
              <button onClick={closeModal} className="text-tertiary hover:text-primary">&times;</button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-semibold text-tertiary mb-1">Nama Lengkap</label>
                <input 
                  type="text" 
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({...formData, name: e.target.value})}
                  className="w-full px-4 py-2 border border-secondary/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-secondary/50"
                  placeholder="Budi Kasir"
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-tertiary mb-1">Penempatan Cabang</label>
                <select 
                  required
                  value={formData.outlet_id}
                  onChange={(e) => setFormData({...formData, outlet_id: e.target.value})}
                  className="w-full px-4 py-2 border border-secondary/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-secondary/50"
                >
                  {outlets.map(out => (
                    <option key={out.id} value={out.id}>{out.name}</option>
                  ))}
                </select>
              </div>
              
              <div>
                <label className="block text-sm font-semibold text-tertiary mb-1">Email Login</label>
                <input 
                  type="email" 
                  required
                  value={formData.email}
                  onChange={(e) => setFormData({...formData, email: e.target.value})}
                  className="w-full px-4 py-2 border border-secondary/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-secondary/50"
                  placeholder="budi@kasir.com"
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-tertiary mb-1">Password</label>
                <input 
                  type="password" 
                  required
                  minLength="6"
                  value={formData.password}
                  onChange={(e) => setFormData({...formData, password: e.target.value})}
                  className="w-full px-4 py-2 border border-secondary/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-secondary/50"
                  placeholder="Minimal 6 karakter"
                />
              </div>

              <div className="pt-4 flex gap-3">
                <button type="button" onClick={closeModal} className="flex-1 px-4 py-2 border border-secondary/30 text-tertiary rounded-lg font-medium hover:bg-cream transition-colors">Batal</button>
                <button type="submit" className="flex-1 px-4 py-2 bg-primary text-cream rounded-lg font-medium hover:bg-tertiary transition-colors">Simpan Kasir</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default StaffManagement;
