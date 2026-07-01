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
    return <div className="min-h-screen bg-grilled-corn flex items-center justify-center font-lato font-bold text-spiced-chili">Memuat tagihan...</div>;
  }

  // Jika pembayaran sukses
  if (isSuccess || order.status !== 'unpaid') {
    return (
      <div className="min-h-screen bg-grilled-corn flex items-center justify-center p-6 text-spiced-chili font-poppins relative">
        <div className="absolute top-0 right-0 w-64 h-64 bg-smoked-mustard rounded-full opacity-30 blur-3xl z-0"></div>
        <div className="bg-white/80 p-8 border-4 border-spiced-chili max-w-md w-full text-center relative z-10 shadow-[8px_8px_0_0_#96311D]">
          <div className="w-24 h-24 bg-smoked-mustard text-spiced-chili border-4 border-spiced-chili rounded-full flex items-center justify-center mx-auto mb-6">
            <CheckCircle size={48} />
          </div>
          <h2 className="text-3xl font-lato font-black uppercase mb-4">Pembayaran<br/>Berhasil!</h2>
          <p className="opacity-80 mb-6">
            Pesanan Anda sedang dikerjakan oleh barista kami. Struk digital akan dikirim ke <b>{order.customer_email}</b>.
          </p>
          <div className="bg-smoked-mustard/20 border-2 border-spiced-chili p-4 mb-8 text-left">
            <p className="text-sm font-bold uppercase opacity-80">Nomor Pesanan</p>
            <p className="font-lato font-black text-xl mb-2">#{order.id}</p>
            <p className="text-sm font-bold uppercase opacity-80">Meja</p>
            <p className="font-lato font-black text-xl">{order.table_number}</p>
          </div>
          <button 
            onClick={() => window.location.href = '/'} 
            className="btn-primary w-full"
          >
            SELESAI
          </button>
        </div>
      </div>
    );
  }

  // Jika belum bayar
  const dynamicQrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=QRIS_CAFE_${order.tenant_id}_TOTAL_${order.total}`;

  return (
    <div className="min-h-screen bg-grilled-corn text-spiced-chili font-poppins p-6 flex flex-col items-center relative overflow-hidden">
      <div className="absolute -top-10 -right-10 w-48 h-48 bg-smoked-mustard rounded-full opacity-30 blur-3xl z-0"></div>
      
      <div className="w-full max-w-md flex items-center gap-4 mb-8 mt-4 relative z-10">
        <button onClick={() => navigate(-1)} className="hover:text-smoked-mustard transition-colors">
          <ArrowLeft size={32} />
        </button>
        <h1 className="text-2xl font-lato font-black uppercase">Pembayaran</h1>
      </div>

      <div className="w-full max-w-md border-4 border-spiced-chili bg-white relative z-10 shadow-[8px_8px_0_0_#96311D]">
        <div className="bg-spiced-chili p-8 text-center text-white border-b-4 border-spiced-chili">
          <p className="text-sm font-bold uppercase tracking-widest opacity-80 mb-2">Total Tagihan</p>
          <h2 className="text-4xl font-lato font-black">Rp {Number(order.total).toLocaleString('id-ID')}</h2>
          <p className="text-sm mt-4 uppercase">Meja: {order.table_number}</p>
        </div>

        <div className="p-8 flex flex-col items-center bg-grilled-corn">
          <div className="bg-white p-4 border-4 border-spiced-chili mb-8 rotate-2 hover:rotate-0 transition-transform">
            <img 
              src={dynamicQrUrl} 
              alt="QRIS Payment" 
              className="w-48 h-48 object-contain"
            />
          </div>

          <p className="text-sm font-medium text-center mb-8 flex items-center justify-center gap-2 max-w-xs opacity-80">
            <QrCode size={20} /> Scan QRIS dengan M-Banking atau e-Wallet Anda
          </p>

          <button 
            onClick={handleSimulatePayment}
            disabled={isPaying}
            className="w-full btn-primary bg-smoked-mustard text-spiced-chili border-spiced-chili hover:bg-spiced-chili flex items-center justify-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed"
          >
            {isPaying ? 'MEMPROSES...' : 'SIMULASIKAN BAYAR (TEST)'}
          </button>
        </div>
        
        <div className="bg-smoked-mustard p-4 border-t-4 border-spiced-chili text-center">
          <p className="text-xs font-bold uppercase flex items-center justify-center gap-2">
            <Receipt size={16} /> Struk digital: {order.customer_email}
          </p>
        </div>
      </div>
    </div>
  );
};

export default CheckoutQR;
