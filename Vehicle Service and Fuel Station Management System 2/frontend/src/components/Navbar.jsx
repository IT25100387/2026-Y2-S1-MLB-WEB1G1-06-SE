import {useEffect, useState} from 'react';
import {Link} from 'react-router-dom';
import {AnimatePresence, motion} from 'framer-motion';
import {useAuth} from '../context/AuthContext';
import NotificationBell from './NotificationBell';

const links = [['Home', 'home'], ['About Us', 'about'], ['Services', 'services'], ['Contact', 'contact']];

export default function Navbar() {
  const {account} = useAuth();
  const [isScrolled, setIsScrolled] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  useEffect(() => {
    const scroll = () => setIsScrolled(window.scrollY > 20);
    scroll();
    window.addEventListener('scroll', scroll, {passive: true});
    return () => window.removeEventListener('scroll', scroll);
  }, []);

  useEffect(() => {
    if (!isMobileMenuOpen) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const close = event => {
      if (event.key === 'Escape') setIsMobileMenuOpen(false);
    };
    window.addEventListener('keydown', close);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', close);
    };
  }, [isMobileMenuOpen]);

  return (
    <>
      <header className={`fixed w-full z-50 flex justify-center transition-all duration-500 ease-in-out ${
        isScrolled ? 'top-0 px-0' : 'top-4 px-4 sm:px-8'
      }`}>
        <nav aria-label="Main navigation" className={`w-full transition-all duration-500 ease-in-out flex justify-between items-center gap-3 ${
          isScrolled
            ? 'max-w-full bg-[#0a0a0a]/95 backdrop-blur-xl border-b border-white/10 shadow-[0_10px_30px_rgba(0,0,0,0.8)] rounded-none px-6 sm:px-12 py-4'
            : 'max-w-6xl bg-black/40 backdrop-blur-md border border-white/5 shadow-2xl rounded-full px-6 py-3'
        }`}>
          <Link to="/" onClick={() => window.scrollTo({top: 0, behavior: 'smooth'})} className="shrink-0 text-xl font-black tracking-widest text-white">
            FUEL<span className="text-brand-accent">CORE</span>
          </Link>

          <div className="hidden md:flex items-center gap-5 lg:gap-10">
            {links.map(([label, id]) => (
              <a key={id} href={`#${id}`} className="text-[10px] lg:text-xs font-bold tracking-widest text-gray-400 hover:text-white uppercase transition-colors duration-300 relative group whitespace-nowrap">
                {label}
                <span className="absolute -bottom-2 left-0 w-0 h-[2px] bg-brand-accent transition-all duration-300 group-hover:w-full" />
              </a>
            ))}
          </div>

          <div className="flex items-center gap-3 shrink-0">
            {account && <NotificationBell />}
            <Link to="/login" className="bg-brand-accent text-white px-5 sm:px-6 py-2.5 rounded-full font-black text-xs tracking-widest hover:brightness-110 hover:scale-105 transition-all duration-300 shadow-[0_0_15px_rgba(229,46,46,0.25)]">
              SIGN IN
            </Link>
            <button aria-label={isMobileMenuOpen ? 'Close navigation' : 'Open navigation'} aria-expanded={isMobileMenuOpen} aria-controls="landing-mobile-navigation" onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)} className="md:hidden text-gray-300 hover:text-white">
              <i aria-hidden="true" className={`fa-solid ${isMobileMenuOpen ? 'fa-xmark' : 'fa-bars'}`} />
            </button>
          </div>
        </nav>
      </header>

      <AnimatePresence>
        {isMobileMenuOpen && (
          <motion.nav id="landing-mobile-navigation" aria-label="Mobile navigation" initial={{opacity: 0}} animate={{opacity: 1}} exit={{opacity: 0}} transition={{duration: 0.2}} className="fixed inset-0 z-40 bg-[#0a0a0a]/98 backdrop-blur-xl pt-28 px-6 flex flex-col md:hidden">
            <div className="flex flex-col space-y-6 text-center">
              {links.map(([label, id]) => (
                <a key={id} href={`#${id}`} onClick={() => setIsMobileMenuOpen(false)} className="text-xl font-bold tracking-widest text-white hover:text-brand-accent uppercase">
                  {label}
                </a>
              ))}
            </div>
          </motion.nav>
        )}
      </AnimatePresence>
    </>
  );
}
