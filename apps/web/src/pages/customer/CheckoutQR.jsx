import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { QrCode, CheckCircle, Receipt, ArrowLeft } from 'lucide-react';

const CheckoutQR = () => {
  const { orderId } = useParams();
  const navigate = useNavigate();
  
  const [order, setOrder] = useState(null);
  const [isPaying, setIsPaying] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  useEffect(() => {
    fetchOrder();
  }, [orderId]);

  const fetchOrder = async () => {
    try {
      const response = await axios.get(`http://127.0.0.1:8000/api/public/order/${orderId}`);
      setOrder(response.data);
    } catch (error) {
      console.error('Error fetching order', error);
      alert('Pesanan tidak ditemukan.');
    }
  };

  const handleSimulatePayment = async () => {
    setIsPaying(true);
    try {
      await axios.post(`http://127.0.0.1:8000/api/public/pay/${orderId}`);
      setIsSuccess(true);
    } catch (error) {
      console.error('Payment error', error);
      alert('Gagal menyimulasikan pembayaran.');
      setIsPaying(false);
    }
  };

  if (!order) {
    return <div className="min-h-screen flex items-center justify-center text-slate-500">Memuat tagihan...</div>;
  }

  // Jika pembayaran sukses
  if (isSuccess || order.status !== 'unpaid') {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
        <div className="bg-white p-8 rounded-3xl shadow-xl max-w-sm w-full text-center">
          <div className="w-20 h-20 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto mb-6">
            <CheckCircle size={48} />
          </div>
          <h2 className="text-2xl font-bold text-slate-800 mb-2">Pembayaran Berhasil!</h2>
          <p className="text-slate-500 mb-6">
            Pesanan Anda sedang dikerjakan oleh barista kami. Struk digital akan dikirim ke <b>{order.customer_email}</b>.
          </p>
          <div className="bg-slate-50 rounded-xl p-4 mb-6 text-left border border-slate-100">
            <p className="text-sm text-slate-500">Nomor Pesanan</p>
            <p className="font-mono font-bold text-slate-800">#{order.id}</p>
            <p className="text-sm text-slate-500 mt-2">Meja</p>
            <p className="font-bold text-slate-800">{order.table_number}</p>
          </div>
          <button 
            onClick={() => window.location.href = '/'} // Redirect kemana saja
            className="w-full bg-slate-100 text-slate-700 font-bold py-3 rounded-xl hover:bg-slate-200 transition-colors"
          >
            Selesai
          </button>
        </div>
      </div>
    );
  }

  // Jika belum bayar
  const dynamicQrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=QRIS_CAFE_${order.tenant_id}_TOTAL_${order.total}`;

  return (
    <div className="min-h-screen bg-slate-50 font-sans p-4 flex flex-col items-center">
      <div className="w-full max-w-sm flex items-center gap-4 mb-6 mt-4">
        <button onClick={() => navigate(-1)} className="text-slate-600 hover:text-slate-900">
          <ArrowLeft size={24} />
        </button>
        <h1 className="text-xl font-bold text-slate-800">Pembayaran QRIS</h1>
      </div>

      <div className="bg-white w-full max-w-sm rounded-3xl shadow-lg border border-slate-100 overflow-hidden">
        <div className="bg-primary p-6 text-center text-white">
          <p className="text-indigo-200 text-sm font-medium mb-1">Total Tagihan</p>
          <h2 className="text-3xl font-bold">Rp {Number(order.total).toLocaleString('id-ID')}</h2>
          <p className="text-indigo-100 text-xs mt-2">Meja: {order.table_number}</p>
        </div>

        <div className="p-8 flex flex-col items-center">
          <div className="bg-white p-2 rounded-2xl shadow-sm border border-slate-200 mb-6 relative">
            {/* Hiasan Sudut QR */}
            <div className="absolute top-0 left-0 w-4 h-4 border-t-4 border-l-4 border-primary rounded-tl-lg"></div>
            <div className="absolute top-0 right-0 w-4 h-4 border-t-4 border-r-4 border-primary rounded-tr-lg"></div>
            <div className="absolute bottom-0 left-0 w-4 h-4 border-b-4 border-l-4 border-primary rounded-bl-lg"></div>
            <div className="absolute bottom-0 right-0 w-4 h-4 border-b-4 border-r-4 border-primary rounded-br-lg"></div>
            
            <img 
              src={dynamicQrUrl} 
              alt="QRIS Payment" 
              className="w-48 h-48 object-contain"
            />
          </div>

          <p className="text-sm text-slate-500 text-center mb-8 flex items-center justify-center gap-2">
            <QrCode size={16} /> Scan QRIS dengan M-Banking / e-Wallet Anda
          </p>

          <button 
            onClick={handleSimulatePayment}
            disabled={isPaying}
            className="w-full bg-emerald-500 hover:bg-emerald-600 text-white font-bold py-3.5 rounded-xl shadow-md transition-transform active:scale-95 flex items-center justify-center gap-2 disabled:opacity-70 disabled:active:scale-100"
          >
            {isPaying ? 'Memproses...' : 'Simulasikan Pembayaran Berhasil'}
          </button>
        </div>
        
        <div className="bg-slate-50 p-4 border-t border-slate-100 text-center">
          <p className="text-xs text-slate-400 flex items-center justify-center gap-1">
            <Receipt size={14} /> Struk akan dikirim ke {order.customer_email}
          </p>
        </div>
      </div>
    </div>
  );
};

export default CheckoutQR;
