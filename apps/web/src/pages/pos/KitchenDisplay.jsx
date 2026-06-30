import React, { useState, useEffect } from 'react';
import { ChefHat, CheckCircle, Clock } from 'lucide-react';
import api from '../../lib/axios';

const KitchenDisplay = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchOrders = async () => {
    try {
      const response = await api.get('/kitchen/orders');
      setOrders(response.data);
    } catch (error) {
      console.error("Gagal mengambil data pesanan dapur", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
    // Auto-refresh tiap 10 detik
    const interval = setInterval(fetchOrders, 10000);
    return () => clearInterval(interval);
  }, []);

  const updateStatus = async (id, newStatus) => {
    try {
      await api.put(`/kitchen/orders/${id}/status`, { status: newStatus });
      fetchOrders(); // Refresh data
    } catch (error) {
      alert('Gagal mengupdate status pesanan.');
    }
  };

  if (loading) return <div className="text-center py-20 text-cream">Memuat Kitchen Display...</div>;

  return (
    <div className="bg-[#1e1e24] min-h-[calc(100vh-8rem)] -m-6 p-6 font-sans">
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold text-white flex items-center gap-3">
          <ChefHat size={36} className="text-amber-400" /> Kitchen Display
        </h1>
        <div className="flex items-center gap-2 bg-white/10 px-4 py-2 rounded-lg text-white">
          <div className="w-3 h-3 bg-emerald-500 rounded-full animate-pulse"></div>
          <span className="font-medium">Live Sync (10s)</span>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        {orders.length === 0 ? (
          <div className="col-span-full py-20 text-center text-white/50 border-2 border-dashed border-white/20 rounded-2xl">
            Tidak ada antrean pesanan. Dapur bersih! 🎉
          </div>
        ) : (
          orders.map(order => (
            <div key={order.id} className="bg-white rounded-2xl overflow-hidden shadow-xl flex flex-col h-full border-t-8 border-amber-400">
              
              <div className="p-4 bg-gray-50 border-b border-gray-100 flex justify-between items-start">
                <div>
                  <div className="font-black text-xl text-gray-800">Order #{order.id}</div>
                  <div className="text-sm font-bold text-gray-500 flex items-center gap-1 mt-1">
                    <Clock size={14} /> {new Date(order.created_at).toLocaleTimeString('id-ID', {hour: '2-digit', minute:'2-digit'})}
                  </div>
                </div>
                <div className={`px-3 py-1 rounded-full text-xs font-bold ${
                  order.channel === 'Dine-in' ? 'bg-blue-100 text-blue-700' : 'bg-purple-100 text-purple-700'
                }`}>
                  {order.channel}
                </div>
              </div>

              <div className="p-4 flex-1 overflow-y-auto bg-white space-y-3">
                {order.items?.map(item => (
                  <div key={item.id} className="flex justify-between items-center border-b border-gray-50 pb-2 last:border-0 last:pb-0">
                    <div className="flex items-start gap-2">
                      <span className="bg-gray-800 text-white w-6 h-6 rounded flex items-center justify-center font-bold text-xs shrink-0">
                        {item.qty}x
                      </span>
                      <span className="font-bold text-gray-700">{item.menu?.name}</span>
                    </div>
                  </div>
                ))}
              </div>

              <div className="p-4 bg-gray-50">
                {order.status === 'pending' ? (
                  <button 
                    onClick={() => updateStatus(order.id, 'preparing')}
                    className="w-full bg-amber-400 text-amber-950 font-bold py-3 rounded-xl hover:bg-amber-500 transition-colors shadow-sm"
                  >
                    Mulai Masak
                  </button>
                ) : (
                  <button 
                    onClick={() => updateStatus(order.id, 'completed')}
                    className="w-full bg-emerald-500 text-white font-bold py-3 rounded-xl hover:bg-emerald-600 transition-colors shadow-sm flex items-center justify-center gap-2"
                  >
                    <CheckCircle size={20} /> Tandai Selesai
                  </button>
                )}
              </div>

            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default KitchenDisplay;
