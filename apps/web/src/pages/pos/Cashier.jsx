import React, { useState, useEffect } from 'react';
import { Coffee, Tag, ShoppingCart, Plus, Minus, Trash2, CreditCard, Banknote } from 'lucide-react';
import api from '../../lib/axios';

const Cashier = () => {
  const [menus, setMenus] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // Kasir State
  const [activeCategory, setActiveCategory] = useState('all');
  const [cart, setCart] = useState([]);
  const [channel, setChannel] = useState('Dine-in');
  const [paymentMethod, setPaymentMethod] = useState('Cash');
  const [isProcessing, setIsProcessing] = useState(false);

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
      // Hanya tampilkan menu yang aktif
      setMenus(resMenus.data.filter(m => m.is_active));
      setCategories(resCats.data);
    } catch (error) {
      console.error("Gagal mengambil data menu kasir", error);
    } finally {
      setLoading(false);
    }
  };

  // Logika Keranjang
  const addToCart = (menu) => {
    const existing = cart.find(item => item.menu_id === menu.id);
    if (existing) {
      setCart(cart.map(item => 
        item.menu_id === menu.id 
          ? { ...item, qty: item.qty + 1, subtotal: (item.qty + 1) * menu.base_price }
          : item
      ));
    } else {
      setCart([...cart, {
        menu_id: menu.id,
        name: menu.name,
        price: menu.base_price,
        qty: 1,
        subtotal: Number(menu.base_price)
      }]);
    }
  };

  const updateQty = (menuId, delta) => {
    setCart(cart.map(item => {
      if (item.menu_id === menuId) {
        const newQty = item.qty + delta;
        if (newQty <= 0) return null; // Akan difilter di bawah
        return { ...item, qty: newQty, subtotal: newQty * item.price };
      }
      return item;
    }).filter(Boolean));
  };

  const removeFromCart = (menuId) => {
    setCart(cart.filter(item => item.menu_id !== menuId));
  };

  const clearCart = () => {
    if (window.confirm('Kosongkan keranjang?')) setCart([]);
  };

  // Kalkulasi
  const totalQty = cart.reduce((sum, item) => sum + item.qty, 0);
  const totalAmount = cart.reduce((sum, item) => sum + item.subtotal, 0);

  // Proses Pembayaran
  const handleCheckout = async () => {
    if (cart.length === 0) return;
    
    try {
      setIsProcessing(true);
      const payload = {
        channel,
        payment_method: paymentMethod,
        items: cart.map(item => ({
          menu_id: item.menu_id,
          qty: item.qty,
          subtotal: item.subtotal
        }))
      };

      const response = await api.post('/pos/checkout', payload);
      alert('Pesanan Sukses! (Order ID: ' + response.data.order_id + ')');
      setCart([]); // Kosongkan keranjang setelah sukses
    } catch (error) {
      console.error(error);
      alert('Gagal memproses pesanan.');
    } finally {
      setIsProcessing(false);
    }
  };

  const filteredMenus = activeCategory === 'all' 
    ? menus 
    : menus.filter(m => m.category_id.toString() === activeCategory.toString());

  if (loading) return <div className="text-center py-10 text-tertiary">Memuat Layar Kasir...</div>;

  return (
    <div className="flex flex-col lg:flex-row gap-6 h-[calc(100vh-8rem)]">
      
      {/* KIRI: Daftar Menu (70%) */}
      <div className="flex-1 flex flex-col h-full bg-white rounded-xl shadow-sm border border-secondary/20 overflow-hidden">
        
        {/* Header Kategori */}
        <div className="p-4 border-b border-secondary/20 bg-cream/30 overflow-x-auto whitespace-nowrap">
          <div className="flex gap-2">
            <button 
              onClick={() => setActiveCategory('all')}
              className={`px-6 py-2.5 rounded-full text-sm font-bold transition-colors ${activeCategory === 'all' ? 'bg-primary text-cream shadow-md' : 'bg-white text-tertiary border border-secondary/30 hover:border-secondary'}`}
            >
              Semua
            </button>
            {categories.map(cat => (
              <button 
                key={cat.id}
                onClick={() => setActiveCategory(cat.id.toString())}
                className={`px-6 py-2.5 rounded-full text-sm font-bold transition-colors ${activeCategory === cat.id.toString() ? 'bg-primary text-cream shadow-md' : 'bg-white text-tertiary border border-secondary/30 hover:border-secondary'}`}
              >
                {cat.name}
              </button>
            ))}
          </div>
        </div>

        {/* Grid Menu Kiri */}
        <div className="flex-1 overflow-y-auto p-4 bg-cream/10">
          <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-4">
            {filteredMenus.map(menu => (
              <button 
                key={menu.id} 
                onClick={() => addToCart(menu)}
                className="group bg-white rounded-xl shadow-sm border border-secondary/20 p-4 flex flex-col text-left hover:shadow-md hover:border-primary/50 transition-all active:scale-95"
              >
                <div className="w-12 h-12 bg-cream rounded-full flex items-center justify-center text-secondary mb-3 group-hover:bg-primary group-hover:text-cream transition-colors">
                  <Coffee size={24} />
                </div>
                <h3 className="font-bold text-primary leading-tight mb-1">{menu.name}</h3>
                <div className="text-xs font-medium text-tertiary mb-3 line-clamp-1">{menu.category?.name}</div>
                <div className="mt-auto font-bold text-secondary">
                  Rp {Number(menu.base_price).toLocaleString('id-ID')}
                </div>
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* KANAN: Keranjang & Checkout (30%) */}
      <div className="w-full lg:w-96 shrink-0 bg-white rounded-xl shadow-sm border border-secondary/20 flex flex-col h-full overflow-hidden relative">
        
        {/* Header Keranjang */}
        <div className="p-5 border-b border-secondary/20 bg-primary text-cream flex justify-between items-center">
          <h2 className="font-bold text-lg flex items-center gap-2">
            <ShoppingCart size={20} /> Pesanan Baru
          </h2>
          <span className="bg-white/20 px-3 py-1 rounded-full text-xs font-bold">
            {totalQty} Items
          </span>
        </div>

        {/* List Item Keranjang */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-cream/5">
          {cart.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center text-tertiary/50">
              <ShoppingCart size={48} className="mb-4" />
              <p className="font-medium">Keranjang masih kosong</p>
            </div>
          ) : (
            cart.map(item => (
              <div key={item.menu_id} className="flex gap-3 bg-white p-3 rounded-lg border border-secondary/20 shadow-sm">
                <div className="flex-1 min-w-0">
                  <h4 className="font-bold text-primary text-sm truncate">{item.name}</h4>
                  <div className="text-xs text-tertiary font-medium mt-0.5">Rp {Number(item.price).toLocaleString('id-ID')}</div>
                </div>
                
                <div className="flex flex-col items-end justify-between">
                  <div className="flex items-center gap-2 bg-cream rounded-md p-1 border border-secondary/30">
                    <button onClick={() => updateQty(item.menu_id, -1)} className="text-tertiary hover:text-primary"><Minus size={14} /></button>
                    <span className="text-sm font-bold w-6 text-center text-primary">{item.qty}</span>
                    <button onClick={() => updateQty(item.menu_id, 1)} className="text-tertiary hover:text-primary"><Plus size={14} /></button>
                  </div>
                  <div className="font-bold text-sm text-secondary mt-2">
                    Rp {item.subtotal.toLocaleString('id-ID')}
                  </div>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Footer Checkout */}
        <div className="border-t border-secondary/20 bg-white p-5 space-y-4">
          
          {/* Opsi */}
          <div className="flex gap-2">
            <select 
              value={channel}
              onChange={(e) => setChannel(e.target.value)}
              className="flex-1 bg-cream/50 border border-secondary/30 rounded-lg px-3 py-2 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-secondary/50 text-primary"
            >
              <option value="Dine-in">Dine-in</option>
              <option value="Takeaway">Takeaway</option>
              <option value="Delivery">Delivery</option>
            </select>
            
            <select 
              value={paymentMethod}
              onChange={(e) => setPaymentMethod(e.target.value)}
              className="flex-1 bg-cream/50 border border-secondary/30 rounded-lg px-3 py-2 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-secondary/50 text-primary"
            >
              <option value="Cash">Tunai (Cash)</option>
              <option value="QRIS">QRIS / E-Wallet</option>
              <option value="Card">Kartu Debit/Kredit</option>
            </select>
          </div>

          <div className="flex justify-between items-center py-2">
            <span className="font-semibold text-tertiary">Total Tagihan</span>
            <span className="text-2xl font-bold text-primary">Rp {totalAmount.toLocaleString('id-ID')}</span>
          </div>

          <div className="flex gap-2">
            <button 
              onClick={clearCart}
              disabled={cart.length === 0}
              className="px-4 py-3 bg-red-50 text-red-500 rounded-lg font-bold hover:bg-red-100 disabled:opacity-50 transition-colors"
            >
              <Trash2 size={20} />
            </button>
            <button 
              onClick={handleCheckout}
              disabled={cart.length === 0 || isProcessing}
              className="flex-1 bg-primary text-cream rounded-lg font-bold text-lg hover:bg-tertiary disabled:opacity-50 flex items-center justify-center gap-2 transition-colors shadow-lg shadow-primary/20"
            >
              {isProcessing ? 'Memproses...' : 'Proses Pesanan'}
            </button>
          </div>
        </div>

      </div>
    </div>
  );
};

export default Cashier;
