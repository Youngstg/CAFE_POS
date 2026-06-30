import React, { useState, useEffect } from 'react';
import { Coffee, TrendingUp, Package, Clock, Activity, BarChart2 } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import api from '../../lib/axios';

const StatCard = ({ title, value, icon: Icon, trend }) => (
  <div className="bg-white p-6 rounded-2xl shadow-sm border border-secondary/20 flex items-center justify-between group hover:shadow-md transition-all">
    <div>
      <p className="text-tertiary font-medium text-sm mb-1">{title}</p>
      <h3 className="text-3xl font-bold text-primary">{value}</h3>
      {trend && (
        <p className="text-emerald-500 text-xs font-semibold mt-2 flex items-center gap-1">
          <TrendingUp size={14} /> {trend}
        </p>
      )}
    </div>
    <div className="bg-cream/50 p-4 rounded-xl text-secondary group-hover:bg-primary group-hover:text-cream transition-colors">
      <Icon size={32} />
    </div>
  </div>
);

const DashboardHome = () => {
  const [data, setData] = useState({ todayRevenue: 0, activeOrders: 0, chartData: [] });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        const response = await api.get('/dashboard');
        setData(response.data);
      } catch (error) {
        console.error("Gagal mengambil data dashboard", error);
      } finally {
        setLoading(false);
      }
    };
    fetchDashboardData();
  }, []);

  if (loading) {
    return <div className="text-center py-20 text-tertiary">Memuat Dashboard Analytics...</div>;
  }

  // Format Y-Axis as Rupiah (k/m)
  const formatYAxis = (tickItem) => {
    if (tickItem >= 1000000) return `Rp ${tickItem / 1000000}M`;
    if (tickItem >= 1000) return `Rp ${tickItem / 1000}k`;
    return tickItem;
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4 bg-primary text-cream p-6 rounded-2xl shadow-md">
        <div>
          <h1 className="text-2xl font-bold flex items-center gap-2 mb-2">
            <Activity size={24} className="text-secondary" /> Analytics Overview
          </h1>
          <p className="text-cream/80 text-sm">Pantau performa penjualan dan operasional kedai Anda hari ini.</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard 
          title="Pendapatan Hari Ini" 
          value={`Rp ${data.todayRevenue.toLocaleString('id-ID')}`} 
          icon={TrendingUp} 
          trend="Real-time"
        />
        <StatCard 
          title="Pesanan Aktif" 
          value={data.activeOrders} 
          icon={Clock} 
        />
        <StatCard 
          title="Total Menu" 
          value="24" 
          icon={Coffee} 
        />
        <StatCard 
          title="Stok Menipis" 
          value="3" 
          icon={Package} 
        />
      </div>

      {/* Grafik Pendapatan */}
      <div className="bg-white p-6 rounded-2xl shadow-sm border border-secondary/20">
        <h3 className="text-lg font-bold text-primary mb-6 flex items-center gap-2">
          <BarChart2 size={20} className="text-secondary" /> Tren Pendapatan 7 Hari Terakhir
        </h3>
        
        <div className="h-80 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={data.chartData} margin={{ top: 5, right: 20, bottom: 5, left: 20 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
              <XAxis 
                dataKey="name" 
                tick={{ fill: '#7288AE', fontSize: 12 }} 
                axisLine={false} 
                tickLine={false} 
              />
              <YAxis 
                tickFormatter={formatYAxis} 
                tick={{ fill: '#7288AE', fontSize: 12 }} 
                axisLine={false} 
                tickLine={false} 
              />
              <Tooltip 
                formatter={(value) => [`Rp ${value.toLocaleString('id-ID')}`, 'Pendapatan']}
                contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
              />
              <Line 
                type="monotone" 
                dataKey="total" 
                stroke="#111844" 
                strokeWidth={3}
                dot={{ fill: '#4B5694', strokeWidth: 2, r: 4 }}
                activeDot={{ r: 6, fill: '#EAE0CF' }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
};

export default DashboardHome;
