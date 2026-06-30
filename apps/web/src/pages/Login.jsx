import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Coffee, Mail, Lock } from 'lucide-react';
import api from '../lib/axios';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      // Panggil endpoint API Login Laravel
      const response = await api.post('/login', { email, password });
      
      // Simpan Token Sanctum dan Data User ke LocalStorage
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('user', JSON.stringify(response.data.user));

      // Redirect ke halaman Dashboard utama
      navigate('/');
    } catch (err) {
      if (err.response && err.response.data.errors) {
        // Tampilkan pesan error validasi
        setError(Object.values(err.response.data.errors).flat().join(', '));
      } else {
        setError('Gagal masuk. Periksa kembali email dan password Anda.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-cream/50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="flex justify-center">
          <div className="w-16 h-16 bg-primary rounded-xl flex items-center justify-center text-cream shadow-md">
            <Coffee size={36} />
          </div>
        </div>
        <h2 className="mt-6 text-center text-3xl font-extrabold text-primary">
          Masuk ke CoffeeOS
        </h2>
        <p className="mt-2 text-center text-sm text-tertiary">
          Sistem Manajemen POS & AI Chatbot
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow sm:rounded-xl sm:px-10 border border-tertiary/20">
          <form className="space-y-6" onSubmit={handleLogin}>
            {error && (
              <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-lg text-sm">
                {error}
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-primary mb-2">
                Alamat Email
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Mail size={18} className="text-tertiary" />
                </div>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="appearance-none block w-full pl-10 pr-3 py-2.5 border border-tertiary/40 rounded-lg placeholder-tertiary focus:outline-none focus:ring-secondary focus:border-secondary sm:text-sm"
                  placeholder="admin@cafe.com"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-primary mb-2">
                Kata Sandi
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Lock size={18} className="text-tertiary" />
                </div>
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="appearance-none block w-full pl-10 pr-3 py-2.5 border border-tertiary/40 rounded-lg placeholder-tertiary focus:outline-none focus:ring-secondary focus:border-secondary sm:text-sm"
                  placeholder="••••••••"
                />
              </div>
            </div>

            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <input
                  id="remember-me"
                  name="remember-me"
                  type="checkbox"
                  className="h-4 w-4 text-secondary focus:ring-secondary border-tertiary/40 rounded"
                />
                <label htmlFor="remember-me" className="ml-2 block text-sm text-primary">
                  Ingat saya
                </label>
              </div>

              <div className="text-sm">
                <a href="#" className="font-medium text-secondary hover:text-primary">
                  Lupa sandi?
                </a>
              </div>
            </div>

            <div>
              <button
                type="submit"
                disabled={loading}
                className="w-full flex justify-center py-2.5 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-primary hover:bg-secondary focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-secondary disabled:opacity-70 transition-colors"
              >
                {loading ? 'Memproses...' : 'Masuk sekarang'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
