import {Input,RequiredMark} from '../components/Validation';
import { apiFetch as fetch, api, notice } from '../lib/api';
import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {useAuth} from '../context/AuthContext';

const AccountSettings = () => {
  const location = useLocation();
  const {account,reload} = useAuth();
  const [formError,setFormError] = useState('');
  const [busy,setBusy] = useState(false);
  const navigate = useNavigate();

  // Determine active tab from route
  const getTabFromPath = () => {
    if (location.pathname.endsWith('/security')) return 'security';
    if (location.pathname.endsWith('/edit')) return 'edit';
    if (location.pathname.includes('/settings')) return 'notifications';
    return 'profile';
  };

  const [activeTab, setActiveTab] = useState(getTabFromPath());
  useEffect(() => { setActiveTab(getTabFromPath()); }, [location.pathname]);

  // Profile form state
  const [profileForm, setProfileForm] = useState({
    fullName: localStorage.getItem('username') || '',
    email: localStorage.getItem('email') || '',
    phoneNumber: '',
    address: '',
    city: ''
  });

  // Password form state
  const [pwForm, setPwForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [pwError, setPwError] = useState('');

  // Notification state
  const [emailNotif, setEmailNotif] = useState(true);
  const [smsNotif, setSmsNotif] = useState(false);

  useEffect(()=>{api('/api/v1/account').then(data=>{setProfileForm({fullName:data.user.fullName||'',email:data.user.email||'',phoneNumber:data.user.phoneNumber||'',address:data.user.address||'',city:data.user.city||''});setEmailNotif(Boolean(data.user.emailNotifications));setSmsNotif(Boolean(data.user.smsNotifications));}).catch(e=>setFormError(e.message));},[]);
  const handleProfileSave = async (e) => {
    e.preventDefault();
    setFormError('');
    if(!profileForm.fullName.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(profileForm.email)) return setFormError('Full name and a valid email are required.');
    setBusy(true);
    try {
      await api('/api/v1/account', { method: 'PUT', headers:{'Content-Type':'application/json'}, credentials:'include', body: JSON.stringify(profileForm) });
      await reload();
      notice('Profile updated successfully!');
    } catch(e) { setFormError(e.message); } finally {setBusy(false);}
  };

  const handlePasswordChange = async (e) => {
    e.preventDefault();
    setPwError('');
    if ([...pwForm.newPassword].length < 15) return setPwError('Use at least 15 characters for a new password.');
    if (pwForm.newPassword !== pwForm.confirmPassword) return setPwError('Passwords do not match.');
    try {
      await api('/api/v1/account/password', { method:'POST', headers:{'Content-Type':'application/json'}, credentials:'include', body: JSON.stringify(pwForm) });
      notice('Password changed successfully!');
      setPwForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch(e) { setPwError(e.message); }
  };

  const handleNotifSave = async () => {
    try {
      await api('/api/v1/account/settings', { method:'PATCH', headers:{'Content-Type':'application/json'}, credentials:'include', body: JSON.stringify({ emailNotifications: emailNotif, smsNotifications: smsNotif }) });
      notice('Notification preferences saved!');
    } catch(e) { setFormError(e.message); }
  };

  const rawRole = account.role;
  const roleName = rawRole.toUpperCase().startsWith('ROLE_') ? rawRole.substring(5) : rawRole.toUpperCase();
  const basePath = roleName === 'CUSTOMER' ? '/customer' : '/dashboard';
  const username = account.fullName || account.username;
  const initials = username.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() || 'US';

  const tabs = [
    { id: 'profile', label: 'View Profile', icon: 'fa-user', path: `${basePath}/profile` },
    { id: 'edit', label: 'Edit Details', icon: 'fa-user-pen', path: `${basePath}/profile/edit` },
    { id: 'security', label: 'Change Password', icon: 'fa-key', path: `${basePath}/profile/security` },
    { id: 'notifications', label: 'Notifications', icon: 'fa-sliders', path: `${basePath}/settings` },
  ];

  return (
    <div className="p-6 max-w-4xl mx-auto">
      {formError && <p role="alert" className="text-brand-accent text-[10px] mt-1 mb-4 font-bold uppercase">{formError}</p>}
      {/* Header */}
      <div className="flex items-center gap-4 mb-8 border-b border-[#262626] pb-6">
        <div className="w-16 h-16 rounded-full bg-brand-accent flex items-center justify-center text-white font-black text-2xl shadow-[0_0_20px_rgba(245,158,11,0.3)]">
          {initials}
        </div>
        <div>
          <h1 className="text-2xl font-black text-white uppercase tracking-widest">{username}</h1>
          <span className="bg-brand-accent/10 border border-brand-accent/20 text-brand-accent text-[9px] font-black uppercase tracking-widest px-3 py-1 rounded-full">
            {roleName}
          </span>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 mb-8 bg-[#0a0a0a] border border-[#262626] rounded-2xl p-1">
        {tabs.map(tab => (
          <button
            key={tab.id}
            onClick={() => navigate(tab.path)}
            className={`flex-1 flex items-center justify-center gap-2 px-3 py-2.5 rounded-xl text-[10px] font-bold uppercase tracking-widest transition-all ${
              activeTab === tab.id
                ? 'bg-brand-accent text-white shadow-[0_0_15px_rgba(245,158,11,0.2)]'
                : 'text-gray-500 hover:text-white'
            }`}
          >
            <i aria-hidden="true" className={`fa-solid ${tab.icon}`}></i>
            <span className="hidden sm:inline">{tab.label}</span>
          </button>
        ))}
      </div>

      {/* Tab Content */}
      <AnimatePresence mode="wait">
        {activeTab === 'profile' && (
          <motion.div key="profile" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
            <div className="card-dark p-8">
              <h2 className="text-xl font-black text-white uppercase tracking-widest mb-6">Account Overview</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {[
                  { label: 'Full Name', value: profileForm.fullName || '—' },
                  { label: 'Email Address', value: profileForm.email || '—' },
                  { label: 'Phone Number', value: profileForm.phoneNumber || '—' },
                  { label: 'City', value: profileForm.city || '—' },
                  { label: 'Role', value: roleName },
                  { label: 'Account Status', value: 'Active' },
                ].map((item, i) => (
                  <div key={i} className="bg-[#0a0a0a] border border-[#262626] rounded-xl p-4">
                    <div className="text-[10px] font-bold text-gray-500 uppercase tracking-widest mb-1">{item.label}</div>
                    <div className="text-white font-bold text-sm">{item.value}</div>
                  </div>
                ))}
              </div>
              <button onClick={() => navigate(`${basePath}/profile/edit`)} className="btn-accent mt-8 px-6 py-3 text-xs flex items-center gap-2">
                <i aria-hidden="true" className="fa-solid fa-user-pen"></i> Edit My Details
              </button>
            </div>
          </motion.div>
        )}

        {activeTab === 'edit' && (
          <motion.div key="edit" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
            <div className="card-dark p-8">
              <h2 className="text-xl font-black text-white uppercase tracking-widest mb-6">Edit Personal Details</h2>
              <form noValidate onSubmit={handleProfileSave} className="space-y-5">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                  <div>
                    <label className="block text-[10px] font-bold text-gray-500 tracking-widest mb-2 uppercase" htmlFor="profile-fullName">Full Name<RequiredMark/></label>
                    <Input required id="profile-fullName" aria-label="Full name" validationKey="fullName" type="text" value={profileForm.fullName} onChange={e => setProfileForm({...profileForm, fullName: e.target.value})} className="input-dark w-full p-3 text-sm" placeholder="Full name" />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-gray-500 tracking-widest mb-2 uppercase" htmlFor="profile-email">Email Address<RequiredMark/></label>
                    <Input required id="profile-email" aria-label="Email address" validationKey="email" type="email" value={profileForm.email} onChange={e => setProfileForm({...profileForm, email: e.target.value})} className="input-dark w-full p-3 text-sm" placeholder="Email" />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-gray-500 tracking-widest mb-2 uppercase">Phone Number</label>
                    <Input aria-label="Phone number" validationKey="phoneNumber" type="text" value={profileForm.phoneNumber} onChange={e => setProfileForm({...profileForm, phoneNumber: e.target.value})} className="input-dark w-full p-3 text-sm" placeholder="optional" />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-gray-500 tracking-widest mb-2 uppercase">City</label>
                    <Input aria-label="City" validationKey="city" type="text" value={profileForm.city} onChange={e => setProfileForm({...profileForm, city: e.target.value})} className="input-dark w-full p-3 text-sm" placeholder="optional" />
                  </div>
                  <div className="md:col-span-2">
                    <label className="block text-[10px] font-bold text-gray-500 tracking-widest mb-2 uppercase">Address</label>
                    <Input aria-label="Address" validationKey="address" type="text" value={profileForm.address} onChange={e => setProfileForm({...profileForm, address: e.target.value})} className="input-dark w-full p-3 text-sm" placeholder="optional" />
                  </div>
                </div>
                <div className="pt-4 flex justify-end">
                  <button type="submit" disabled={busy} className="btn-accent px-8 py-3 text-xs flex items-center gap-2">
                    <i aria-hidden="true" className="fa-solid fa-check"></i> Save Changes
                  </button>
                </div>
              </form>
            </div>
          </motion.div>
        )}

        {activeTab === 'security' && (
          <motion.div key="security" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
            <div className="card-dark p-8">
              <h2 className="text-xl font-black text-white uppercase tracking-widest mb-6">Change Password</h2>
              {pwError && (
                <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-4 rounded-xl mb-6 text-xs font-bold uppercase tracking-widest">
                  {pwError}
                </div>
              )}
              <form noValidate onSubmit={handlePasswordChange} className="space-y-5 max-w-md">
                <div>
                  <label className="block text-[10px] font-bold text-gray-500 tracking-widest mb-2 uppercase" htmlFor="security-currentPassword">Current Password<RequiredMark/></label>
                  <Input required id="security-currentPassword" aria-label="Current password" autoComplete="current-password" validationKey="currentPassword" type="password" value={pwForm.currentPassword} onChange={e => setPwForm({...pwForm, currentPassword: e.target.value})} className="input-dark w-full p-3 text-sm" placeholder="••••••••" />
                </div>
                <div>
                  <label className="block text-[10px] font-bold text-gray-500 tracking-widest mb-2 uppercase" htmlFor="security-newPassword">New Password<RequiredMark/></label>
                  <Input required id="security-newPassword" aria-label="New password" autoComplete="new-password" validationKey="newPassword" type="password" value={pwForm.newPassword} onChange={e => setPwForm({...pwForm, newPassword: e.target.value})} className="input-dark w-full p-3 text-sm" placeholder="Min. 15 characters" />
                </div>
                <div>
                  <label className="block text-[10px] font-bold text-gray-500 tracking-widest mb-2 uppercase" htmlFor="security-confirmPassword">Confirm New Password<RequiredMark/></label>
                  <Input required id="security-confirmPassword" aria-label="Confirm new password" autoComplete="new-password" validationKey="confirmPassword" type="password" value={pwForm.confirmPassword} onChange={e => setPwForm({...pwForm, confirmPassword: e.target.value})} className="input-dark w-full p-3 text-sm" placeholder="••••••••" />
                </div>
                <div className="pt-4">
                  <button type="submit" disabled={busy} className="btn-accent px-8 py-3 text-xs flex items-center gap-2">
                    <i aria-hidden="true" className="fa-solid fa-lock"></i> Update Password
                  </button>
                </div>
              </form>
            </div>
          </motion.div>
        )}

        {activeTab === 'notifications' && (
          <motion.div key="notifications" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
            <div className="card-dark p-8">
              <h2 className="text-xl font-black text-white uppercase tracking-widest mb-6">Notification Settings</h2>
              <div className="space-y-4 max-w-md">
                {[
                  { label: 'Email Notifications', desc: 'Receive booking confirmations and updates via email.', state: emailNotif, set: setEmailNotif, icon: 'fa-envelope' },
                  { label: 'SMS Notifications', desc: 'Receive service reminders and alerts via SMS.', state: smsNotif, set: setSmsNotif, icon: 'fa-mobile-screen' },
                ].map((item, i) => (
                  <div key={i} className="flex items-center justify-between bg-[#0a0a0a] border border-[#262626] rounded-xl p-5">
                    <div className="flex items-center gap-4">
                      <i aria-hidden="true" className={`fa-solid ${item.icon} text-brand-accent text-lg`}></i>
                      <div>
                        <div className="text-white font-bold text-sm">{item.label}</div>
                        <div className="text-gray-500 text-xs mt-0.5">{item.desc}</div>
                      </div>
                    </div>
                    <button
                      onClick={() => item.set(!item.state)}
                      className={`w-12 h-6 rounded-full transition-colors relative flex-shrink-0 ${item.state ? 'bg-brand-accent' : 'bg-[#333]'}`}
                    >
                      <span className={`absolute top-1 w-4 h-4 bg-white rounded-full shadow transition-all ${item.state ? 'left-7' : 'left-1'}`}></span>
                    </button>
                  </div>
                ))}
              </div>
              <button onClick={handleNotifSave} className="btn-accent mt-8 px-8 py-3 text-xs flex items-center gap-2">
                <i aria-hidden="true" className="fa-solid fa-check"></i> Save Preferences
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default AccountSettings;
