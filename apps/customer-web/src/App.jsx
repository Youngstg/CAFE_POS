import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import EMenu from './pages/customer/EMenu';
import CheckoutQR from './pages/customer/CheckoutQR';

export default function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<EMenu />} />
        <Route path="/menu/:tenantId/:outletId/:tableNumber" element={<EMenu />} />
        <Route path="/checkout/:orderId" element={<CheckoutQR />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  );
}
