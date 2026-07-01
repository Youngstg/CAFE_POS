import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ShoppingCart, Plus, Minus, Menu as MenuIcon } from 'lucide-react';
import axios from 'axios';

const EMenu = () => {
  const { tenantId = 1, outletId = 1, tableNumber } = useParams();
  const navigate = useNavigate();
  
  const [menus, setMenus] = useState([]);
  const [categories, setCategories] = useState([]);
  const [activeCategory, setActiveCategory] = useState('Semua');
  
  const [cart, setCart] = useState([]);
  const [isCartOpen, setIsCartOpen] = useState(false);
  
  const [customerEmail, setCustomerEmail] = useState('');
  const [manualTable, setManualTable] = useState('');
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
    const finalTable = tableNumber || manualTable;
    if (!finalTable) return alert('Silakan masukkan nomor meja Anda.');
    if (!customerEmail) return alert('Silakan masukkan email Anda untuk pengiriman struk.');

    setIsSubmitting(true);
    try {
      const payload = {
        tenant_id: tenantId,
        outlet_id: outletId,
        table_number: finalTable,
        customer_email: customerEmail,
        items: cart.map(item => ({ menu_id: item.id, qty: item.qty, subtotal: item.qty * item.base_price }))
      };
      const response = await axios.post('http://127.0.0.1:8000/api/public/checkout', payload);
      navigate(`/checkout/${response.data.order.id}`);
    } catch (error) {
      alert('Gagal membuat pesanan');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-grilled-corn text-spiced-chili font-poppins relative overflow-x-hidden">
      
      {/* Floating Cart Button */}
      {totalItems > 0 && !isCartOpen && (
        <div className="fixed bottom-6 right-6 z-50">
          <button 
            onClick={() => setIsCartOpen(true)}
            className="bg-spiced-chili text-white p-4 rounded-full shadow-2xl hover:scale-105 transition-transform relative"
          >
            <ShoppingCart size={24} />
            <span className="absolute -top-2 -right-2 bg-smoked-mustard text-spiced-chili font-black text-xs w-6 h-6 flex items-center justify-center rounded-full border border-spiced-chili">
              {totalItems}
            </span>
          </button>
        </div>
      )}

      {/* Cart Modal */}
      {isCartOpen && (
        <div className="fixed inset-0 z-[60] flex justify-end">
          <div className="absolute inset-0 bg-spiced-chili/50 backdrop-blur-sm" onClick={() => setIsCartOpen(false)}></div>
          <div className="bg-grilled-corn border-l-2 border-spiced-chili w-full max-w-md h-full flex flex-col relative z-10 shadow-2xl">
            <div className="p-6 border-b-2 border-spiced-chili flex justify-between items-center bg-smoked-mustard">
              <h2 className="text-xl font-lato font-black uppercase">Your Order</h2>
              <button onClick={() => setIsCartOpen(false)} className="font-bold">X</button>
            </div>
            <div className="overflow-y-auto p-6 flex-1 space-y-4">
              {cart.map(item => (
                <div key={item.id} className="flex justify-between items-center pb-4 border-b border-spiced-chili/20">
                  <div>
                    <h4 className="font-lato font-bold">{item.name}</h4>
                    <p className="text-sm font-medium">Rp {Number(item.base_price).toLocaleString('id-ID')}</p>
                  </div>
                  <div className="flex items-center gap-3 border border-spiced-chili px-2 py-1">
                    <button onClick={() => removeFromCart(item.id)}><Minus size={14} /></button>
                    <span className="font-bold w-4 text-center">{item.qty}</span>
                    <button onClick={() => addToCart(item)}><Plus size={14} /></button>
                  </div>
                </div>
              ))}
              <div className="mt-8 space-y-4">
                {!tableNumber && (
                  <div>
                    <label className="block font-lato font-bold text-sm mb-1">TABLE NUMBER</label>
                    <input type="text" value={manualTable} onChange={e => setManualTable(e.target.value)} className="w-full p-2 border border-spiced-chili bg-transparent focus:outline-none" />
                  </div>
                )}
                <div>
                  <label className="block font-lato font-bold text-sm mb-1">EMAIL FOR RECEIPT</label>
                  <input type="email" value={customerEmail} onChange={e => setCustomerEmail(e.target.value)} className="w-full p-2 border border-spiced-chili bg-transparent focus:outline-none" />
                </div>
              </div>
            </div>
            <div className="p-6 border-t-2 border-spiced-chili">
              <div className="flex justify-between items-center mb-6">
                <span className="font-lato font-bold">TOTAL</span>
                <span className="text-2xl font-black">Rp {totalPrice.toLocaleString('id-ID')}</span>
              </div>
              <button onClick={handleCheckout} disabled={isSubmitting || cart.length===0} className="w-full py-3 border-2 border-spiced-chili font-lato font-bold uppercase hover:bg-spiced-chili hover:text-white transition-colors">
                {isSubmitting ? 'PROCESSING...' : 'ORDER NOW'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Navbar */}
      <nav className="flex justify-between items-center px-6 lg:px-16 py-6 border-b-2 border-spiced-chili bg-grilled-corn">
        <div className="flex items-center gap-1">
          <h1 className="text-2xl lg:text-3xl font-lato font-black tracking-widest text-spiced-chili flex flex-col">
            BIRDHOUSE
            <span className="text-[10px] lg:text-xs font-poppins font-normal tracking-wide">BAR & GRILL</span>
          </h1>
        </div>
        <div className="hidden lg:flex gap-10 font-lato font-bold text-sm">
          <a href="#about" className="hover:text-smoked-mustard transition-colors">ABOUT US</a>
          <a href="#menu" className="hover:text-smoked-mustard transition-colors">MENU</a>
          <a href="#instagram" className="hover:text-smoked-mustard transition-colors">INSTAGRAM FEED</a>
          <a href="#contact" className="hover:text-smoked-mustard transition-colors">CONTACT US</a>
        </div>
        <button className="lg:hidden p-2">
          <MenuIcon size={24} />
        </button>
      </nav>

      {/* Hero Section */}
      <section className="px-6 lg:px-16 py-12 lg:py-20 relative overflow-hidden">
        {/* Background blobs simulating the collage */}
        <div className="absolute top-10 right-10 w-[300px] h-[300px] bg-smoked-mustard rounded-full opacity-50 mix-blend-multiply blur-xl"></div>
        <div className="absolute top-40 right-40 w-[250px] h-[250px] bg-spiced-chili rounded-full opacity-20 mix-blend-multiply blur-xl"></div>
        
        <div className="flex flex-col lg:flex-row items-center gap-12 max-w-7xl mx-auto relative z-10">
          <div className="flex-1 space-y-8">
            <div className="space-y-4">
              <p className="text-xl lg:text-2xl italic font-lato text-spiced-chili/80">COME AS</p>
              <div className="relative inline-block">
                <h2 className="text-5xl lg:text-7xl font-lato font-black uppercase italic relative z-10">A GUEST,</h2>
                <div className="absolute bottom-2 left-0 w-full h-3 bg-smoked-mustard -rotate-2 z-0"></div>
              </div>
            </div>
            
            <div className="flex justify-end pr-10 lg:pr-20">
              <img src="https://images.unsplash.com/photo-1544148103-0773bf10d330?ixlib=rb-4.0.3&auto=format&fit=crop&w=400&q=80" alt="Guests" className="w-48 h-32 object-cover border-2 border-spiced-chili shadow-[4px_4px_0_0_#96311D]" />
            </div>

            <div className="space-y-4 pt-4">
              <p className="text-xl lg:text-2xl italic font-lato text-spiced-chili/80">LEAVE AS</p>
              <div className="relative inline-block">
                <h2 className="text-5xl lg:text-7xl font-lato font-black uppercase italic relative z-10">A FRIEND!</h2>
                <div className="absolute bottom-2 left-0 w-full h-3 bg-smoked-mustard rotate-2 z-0"></div>
              </div>
            </div>
          </div>
          
          <div className="flex-1 w-full relative">
            <div className="relative w-full aspect-[4/5] max-w-md mx-auto">
              <img src="https://images.unsplash.com/photo-1555396273-367ea4eb4db5?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80" alt="Bar atmosphere" className="w-full h-full object-cover border-2 border-spiced-chili shadow-[8px_8px_0_0_#96311D]" />
              <img src="https://images.unsplash.com/photo-1414235077428-338989a2e8c0?ixlib=rb-4.0.3&auto=format&fit=crop&w=300&q=80" alt="Food detail" className="absolute -bottom-10 -left-10 w-40 h-40 object-cover border-2 border-spiced-chili shadow-[4px_4px_0_0_#96311D]" />
            </div>
          </div>
        </div>
      </section>

      {/* Serving Tasty Food Section */}
      <section id="about" className="bg-smoked-mustard py-16 px-6 lg:px-16 border-y-2 border-spiced-chili">
        <div className="max-w-7xl mx-auto flex flex-col-reverse lg:flex-row items-center gap-12">
          <div className="flex-1 w-full max-w-md">
            <img src="https://images.unsplash.com/photo-1559339352-11d035aa65de?ixlib=rb-4.0.3&auto=format&fit=crop&w=600&q=80" alt="Waiter serving" className="w-full h-auto border-2 border-spiced-chili" />
          </div>
          <div className="flex-1 space-y-6">
            <h2 className="text-4xl lg:text-5xl font-lato font-black uppercase leading-tight">Serving Tasty Food And Beverages Since 1995</h2>
            <div className="space-y-4 text-sm font-medium">
              <p>Our dishes have stayed true to <span className="underline decoration-spiced-chili decoration-2">family recipes</span> since '95, offering a home-cooked feel and preserving the Wilson family legacy.</p>
              <p>We serve tasty burgers, sandwiches, juicy steaks, and snacks, with our homemade buffalo wings being the most popular item.</p>
              <p>For a lighter option, try our healthy salads, but don't miss out on the buffalo wings!</p>
            </div>
            <button className="mt-4 px-8 py-3 border-2 border-spiced-chili font-lato font-bold uppercase tracking-widest hover:bg-spiced-chili hover:text-white transition-colors bg-transparent">
              BOOK A TABLE
            </button>
          </div>
        </div>
      </section>

      {/* Menu Section */}
      <section id="menu" className="py-20 px-6 lg:px-16 bg-grilled-corn">
        <div className="max-w-7xl mx-auto">
          <div className="flex flex-col lg:flex-row justify-between items-end mb-12 gap-6">
            <div>
              <h2 className="text-5xl font-lato font-black uppercase relative inline-block mb-4">
                Menu
                <div className="absolute -z-10 top-2 -left-4 w-12 h-12 bg-spiced-chili opacity-20 rounded-full blur-md"></div>
              </h2>
              <p className="max-w-md text-sm font-medium">
                Discover our tasty quick bites: fresh salads, juicy burgers, hearty sandwiches, and succulent steaks!
              </p>
            </div>
            <div className="flex flex-wrap gap-4 font-lato font-bold text-sm uppercase">
              {categories.map(cat => (
                <button
                  key={cat}
                  onClick={() => setActiveCategory(cat)}
                  className={`pb-1 border-b-2 transition-colors ${activeCategory === cat ? 'border-spiced-chili' : 'border-transparent text-spiced-chili/60 hover:text-spiced-chili'}`}
                >
                  {cat}
                </button>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {filteredMenus.map(menu => {
              const cartItem = cart.find(c => c.id === menu.id);
              // Using Unsplash source for realistic food mockup if no image
              const imgUrl = menu.image || `https://source.unsplash.com/400x300/?food,${menu.category?.name || 'burger'}`;
              return (
                <div key={menu.id} className="border border-spiced-chili bg-white p-3 relative flex flex-col">
                  {/* Price Badge */}
                  <div className="absolute -top-4 -right-4 w-14 h-14 rounded-full bg-smoked-mustard text-spiced-chili font-lato font-black text-xl flex items-center justify-center border-2 border-spiced-chili z-10 shadow-sm">
                    {Math.floor(menu.base_price / 1000)}$
                  </div>
                  
                  <img src="https://images.unsplash.com/photo-1568901346375-23c9450c58cd?ixlib=rb-4.0.3&auto=format&fit=crop&w=400&q=80" alt={menu.name} className="w-full h-48 object-cover border border-spiced-chili/30 mb-4" />
                  
                  <div className="flex-1 flex flex-col">
                    <h4 className="font-lato font-black text-lg uppercase leading-tight mb-2">{menu.name}:</h4>
                    <p className="text-xs opacity-70 mb-4 line-clamp-3">
                      Buns, beef patty, cheddar cheese, lettuce, pickles, onions, signature sauce.
                    </p>
                    
                    <div className="mt-auto">
                      {cartItem ? (
                        <div className="flex items-center justify-between border border-spiced-chili p-1">
                          <button onClick={() => removeFromCart(menu.id)} className="p-1 hover:bg-spiced-chili hover:text-white"><Minus size={16}/></button>
                          <span className="font-lato font-bold">{cartItem.qty}</span>
                          <button onClick={() => addToCart(menu)} className="p-1 hover:bg-spiced-chili hover:text-white"><Plus size={16}/></button>
                        </div>
                      ) : (
                        <button onClick={() => addToCart(menu)} className="w-full py-2 border border-spiced-chili font-lato font-bold uppercase text-xs hover:bg-spiced-chili hover:text-white transition-colors">
                          Add to Order
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
          
          <div className="mt-12 text-center">
            <button className="px-8 py-3 border-2 border-spiced-chili font-lato font-bold uppercase tracking-widest hover:bg-spiced-chili hover:text-white transition-colors">
              VIEW FULL MENU
            </button>
          </div>
        </div>
      </section>

      {/* Guests Love Us */}
      <section className="bg-grilled-corn py-16 px-6 lg:px-16 border-t border-spiced-chili/20">
        <div className="max-w-7xl mx-auto flex flex-col lg:flex-row gap-12">
          <div className="flex-1">
            <h2 className="text-4xl lg:text-5xl font-lato font-black uppercase leading-tight mb-6 relative">
              Our Guests<br/>Love Us!
              <div className="absolute top-1/2 left-0 w-32 h-16 border-2 border-smoked-mustard rounded-full -rotate-12 transform -translate-y-1/2 -z-10"></div>
            </h2>
            <p className="text-sm font-medium mb-8 max-w-sm">
              We aim to provide customers with delicious food, beverages, and exceptional service.
            </p>
            <div className="flex gap-4">
              <button className="w-10 h-10 rounded-full border-2 border-spiced-chili flex items-center justify-center hover:bg-spiced-chili hover:text-white"><Minus size={16} /></button>
              <button className="w-10 h-10 rounded-full border-2 border-spiced-chili bg-spiced-chili text-white flex items-center justify-center"><Plus size={16} /></button>
            </div>
          </div>
          <div className="flex-1 grid grid-cols-1 sm:grid-cols-2 gap-6">
            <div className="border-2 border-spiced-chili p-6 bg-white relative">
              <div className="flex items-center gap-4 mb-4">
                <img src="https://i.pravatar.cc/150?img=11" className="w-12 h-12 rounded-full border border-spiced-chili" alt="Michael Davis" />
                <h4 className="font-lato font-bold uppercase text-sm">Michael Davis</h4>
              </div>
              <p className="text-xs leading-relaxed opacity-80">
                Birdhouse is my go-to spot for a relaxing evening. The no-smoking policy is a big plus, and the food always hits the spot. Try the buffalo wings!
              </p>
            </div>
            <div className="border-2 border-spiced-chili p-6 bg-white relative">
              <div className="flex items-center gap-4 mb-4">
                <img src="https://i.pravatar.cc/150?img=5" className="w-12 h-12 rounded-full border border-spiced-chili" alt="Sarah Caldwell" />
                <h4 className="font-lato font-bold uppercase text-sm">Sarah Caldwell</h4>
              </div>
              <p className="text-xs leading-relaxed opacity-80">
                Birdhouse Bar & Grill is a hidden gem! The service is fantastic, and it feels like home. Highly recommend their fresh salads.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Instagram Section */}
      <section id="instagram" className="bg-smoked-mustard py-20 px-6 lg:px-16 border-y-2 border-spiced-chili">
        <div className="max-w-7xl mx-auto flex flex-col lg:flex-row items-center gap-12">
          <div className="flex-1">
            <h2 className="text-4xl lg:text-6xl font-lato font-black uppercase leading-tight mb-6">
              Explore Birdhouse Bar & Grill Through Our Instagram.
            </h2>
            <p className="text-sm font-medium mb-8">
              Enjoy mouthwatering dishes, cozy spaces, and stay updated on new menu items and events. Join our community and get a taste of the Birdhouse experience before you visit.
            </p>
            <button className="px-8 py-3 border-2 border-spiced-chili font-lato font-bold uppercase tracking-widest bg-grilled-corn hover:bg-spiced-chili hover:text-white transition-colors">
              FOLLOW US ON INSTAGRAM
            </button>
          </div>
          <div className="flex-1 grid grid-cols-2 gap-4">
            <img src="https://images.unsplash.com/photo-1543353071-087092ec393a?ixlib=rb-4.0.3&auto=format&fit=crop&w=400&q=80" alt="Insta 1" className="w-full h-48 object-cover border-2 border-spiced-chili" />
            <img src="https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?ixlib=rb-4.0.3&auto=format&fit=crop&w=400&q=80" alt="Insta 2" className="w-full h-48 object-cover border-2 border-spiced-chili" />
            <img src="https://images.unsplash.com/photo-1551782450-a2132b4ba21d?ixlib=rb-4.0.3&auto=format&fit=crop&w=400&q=80" alt="Insta 3" className="w-full h-48 object-cover border-2 border-spiced-chili col-span-2" />
          </div>
        </div>
      </section>

      {/* Get In Touch */}
      <section id="contact" className="bg-smoked-mustard py-16 px-6 lg:px-16 pb-32">
        <div className="max-w-2xl mx-auto text-center">
          <h2 className="text-4xl lg:text-5xl font-lato font-black uppercase mb-10">Get In Touch!</h2>
          <form className="space-y-4">
            <input type="text" placeholder="Name" className="w-full bg-grilled-corn border-2 border-spiced-chili p-4 font-poppins focus:outline-none" />
            <input type="email" placeholder="E-mail" className="w-full bg-grilled-corn border-2 border-spiced-chili p-4 font-poppins focus:outline-none" />
            <input type="tel" placeholder="Phone" className="w-full bg-grilled-corn border-2 border-spiced-chili p-4 font-poppins focus:outline-none" />
            <textarea placeholder="Message" rows="4" className="w-full bg-grilled-corn border-2 border-spiced-chili p-4 font-poppins focus:outline-none"></textarea>
            <button type="button" className="w-full bg-grilled-corn border-2 border-spiced-chili py-4 font-lato font-bold uppercase tracking-widest hover:bg-spiced-chili hover:text-white transition-colors">
              SEND A MESSAGE
            </button>
          </form>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-spiced-chili text-grilled-corn py-12 px-6 lg:px-16 border-t-2 border-spiced-chili -mt-10 relative z-10">
        <div className="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-3 gap-8 items-start">
          <div>
            <h1 className="text-2xl font-lato font-black tracking-widest flex flex-col mb-6">
              BIRDHOUSE
              <span className="text-[10px] font-poppins font-normal tracking-wide">BAR & GRILL</span>
            </h1>
            <div className="flex gap-4">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="2" y="2" width="20" height="20" rx="5" ry="5"></rect><path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"></path><line x1="17.5" y1="6.5" x2="17.51" y2="6.5"></line></svg>
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z"></path></svg>
            </div>
          </div>
          
          <div className="grid grid-cols-2 gap-4 text-xs font-lato font-bold tracking-widest uppercase">
            <div className="space-y-4">
              <p><a href="#about" className="hover:text-smoked-mustard">ABOUT US</a></p>
              <p><a href="#menu" className="hover:text-smoked-mustard">MENU</a></p>
            </div>
            <div className="space-y-4">
              <p><a href="#instagram" className="hover:text-smoked-mustard">INSTAGRAM FEED</a></p>
              <p><a href="#contact" className="hover:text-smoked-mustard">CONTACT US</a></p>
            </div>
          </div>

          <div className="text-xs font-poppins opacity-80 space-y-2 text-right">
            <p>123 Restaurant St.</p>
            <p>California, 52601</p>
            <p>(671) 555-0110</p>
            <p>Mon-Thu: 11PM-11PM</p>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default EMenu;
