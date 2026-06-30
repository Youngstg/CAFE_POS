import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ShoppingCart, Plus, Minus, Coffee, Mail, CheckCircle } from 'lucide-react';
import axios from 'axios';

const EMenu = () => {
  const { tenantId, outletId, tableNumber } = useParams();
  const navigate = useNavigate();
  
  const [menus, setMenus] = useState([]);
  const [categories, setCategories] = useState([]);
  const [activeCategory, setActiveCategory] = useState('Semua');
  
  const [cart, setCart] = useState([]);
  const [isCartOpen, setIsCartOpen] = useState(false);
  
  const [customerEmail, setCustomerEmail] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    fetchMenus();
  }, [tenantId, outletId]);

  const fetchMenus = async () => {
    try {
      const response = await axios.get(`http://127.0.0.1:8000/api/public/menu/${tenantId}/${outletId}`);
      setMenus(response.data);
      
      const cats = ['Semua', ...new Set(response.data.map(m => m.category?.name || 'Uncategorized'))];
      setCategories(cats);
    } catch (error) {
      console.error('Error fetching menu', error);
      alert('Gagal memuat menu. Pastikan QR valid.');
    }
  };

  const addToCart = (menu) => {
    setCart(prev => {
      const existing = prev.find(item => item.id === menu.id);
      if (existing) {
        return prev.map(item => item.id === menu.id ? { ...item, qty: item.qty + 1 } : item);
      }
      return [...prev, { ...menu, qty: 1 }];
    });
  };

  const removeFromCart = (menuId) => {
    setCart(prev => {
      const existing = prev.find(item => item.id === menuId);
      if (existing.qty === 1) {
        return prev.filter(item => item.id !== menuId);
      }
      return prev.map(item => item.id === menuId ? { ...item, qty: item.qty - 1 } : item);
    });
  };

  const totalItems = cart.reduce((sum, item) => sum + item.qty, 0);
  const totalPrice = cart.reduce((sum, item) => sum + (item.base_price * item.qty), 0);

  const filteredMenus = activeCategory === 'Semua' 
    ? menus 
    : menus.filter(m => (m.category?.name || 'Uncategorized') === activeCategory);

  const handleCheckout = async (e) => {
    e.preventDefault();
    if (!customerEmail) {
      alert('Silakan masukkan email Anda untuk pengiriman struk.');
      return;
    }

    setIsSubmitting(true);
    try {
      const payload = {
        tenant_id: tenantId,
        outlet_id: outletId,
        table_number: tableNumber,
        customer_email: customerEmail,
        items: cart.map(item => ({
          menu_id: item.id,
          qty: item.qty,
          subtotal: item.qty * item.base_price
        }))
      };

      const response = await axios.post('http://127.0.0.1:8000/api/public/checkout', payload);
      
      // Arahkan ke halaman Pembayaran QR
      navigate(`/checkout/${response.data.order.id}`);
      
    } catch (error) {
      console.error('Checkout error', error);
      alert('Gagal membuat pesanan: ' + (error.response?.data?.message || error.message));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800 pb-24 font-sans">
      {/* Header */}
      <header className="bg-white shadow-sm sticky top-0 z-10 p-4 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-primary flex items-center gap-2">
            <Coffee className="text-amber-600" />
            CoffeeOS E-Menu
          </h1>
          <p className="text-sm text-slate-500">Meja: {tableNumber}</p>
        </div>
      </header>

      {/* Categories */}
      <div className="overflow-x-auto p-4 flex gap-2 no-scrollbar">
        {categories.map(cat => (
          <button
            key={cat}
            onClick={() => setActiveCategory(cat)}
            className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${
              activeCategory === cat 
                ? 'bg-amber-500 text-white shadow-md' 
                : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-100'
            }`}
          >
            {cat}
          </button>
        ))}
      </div>

      {/* Menu Grid */}
      <div className="p-4 grid grid-cols-2 gap-4">
        {filteredMenus.map(menu => {
          const cartItem = cart.find(c => c.id === menu.id);
          return (
            <div key={menu.id} className="bg-white rounded-xl shadow-sm overflow-hidden flex flex-col border border-slate-100">
              <div className="h-32 bg-slate-100 flex items-center justify-center">
                <Coffee size={40} className="text-slate-300" />
              </div>
              <div className="p-3 flex-1 flex flex-col">
                <h3 className="font-semibold text-slate-800 line-clamp-1">{menu.name}</h3>
                <p className="text-amber-600 font-bold mt-1 text-sm">Rp {Number(menu.base_price).toLocaleString('id-ID')}</p>
                
                <div className="mt-auto pt-3">
                  {cartItem ? (
                    <div className="flex items-center justify-between bg-amber-50 rounded-lg p-1">
                      <button onClick={() => removeFromCart(menu.id)} className="p-1.5 text-amber-600 hover:bg-amber-100 rounded-md">
                        <Minus size={16} />
                      </button>
                      <span className="font-medium text-amber-800 text-sm">{cartItem.qty}</span>
                      <button onClick={() => addToCart(menu)} className="p-1.5 text-amber-600 hover:bg-amber-100 rounded-md">
                        <Plus size={16} />
                      </button>
                    </div>
                  ) : (
                    <button 
                      onClick={() => addToCart(menu)}
                      className="w-full py-2 bg-slate-800 text-white rounded-lg text-sm font-medium hover:bg-slate-700 transition-colors"
                    >
                      Tambah
                    </button>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Floating Cart Button */}
      {totalItems > 0 && !isCartOpen && (
        <div className="fixed bottom-6 left-0 right-0 px-4 z-20">
          <button 
            onClick={() => setIsCartOpen(true)}
            className="w-full bg-primary text-white p-4 rounded-2xl shadow-xl flex items-center justify-between hover:bg-indigo-900 transition-all transform hover:scale-[1.02]"
          >
            <div className="flex items-center gap-3">
              <div className="relative">
                <ShoppingCart />
                <span className="absolute -top-2 -right-2 bg-amber-500 text-white text-xs w-5 h-5 flex items-center justify-center rounded-full font-bold">
                  {totalItems}
                </span>
              </div>
              <span className="font-medium">Lihat Keranjang</span>
            </div>
            <span className="font-bold">Rp {totalPrice.toLocaleString('id-ID')}</span>
          </button>
        </div>
      )}

      {/* Cart Modal / Drawer */}
      {isCartOpen && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
          <div className="bg-white w-full max-w-md rounded-t-3xl sm:rounded-3xl max-h-[90vh] flex flex-col shadow-2xl">
            <div className="p-5 border-b border-slate-100 flex justify-between items-center">
              <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
                <ShoppingCart className="text-amber-500" /> Keranjang Anda
              </h2>
              <button onClick={() => setIsCartOpen(false)} className="text-slate-400 hover:text-slate-600 p-2">
                Tutup
              </button>
            </div>
            
            <div className="overflow-y-auto p-5 flex-1">
              {cart.map(item => (
                <div key={item.id} className="flex justify-between items-center py-3 border-b border-slate-50 last:border-0">
                  <div>
                    <h4 className="font-medium text-slate-800">{item.name}</h4>
                    <p className="text-sm text-slate-500">Rp {Number(item.base_price).toLocaleString('id-ID')}</p>
                  </div>
                  <div className="flex items-center gap-3 bg-slate-50 rounded-lg p-1 border border-slate-100">
                    <button onClick={() => removeFromCart(item.id)} className="p-1 text-slate-600 hover:text-primary">
                      <Minus size={16} />
                    </button>
                    <span className="font-medium w-4 text-center">{item.qty}</span>
                    <button onClick={() => addToCart(item)} className="p-1 text-slate-600 hover:text-primary">
                      <Plus size={16} />
                    </button>
                  </div>
                </div>
              ))}

              <div className="mt-6 space-y-4">
                <div className="bg-amber-50 rounded-xl p-4 border border-amber-100">
                  <label className="block text-sm font-medium text-amber-900 mb-2 flex items-center gap-2">
                    <Mail size={16} />
                    Email untuk Struk Digital
                  </label>
                  <input
                    type="email"
                    required
                    value={customerEmail}
                    onChange={(e) => setCustomerEmail(e.target.value)}
                    placeholder="nama@email.com"
                    className="w-full px-4 py-2.5 rounded-lg border border-amber-200 focus:outline-none focus:ring-2 focus:ring-amber-500 bg-white"
                  />
                  <p className="text-xs text-amber-700 mt-2">*Struk dan nomor antrean akan dikirim ke email ini.</p>
                </div>
              </div>
            </div>

            <div className="p-5 border-t border-slate-100 bg-slate-50 rounded-b-3xl">
              <div className="flex justify-between items-center mb-4">
                <span className="text-slate-600 font-medium">Total Pembayaran</span>
                <span className="text-2xl font-bold text-slate-800">Rp {totalPrice.toLocaleString('id-ID')}</span>
              </div>
              <button 
                onClick={handleCheckout}
                disabled={isSubmitting || cart.length === 0}
                className="w-full bg-primary hover:bg-indigo-900 text-white py-4 rounded-xl font-bold text-lg flex justify-center items-center gap-2 transition-colors disabled:opacity-70 disabled:cursor-not-allowed"
              >
                {isSubmitting ? 'Memproses...' : (
                  <>
                    <CheckCircle size={20} />
                    Pesan Sekarang
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default EMenu;
