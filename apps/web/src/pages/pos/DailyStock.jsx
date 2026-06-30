import React, { useState, useEffect } from 'react';
import { Package, Save, AlertTriangle, CheckCircle } from 'lucide-react';
import api from '../../lib/axios';

const DailyStock = () => {
  const [ingredients, setIngredients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');

  useEffect(() => {
    fetchIngredients();
  }, []);

  const fetchIngredients = async () => {
    try {
      setLoading(true);
      const response = await api.get('/inventory/ingredients');
      // Set initial state matching current DB stock
      const initialStockData = response.data.map(item => ({
        ...item,
        actual_stock: item.stock_qty // default ke angka sistem
      }));
      setIngredients(initialStockData);
    } catch (error) {
      console.error("Gagal mengambil data bahan baku", error);
    } finally {
      setLoading(false);
    }
  };

  const handleStockChange = (id, value) => {
    setIngredients(prev => 
      prev.map(item => item.id === id ? { ...item, actual_stock: parseFloat(value) || 0 } : item)
    );
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      setIsSaving(true);
      setSuccessMsg('');
      
      const payload = {
        stocks: ingredients.map(item => ({
          id: item.id,
          actual_stock: item.actual_stock
        }))
      };

      await api.post('/inventory/daily-input', payload);
      
      setSuccessMsg('Laporan Stok Fisik berhasil disimpan!');
      setTimeout(() => setSuccessMsg(''), 3000);
      
      // Update data terbaru dari server
      fetchIngredients();

    } catch (error) {
      console.error("Gagal menyimpan stok", error);
      alert('Gagal menyimpan laporan stok.');
    } finally {
      setIsSaving(false);
    }
  };

  if (loading) return <div className="text-center py-10 text-tertiary">Memuat Data Inventori...</div>;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-6 rounded-2xl shadow-sm border border-secondary/20">
        <div>
          <h1 className="text-2xl font-bold text-primary flex items-center gap-2">
            <Package size={28} /> Input Stok Harian (Stock Opname)
          </h1>
          <p className="text-tertiary mt-1">Masukkan hasil penghitungan fisik bahan baku riil di gudang/kulkas saat ini.</p>
        </div>
      </div>

      {successMsg && (
        <div className="bg-emerald-100 border border-emerald-400 text-emerald-700 px-4 py-3 rounded-xl flex items-center gap-2">
          <CheckCircle size={20} /> {successMsg}
        </div>
      )}

      <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-sm border border-secondary/20 overflow-hidden">
        
        <div className="p-4 bg-cream/30 border-b border-secondary/20 flex items-center gap-2 text-sm font-semibold text-amber-600">
          <AlertTriangle size={16} /> Data yang Anda simpan akan menimpa/me-*replace* stok lama di sistem. Pastikan hitungan fisik sudah benar!
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead className="bg-cream/30 text-tertiary text-sm border-b border-secondary/20">
              <tr>
                <th className="px-6 py-4 font-semibold">Nama Bahan Baku</th>
                <th className="px-6 py-4 font-semibold text-center">Stok Di Sistem (Lama)</th>
                <th className="px-6 py-4 font-semibold text-right w-1/3">Hitungan Fisik Riil (Baru)</th>
              </tr>
            </thead>
            <tbody>
              {ingredients.length === 0 ? (
                <tr>
                  <td colSpan="3" className="px-6 py-8 text-center text-tertiary">Belum ada bahan baku.</td>
                </tr>
              ) : (
                ingredients.map((item) => (
                  <tr key={item.id} className="border-b border-secondary/10 hover:bg-cream/20 transition-colors">
                    <td className="px-6 py-4">
                      <div className="font-bold text-primary">{item.name}</div>
                    </td>
                    <td className="px-6 py-4 text-center">
                      <span className="text-tertiary font-medium">{item.stock_qty} {item.unit}</span>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <input 
                          type="number" 
                          min="0"
                          step="0.01"
                          required
                          value={item.actual_stock}
                          onChange={(e) => handleStockChange(item.id, e.target.value)}
                          className="w-32 px-3 py-2 border border-secondary/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-secondary/50 text-right font-bold text-primary"
                        />
                        <span className="text-sm font-medium text-tertiary w-12 text-left">{item.unit}</span>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="p-6 bg-gray-50 flex justify-end">
          <button 
            type="submit" 
            disabled={isSaving || ingredients.length === 0}
            className="flex items-center gap-2 bg-primary text-cream px-8 py-3 rounded-xl font-bold hover:bg-tertiary transition-colors shadow-md disabled:opacity-50"
          >
            <Save size={20} /> {isSaving ? 'Menyimpan...' : 'Simpan Laporan Stok'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default DailyStock;
