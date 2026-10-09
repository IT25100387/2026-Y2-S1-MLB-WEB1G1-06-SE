import {useRef, useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {useAuth} from '../context/AuthContext';
import {Modal} from './DataUI';

// Temporary demo credentials. This component is loaded only when quick login is enabled.
const accounts = [
  {label: 'Admin', username: 'admin', password: 'Admin@123'},
  {label: 'Manager', username: 'manager', password: 'manager@123'},
  {label: 'Cashier', username: 'cashier', password: 'cashier@123'},
  {label: 'Mechanic 1', username: 'mechanic1', password: 'mechanic1@123'},
  {label: 'Mechanic 2', username: 'mechanic2', password: 'mechanic2@123'},
  {label: 'Mechanic 3', username: 'mechanic3', password: 'mechanic3@123'},
  {label: 'Customer 1', username: 'customer1', password: 'customer1@123'},
  {label: 'Customer 2', username: 'customer2', password: 'customer2@123'},
];

export default function TemporaryQuickLogin() {
  const [open, setOpen] = useState(false);
  const [pending, setPending] = useState('');
  const [error, setError] = useState('');
  const trigger = useRef(null);
  const {login} = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const close = () => {
    if (pending) return;
    setOpen(false);
    trigger.current?.focus();
  };

  const signIn = async account => {
    if (pending) return;
    setPending(account.username);
    setError('');
    try {
      const role = await login(account.username, account.password);
      const base = role === 'ROLE_CUSTOMER' ? '/customer' : '/dashboard';
      const requested = location.state?.from;
      navigate(role === 'ROLE_CASHIER' ? '/dashboard/pos' : requested?.startsWith(base) ? requested : base, {replace: true});
    } catch (failure) {
      setError(failure.message);
    } finally {
      setPending('');
    }
  };

  return (
    <>
      <button ref={trigger} type="button" onClick={() => {setError(''); setOpen(true);}} className="mt-6 w-full rounded-full border border-[#333] px-4 py-3 text-xs font-bold uppercase tracking-widest text-gray-300 hover:border-brand-accent hover:text-white transition-colors">
        <i aria-hidden="true" className="fa-solid fa-flask mr-2" />
        Test account login
      </button>
      {open && (
        <Modal title="Temporary test login" onClose={close}>
          <p className="mb-5 text-sm text-gray-400">Choose a test account to sign in.</p>
          <div className="grid grid-cols-2 gap-3">
            {accounts.map(account => (
              <button key={account.username} type="button" aria-label={`Sign in as ${account.label}`} disabled={Boolean(pending)} onClick={() => signIn(account)} className="rounded-xl border border-[#333] bg-[#1a1a1a] p-4 text-left hover:border-brand-accent hover:bg-brand-accent/10 transition-colors disabled:opacity-40">
                <span className="block text-sm font-bold text-white">{pending === account.username ? 'Signing in...' : account.label}</span>
                <span className="mt-1 block text-[10px] font-mono text-gray-400">{account.username}</span>
              </button>
            ))}
          </div>
          {error && <p role="alert" className="mt-4 text-xs text-brand-accent">{error}</p>}
        </Modal>
      )}
    </>
  );
}
