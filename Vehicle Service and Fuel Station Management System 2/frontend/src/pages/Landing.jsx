import {useEffect, useRef, useState} from 'react';
import {motion, useReducedMotion} from 'framer-motion';
import {Link, useNavigate} from 'react-router-dom';
import {useAuth} from '../context/AuthContext';
import Navbar from '../components/Navbar';
import './Landing.css';

const heroPhotos = [
  {src: '/landing-workshop.jpg', focus: 'center 48%'},
  {src: '/landing-garage.jpg', focus: 'center 100%', zoom: 1.28, origin: 'center bottom'},
  {src: '/landing-interior.jpg', focus: 'center 58%'},
  {src: '/landing-engine.jpg', focus: 'center 55%'},
];

const services = [
  {number: '01', title: 'Service & repairs', label: 'Mechanic repairing a vehicle underside', icon: 'fa-screwdriver-wrench', image: '/landing-service-repairs.jpg', focus: 'center 58%',
    description: 'Book a service for your vehicle and follow its progress, from workshop allocation to the final invoice.',
    link: '/customer/catalog', action: 'Explore services'},
  {number: '02', title: 'Fuel & forecourt', label: 'Fuel pumps at a station at sunset', icon: 'fa-gas-pump', image: '/landing-fuel-forecourt.jpg', focus: 'center 48%',
    description: 'Check current fuel prices before your next stop at our forecourt.',
    link: '/customer/fuel', action: 'View fuel prices'},
  {number: '03', title: 'Parts & essentials', label: 'Vehicle alloy wheels on a parts display', icon: 'fa-gears', image: '/landing-parts-essentials.jpg', focus: 'center 44%',
    description: 'Browse spare parts and available offers. Buy what you need without booking a service appointment.',
    link: '/customer/store', action: 'Browse spare parts'},
];
const journey = [
  ['Book your visit', 'Choose your vehicle, service and an available appointment time.'],
  ['Follow the work', 'Check your service reference for job progress and recorded parts.'],
  ['Review your invoice', 'See your service charges, used parts and payments in one place.'],
  ['Settle your balance', 'Pay through your account or with the cashier once the service is complete.'],
];

function PhotoSpace({src, label, number, icon, focus}) {
  return (
    <div className={`landing-photo-space${src ? ' has-photo' : ''}`}>
      {src ? <img src={src} alt={label} style={{objectPosition: focus || 'center'}} loading="lazy" decoding="async" /> : (
        <div className="landing-photo-placeholder" role="img" aria-label={`${label} photograph space`}>
          <i className={`fa-solid ${icon}`} aria-hidden="true" />
          <span>{label}</span><span className="landing-photo-caption">PHOTO / {number}</span>
        </div>
      )}
      <span className="landing-photo-corner" aria-hidden="true" />
    </div>
  );
}

export default function Landing() {
  const navigate = useNavigate();
  const {account} = useAuth();
  const [activePhoto, setActivePhoto] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const photos = useRef([]);
  const reduceMotion = useReducedMotion();
  const customerRoute = route => account?.role === 'ROLE_CUSTOMER' ? route : '/login';
  const reveal = delay => ({
    initial: reduceMotion ? false : {opacity: 0, y: 18},
    whileInView: {opacity: 1, y: 0},
    viewport: {once: true, amount: 0.12},
    transition: {duration: reduceMotion ? 0 : 0.2, delay: reduceMotion ? 0 : delay, ease: 'easeOut'},
  });
  useEffect(() => {
    if (isPaused || reduceMotion) return;
    const timer = window.setInterval(() => {
      setActivePhoto(current => {
        const next = (current + 1) % heroPhotos.length;
        const image = photos.current[next];
        return image?.complete && image.naturalWidth > 0 ? next : current;
      });
    }, 3000);
    return () => window.clearInterval(timer);
  }, [isPaused, reduceMotion]);

  return (
    <div className="fuelcore-landing">
      <Navbar />
      <main>
        <section id="home" className="landing-hero" aria-labelledby="landing-title">
          <div className="landing-hero-slideshow" aria-hidden="true">
            {heroPhotos.map((photo, index) => (
              <img key={photo.src} ref={image => { photos.current[index] = image; }}
                className={`landing-hero-photo${index === activePhoto ? ' is-active' : ''}`}
                style={{'--photo-focus': photo.focus, '--photo-zoom': photo.zoom || 1.04, '--photo-origin': photo.origin || 'center'}}
                src={photo.src} alt="" fetchPriority={index === 0 ? 'high' : 'low'} decoding="async" />
            ))}
          </div>
          <div className="landing-hero-shade" aria-hidden="true" />
          <div className="landing-hero-content">
            <p className="landing-eyebrow animate-fade-in-up">Service / Fuel / Parts</p>
            <h1 id="landing-title" className="landing-hero-title animate-fade-in-up" style={{animationDelay: '0.1s'}}>
              <span>PREMIUM</span><span>AUTOMOTIVE CARE</span>
            </h1>
            <p className="landing-hero-description animate-fade-in-up" style={{animationDelay: '0.2s'}}>
              Your vehicle. Your journey. Taken care of.<br />
              Vehicle servicing, fuel and spare parts, with a clearer view of every visit.
            </p>
            <div className="landing-hero-actions animate-fade-in-up" style={{animationDelay: '0.3s'}}>
              <button onClick={() => navigate(customerRoute('/customer/book'))} className="landing-button landing-button-primary">
                Schedule appointment <i className="fa-solid fa-arrow-right" aria-hidden="true" />
              </button>
              <button onClick={() => document.getElementById('services')?.scrollIntoView({behavior: reduceMotion ? 'auto' : 'smooth'})} className="landing-button landing-button-secondary">Our services</button>
            </div>
          </div>
          <div className="landing-hero-bottom">
            <a href="#services" className="landing-scroll-link">Discover FuelCore <i className="fa-solid fa-arrow-down" aria-hidden="true" /></a>
            {!reduceMotion && (
              <button className="landing-slideshow-toggle" onClick={() => setIsPaused(paused => !paused)} aria-label={isPaused ? 'Resume background slideshow' : 'Pause background slideshow'}>
                <i className={`fa-solid ${isPaused ? 'fa-play' : 'fa-pause'}`} aria-hidden="true" />{isPaused ? 'RESUME' : 'PAUSE'}
              </button>
            )}
          </div>
        </section>

        <div className="landing-capabilities landing-container" aria-label="FuelCore essentials">
          {[['fa-screwdriver-wrench', 'Workshop', 'Services & repairs'], ['fa-gas-pump', 'Forecourt', 'Current fuel prices'], ['fa-gears', 'Parts counter', 'Spare parts & offers']].map(([icon, title, detail]) => (
            <div key={title}><i className={`fa-solid ${icon}`} aria-hidden="true" /><p><strong>{title}</strong><span>{detail}</span></p></div>
          ))}
        </div>

        <section id="services" className="landing-section landing-container">
          <motion.div className="landing-section-heading" {...reveal(0)}>
            <div><p className="landing-eyebrow">01 / What we do</p><h2>One stop.<br />Every essential.</h2></div>
            <p>From a routine service to your next fuel stop, FuelCore brings the workshop, forecourt and parts counter together.</p>
          </motion.div>
          <div className="landing-services-grid">
            {services.map((service, index) => (
              <motion.article key={service.number} className="landing-service-card" {...reveal(index * 0.03)}>
                <PhotoSpace src={service.image} label={service.label} number={service.number} icon={service.icon} focus={service.focus} />
                <div className="landing-service-copy">
                  <span className="landing-card-number">/{service.number}</span><h3>{service.title}</h3><p>{service.description}</p>
                  <Link to={customerRoute(service.link)} className="landing-text-link">{service.action}<i className="fa-solid fa-arrow-up-right-from-square" aria-hidden="true" /></Link>
                </div>
              </motion.article>
            ))}
          </div>
        </section>

        <section id="about" className="landing-section landing-about">
          <div className="landing-container landing-about-grid">
            <motion.div className="landing-about-visual" {...reveal(0)}>
              <img src="/landing-engine.jpg" alt="Engine components laid out in a workshop" loading="lazy" decoding="async" />
              <div className="landing-image-note"><span>Care in every detail.</span><i className="fa-solid fa-arrow-up-right-from-square" aria-hidden="true" /></div>
            </motion.div>
            <motion.div className="landing-about-copy" {...reveal(0.06)}>
              <p className="landing-eyebrow">02 / The FuelCore experience</p><h2>Real work.<br /><span>Clear progress.</span></h2>
              <p>Good vehicle care starts with knowing what is happening. FuelCore connects your booking, the work on your vehicle and your invoice, so you can follow your service from start to finish.</p>
              <ul className="landing-benefits">
                <li><i className="fa-solid fa-check" aria-hidden="true" />Your vehicles and service history in one place</li>
                <li><i className="fa-solid fa-check" aria-hidden="true" />Job updates and notifications in your account</li>
                <li><i className="fa-solid fa-check" aria-hidden="true" />Invoices, recorded parts and payment history</li>
              </ul>
              <Link to={customerRoute('/customer')} className="landing-text-link">Explore your account<i className="fa-solid fa-arrow-right" aria-hidden="true" /></Link>
            </motion.div>
          </div>
        </section>

        <section id="how-it-works" className="landing-section landing-container">
          <motion.div className="landing-section-heading" {...reveal(0)}>
            <div><p className="landing-eyebrow">03 / From booking to the road</p><h2>A simpler<br />service journey.</h2></div>
            <p>Book through your customer account, or visit the cashier for help. Keep track of the work and settle the final bill when your service is complete.</p>
          </motion.div>
          <div className="landing-journey-grid">
            {journey.map(([title, description], index) => (
              <motion.article className="landing-journey-step" key={title} {...reveal(index * 0.03)}>
                <span className="landing-step-number">0{index + 1}</span><h3>{title}</h3><p>{description}</p>
              </motion.article>
            ))}
          </div>
          <motion.div className="landing-cta" {...reveal(0)}>
            <div><p className="landing-eyebrow">Ready when you are</p><h2>Your next visit<br />starts here.</h2></div>
            <div className="landing-cta-actions">
              <Link to={customerRoute('/customer/book')} className="landing-button landing-button-primary">Book a service<i className="fa-solid fa-arrow-right" aria-hidden="true" /></Link>
              <Link to={account ? (account.role === 'ROLE_CUSTOMER' ? '/customer' : '/dashboard') : '/signup'} className="landing-text-link">{account ? 'Open your account' : 'Create an account'}<i className="fa-solid fa-arrow-up-right-from-square" aria-hidden="true" /></Link>
            </div>
          </motion.div>
        </section>
      </main>

      <footer id="contact" className="landing-footer">
        <div className="landing-container landing-footer-grid">
          <div><Link to="/" className="landing-logo">FUEL<span>CORE</span></Link><p>Vehicle service. Fuel. Spare parts.<br />Everything for the road ahead.</p><a href="https://wa.me/94771234567" target="_blank" rel="noopener noreferrer" className="landing-text-link">WhatsApp us<i className="fa-brands fa-whatsapp" aria-hidden="true" /></a></div>
          <div><h3>Explore</h3><a href="#services">Our services</a><a href="#about">About FuelCore</a><a href="#how-it-works">How it works</a><Link to={customerRoute('/customer/book')}>Book an appointment</Link></div>
          <div><h3>Visit us</h3><p>123 Main Street<br />Malabe, Sri Lanka</p><p>Mon - Sat<br />9:00 AM - 6:00 PM</p></div>
          <div><h3>Let's talk</h3><a href="tel:+94771234567">(+94) 77 123 4567</a><a href="mailto:info@fuelcore.com">info@fuelcore.com</a><Link to={customerRoute('/customer/support')}>Customer support<i className="fa-solid fa-arrow-up-right-from-square" aria-hidden="true" /></Link></div>
        </div>
        <div className="landing-container landing-footer-bottom"><span>FUELCORE / SERVICE CENTRE & FUEL STATION</span><a href="#home">Back to top<i className="fa-solid fa-arrow-up" aria-hidden="true" /></a></div>
      </footer>
    </div>
  );
}
